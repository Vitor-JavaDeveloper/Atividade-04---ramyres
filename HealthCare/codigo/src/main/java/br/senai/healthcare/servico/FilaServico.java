package br.senai.healthcare.servico;

import br.senai.healthcare.modelo.Enfermeiro;
import br.senai.healthcare.modelo.FichaTriagem;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.HistoricoMedico;
import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.NivelGravidade;
import br.senai.healthcare.modelo.Paciente;
import br.senai.healthcare.modelo.StatusFila;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.repositorio.FichaTriagemRepositorio;
import br.senai.healthcare.repositorio.FilaAtendimentoRepositorio;
import br.senai.healthcare.repositorio.HistoricoMedicoRepositorio;
import br.senai.healthcare.repositorio.MedicoRepositorio;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/**
 * coracao do sistema: triagem, fila de prioridade, atendimento e historico.
 *
 * fluxo (figura 4 da documentacao):
 * enfermeiro salva a ficha de triagem, o sistema coloca o paciente na fila,
 * a fila e ordenada por gravidade e chegada, o medico chama o proximo,
 * ve o historico, atende e registra a consulta no historico.
 */
@Service
public class FilaServico {

    // ordem da fila (rd02): primeiro a gravidade (emergencia vem antes), depois quem chegou primeiro.
    private static final Comparator<FilaAtendimento> ORDEM_DA_FILA =
            Comparator.comparing((FilaAtendimento f) -> f.getFicha().getGravidade().ordinal())
                    .thenComparing(FilaAtendimento::getEntradaEm);

    private final FichaTriagemRepositorio fichas;
    private final FilaAtendimentoRepositorio fila;
    private final HistoricoMedicoRepositorio historicos;
    private final MedicoRepositorio medicos;
    private final CadastroServico cadastro;
    private final int tempoMedioConsultaMin;

    public FilaServico(FichaTriagemRepositorio fichas, FilaAtendimentoRepositorio fila,
                       HistoricoMedicoRepositorio historicos, MedicoRepositorio medicos, CadastroServico cadastro,
                       @Value("${healthcare.tempo-medio-consulta-min:15}") int tempoMedioConsultaMin) {
        this.fichas = fichas;
        this.fila = fila;
        this.historicos = historicos;
        this.medicos = medicos;
        this.cadastro = cadastro;
        this.tempoMedioConsultaMin = tempoMedioConsultaMin;
    }

    // ===================== triagem (rf03) =====================

    /**
     * registra a ficha de triagem e ja coloca o paciente na fila.
     * criterios do hu02: exige gravidade, liga a ficha ao paciente e insere na fila automaticamente.
     */
    @Transactional // grava ficha e fila juntas: se uma falhar, nenhuma fica gravada
    public FilaAtendimento registrarTriagem(Map<String, Object> dados, Usuario enfermeiro) {
        String sintomas = CadastroServico.limpar(CadastroServico.texto(dados.get("sintomas")));
        String gravidadeTexto = CadastroServico.texto(dados.get("gravidade"));
        Long pacienteId = lerId(dados.get("pacienteId"));
        Long medicoId = lerId(dados.get("medicoId"));

        NivelGravidade gravidade = null;
        try {
            gravidade = gravidadeTexto == null ? null : NivelGravidade.valueOf(gravidadeTexto);
        } catch (IllegalArgumentException ignorado) { /* fica null e cai na validacao */ }

        new ErroApi.Validador()
                .exigir(pacienteId != null, "pacienteId", "Selecione o paciente.")
                .exigir(sintomas.length() >= 3, "sintomas", "Descreva os sintomas do paciente.")
                .exigir(gravidade != null, "gravidade", "Selecione o nível de gravidade.")
                .lancarSeTiverErro();

        Paciente paciente = cadastro.buscarPaciente(pacienteId);

        // nao deixa o mesmo paciente entrar duas vezes na fila ao mesmo tempo
        if (fila.existsByStatusInAndFichaPacienteId(List.of(StatusFila.AGUARDANDO, StatusFila.EM_ATENDIMENTO), pacienteId)) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este paciente já está na fila de atendimento.",
                    Map.of("pacienteId", "Paciente já está na fila."));
        }

        FichaTriagem ficha = new FichaTriagem();
        ficha.setPaciente(paciente);
        ficha.setSintomas(sintomas);
        ficha.classificarGravidade(gravidade);
        if (enfermeiro instanceof Enfermeiro e) ficha.setEnfermeiro(e);
        if (medicoId != null) ficha.setMedico(cadastro.buscarMedico(medicoId)); // rnf04: medico responsavel
        fichas.save(ficha);

        FilaAtendimento entrada = new FilaAtendimento();
        entrada.setFicha(ficha);
        fila.save(entrada);
        recalcularFila(); // a fila se reorganiza na hora (hu03)
        return entrada;
    }

    // ===================== fila (rf04) =====================

    /**
     * recalcula posicao e tempo estimado de todo mundo que esta aguardando.
     * e chamado sempre que alguem entra, sai ou quando a fila e consultada.
     */
    public List<FilaAtendimento> recalcularFila() {
        int medicosLivres = medicosDisponiveis();
        List<FilaAtendimento> aguardando = fila.findByStatus(StatusFila.AGUARDANDO).stream()
                .sorted(ORDEM_DA_FILA)
                .toList();
        for (int i = 0; i < aguardando.size(); i++) {
            aguardando.get(i).calcularPosicao(i + 1);
            aguardando.get(i).calcularTempoEspera(i, tempoMedioConsultaMin, medicosLivres);
        }
        return aguardando;
    }

    /** fila ativa: primeiro quem esta em atendimento, depois quem aguarda, ja em ordem de prioridade. */
    public List<FilaAtendimento> filaAtiva() {
        List<FilaAtendimento> resultado = new ArrayList<>(fila.findByStatus(StatusFila.EM_ATENDIMENTO).stream()
                .sorted(Comparator.comparing(FilaAtendimento::getInicioAtendimento))
                .toList());
        resultado.addAll(recalcularFila());
        return resultado;
    }

    /**
     * tempo medio de espera estimado por gravidade (hu08 e rnf03).
     * responde a pergunta da recepcao: "se chegar um paciente agora com esta gravidade,
     * quanto tempo ele deve esperar?". conta quem esta na frente dele (mesma gravidade ou mais grave).
     */
    public List<Map<String, Object>> temposPorGravidade() {
        List<FilaAtendimento> aguardando = recalcularFila();
        int medicosLivres = medicosDisponiveis();
        List<Map<String, Object>> linhas = new ArrayList<>();
        for (NivelGravidade g : NivelGravidade.values()) {
            long naFrente = aguardando.stream().filter(f -> f.getFicha().getGravidade().ordinal() <= g.ordinal()).count();
            long nesteNivel = aguardando.stream().filter(f -> f.getFicha().getGravidade() == g).count();
            int estimado = g == NivelGravidade.EMERGENCIA ? 0
                    : (int) Math.ceil(naFrente * tempoMedioConsultaMin / (double) Math.max(1, medicosLivres));
            Map<String, Object> linha = new LinkedHashMap<>();
            linha.put("gravidade", g);
            linha.put("descricao", g.getDescricao());
            linha.put("aguardando", nesteNivel);
            linha.put("tempoEstimadoMin", estimado);
            linha.put("tempoMaximoReferenciaMin", g.getTempoMaximoEsperaMin());
            linhas.add(linha);
        }
        return linhas;
    }

    public int medicosDisponiveis() { return (int) medicos.countByDisponivelTrue(); }
    public int getTempoMedioConsultaMin() { return tempoMedioConsultaMin; }

    // ===================== atendimento =====================

    /** o medico chama o proximo da fila (o primeiro da ordem de prioridade). */
    @Transactional
    public FilaAtendimento chamarProximo(Medico medico) {
        List<FilaAtendimento> aguardando = recalcularFila();
        if (aguardando.isEmpty()) throw new ErroApi(HttpStatus.CONFLICT, "Não há pacientes aguardando na fila.");
        return chamar(aguardando.get(0).getId(), medico);
    }

    /**
     * o medico chama um paciente especifico.
     * a ficha passa a ficar ligada a ele (rnf04) e ele fica indisponivel enquanto atende.
     */
    @Transactional
    public synchronized FilaAtendimento chamar(Long filaId, Medico medico) {
        FilaAtendimento f = buscar(filaId);
        if (f.getStatus() != StatusFila.AGUARDANDO) throw new ErroApi(HttpStatus.CONFLICT, "Este paciente não está aguardando.");
        if (fila.existsByStatusAndMedicoId(StatusFila.EM_ATENDIMENTO, medico.getId())) throw new ErroApi(HttpStatus.CONFLICT, "Finalize o atendimento atual antes de chamar outro paciente.");

        f.setMedico(medico);
        f.getFicha().setMedico(medico);
        f.atualizarStatus(StatusFila.EM_ATENDIMENTO);
        medico.atualizarDisponibilidade(false);
        medicos.save(medico);
        fichas.save(f.getFicha());
        return fila.save(f);
    }

    /** finaliza a consulta e grava no historico medico do paciente (rf05). */
    @Transactional
    public synchronized HistoricoMedico finalizar(Long filaId, Map<String, String> dados, Medico medico) {
        FilaAtendimento f = buscar(filaId);
        if (f.getStatus() != StatusFila.EM_ATENDIMENTO) throw new ErroApi(HttpStatus.CONFLICT, "Este atendimento não está em andamento.");
        if (!mesmoMedico(f.getMedico(), medico)) throw new ErroApi(HttpStatus.FORBIDDEN, "Somente o médico que chamou o paciente pode finalizar.");

        String diagnostico = CadastroServico.limpar(dados.get("diagnostico"));
        new ErroApi.Validador()
                .exigir(diagnostico.length() >= 3, "diagnostico", "Informe o diagnóstico.")
                .lancarSeTiverErro();

        HistoricoMedico h = new HistoricoMedico();
        h.registrarAtendimento(diagnostico, CadastroServico.limpar(dados.get("observacoes")), CadastroServico.limpar(dados.get("prescricao")));
        h.setPaciente(f.getFicha().getPaciente());
        h.setMedico(medico);
        h.setFicha(f.getFicha());
        historicos.save(h);

        f.atualizarStatus(StatusFila.FINALIZADO);
        medico.atualizarDisponibilidade(true); // terminou, volta a ficar livre
        medicos.save(medico);
        fila.save(f);
        return h;
    }

    /** tira o paciente da fila sem consulta (desistencia, transferencia etc). */
    @Transactional
    public synchronized FilaAtendimento cancelar(Long filaId) {
        FilaAtendimento f = buscar(filaId);
        if (f.getStatus() == StatusFila.FINALIZADO || f.getStatus() == StatusFila.CANCELADO) {
            throw new ErroApi(HttpStatus.CONFLICT, "Este atendimento já foi encerrado.");
        }
        if (f.getStatus() == StatusFila.EM_ATENDIMENTO && f.getMedico() != null) {
            f.getMedico().atualizarDisponibilidade(true); // libera o medico que estava atendendo
            medicos.save(f.getMedico());
        }
        f.atualizarStatus(StatusFila.CANCELADO);
        return fila.save(f);
    }

    public FilaAtendimento buscar(Long filaId) {
        return fila.findById(filaId).orElseThrow(() -> ErroApi.naoEncontrado("Atendimento"));
    }

    // ===================== historico (rf05 e hu04) =====================

    /** junta tudo do paciente: dados, consultas anteriores e triagens (mais recentes primeiro). */
    public Map<String, Object> historicoDoPaciente(Long pacienteId) {
        Paciente p = cadastro.buscarPaciente(pacienteId);
        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("paciente", p);
        resposta.put("consultas", historicos.findByPacienteIdOrderByDataConsultaDesc(pacienteId));
        resposta.put("triagens", fichas.findByPacienteIdOrderByDataHoraDesc(pacienteId));
        return resposta;
    }

    /** media simples de uma lista de minutos (null se a lista estiver vazia). */
    static Integer media(List<Long> minutos) {
        OptionalDouble m = minutos.stream().mapToLong(Long::longValue).average();
        return m.isPresent() ? (int) Math.round(m.getAsDouble()) : null;
    }

    /** compara dois medicos pelo id (dois objetos lidos do banco podem ser diferentes mesmo sendo o mesmo medico). */
    private static boolean mesmoMedico(Medico a, Medico b) {
        return a != null && b != null && a.getId().equals(b.getId());
    }

    private static Long lerId(Object o) {
        try {
            return o == null || String.valueOf(o).isBlank() ? null : Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
