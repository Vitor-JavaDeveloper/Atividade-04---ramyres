package br.senai.healthcare.config;

import br.senai.healthcare.modelo.Administrador;
import br.senai.healthcare.modelo.Enfermeiro;
import br.senai.healthcare.modelo.FichaTriagem;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.HistoricoMedico;
import br.senai.healthcare.modelo.Medico;
import br.senai.healthcare.modelo.NivelGravidade;
import br.senai.healthcare.modelo.Paciente;
import br.senai.healthcare.modelo.Recepcionista;
import br.senai.healthcare.modelo.StatusFila;
import br.senai.healthcare.repositorio.FichaTriagemRepositorio;
import br.senai.healthcare.repositorio.FilaAtendimentoRepositorio;
import br.senai.healthcare.repositorio.HistoricoMedicoRepositorio;
import br.senai.healthcare.repositorio.PacienteRepositorio;
import br.senai.healthcare.repositorio.UsuarioRepositorio;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * dados de demonstracao (todos ficticios) para a apresentacao.
 *
 * roda sozinho quando o sistema sobe. so grava alguma coisa se o banco
 * estiver vazio (nenhum usuario cadastrado), entao nao duplica nada
 * e nao apaga o que foi cadastrado durante o uso.
 *
 * usuarios de teste (login / senha):
 *   admin      / admin123      administrador
 *   recepcao   / recepcao123   recepcionista
 *   enfermeiro / enfermeiro123 enfermeiro
 *   medico     / medico123     medica (dra. helena rocha)
 *   medico2    / medico123     medico (dr. rafael lima)
 */
@Component
public class DadosIniciais implements ApplicationRunner {

    private final UsuarioRepositorio usuarios;
    private final PacienteRepositorio pacientes;
    private final FichaTriagemRepositorio fichas;
    private final FilaAtendimentoRepositorio fila;
    private final HistoricoMedicoRepositorio historicos;

    public DadosIniciais(UsuarioRepositorio usuarios, PacienteRepositorio pacientes, FichaTriagemRepositorio fichas,
                         FilaAtendimentoRepositorio fila, HistoricoMedicoRepositorio historicos) {
        this.usuarios = usuarios;
        this.pacientes = pacientes;
        this.fichas = fichas;
        this.fila = fila;
        this.historicos = historicos;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) {
            System.out.println("[HealthCare] Banco ja tem dados, carga inicial ignorada.");
            return;
        }
        popular();
        System.out.println("[HealthCare] Dados de demonstracao gravados no banco.");
    }

    private void popular() {
        LocalDateTime agora = LocalDateTime.now();

        // ---------- usuarios (as senhas sao guardadas com bcrypt; todos comecam ativos) ----------
        usuarios.save(new Administrador("José Alencar Franco", "admin", "admin123", "TOTAL"));
        usuarios.save(new Recepcionista("Carla Mendes", "recepcao", "recepcao123", "REC-001"));
        Enfermeiro enf = (Enfermeiro) usuarios.save(new Enfermeiro("Lucas Ferreira", "enfermeiro", "enfermeiro123", "COREN-GO 123456"));
        Medico helena = (Medico) usuarios.save(new Medico("Dra. Helena Rocha", "medico", "medico123", "CRM-GO 12345", "Clínica Geral", true));
        Medico rafael = (Medico) usuarios.save(new Medico("Dr. Rafael Lima", "medico2", "medico123", "CRM-GO 23456", "Pediatria", true));
        usuarios.save(new Medico("Dr. Paulo Nunes", null, null, "CRM-GO 34567", "Ortopedia", false));

        // ---------- pacientes (cpfs ficticios, mas com digitos validos) ----------
        Paciente maria = paciente("Maria Oliveira Silva", "12345678909", "1985-08-24", "Feminino", "(62) 98765-4321", "Rua das Flores, 123, Centro, Goiânia");
        Paciente joao = paciente("João Pedro Santos", "98765432100", "1990-03-12", "Masculino", "(62) 99876-1122", "Av. Goiás, 450, Setor Central, Goiânia");
        Paciente ana = paciente("Ana Beatriz Costa", "11144477735", "2001-11-05", "Feminino", "(62) 98123-4455", "Rua 10, 87, Setor Oeste, Goiânia");
        Paciente carlos = paciente("Carlos Eduardo Souza", "52998224725", "1978-06-30", "Masculino", "(62) 99654-7788", "Rua T-63, 1500, Setor Bueno, Goiânia");
        Paciente fernanda = paciente("Fernanda Lima", "39053344705", "1995-01-19", "Feminino", "(62) 98444-3322", "Av. T-9, 2200, Jardim América, Goiânia");
        Paciente roberto = paciente("Roberto Alves", "74697131401", "1962-09-02", "Masculino", "(62) 99111-2233", "Rua 15, 40, Setor Marista, Goiânia");
        Paciente julia = paciente("Júlia Martins", "32165498791", "2015-04-22", "Feminino", "(62) 98222-6677", "Rua C-140, 300, Jardim América, Goiânia");

        // ---------- fila atual: os 5 pacientes da tela de fila do prototipo ----------
        // entradas em horarios diferentes para mostrar que a gravidade passa na frente da ordem de chegada
        aguardando(fernanda, "Dor de garganta leve há 2 dias, sem febre.", NivelGravidade.NAO_URGENTE, agora.minusMinutes(50), enf);
        aguardando(carlos, "Torção no tornozelo direito, consegue apoiar o pé.", NivelGravidade.POUCO_URGENTE, agora.minusMinutes(40), enf);
        aguardando(ana, "Febre de 38,9°C e vômitos desde ontem.", NivelGravidade.URGENTE, agora.minusMinutes(30), enf);
        aguardando(joao, "Dor no peito moderada há 1 hora, pressão 16x10.", NivelGravidade.MUITO_URGENTE, agora.minusMinutes(12), enf);
        aguardando(maria, "Falta de ar intensa, saturação 86%, lábios arroxeados.", NivelGravidade.EMERGENCIA, agora.minusMinutes(2), enf);

        // ---------- historico: consultas antigas da maria (iguais ao mockup) ----------
        consultaAntiga(maria, helena, LocalDateTime.of(2023, 10, 10, 14, 20), NivelGravidade.POUCO_URGENTE,
                "Tosse seca e febre leve.", "Infecção respiratória superior",
                "Paciente apresentou tosse seca, febre leve (37,8°C) e coriza.", "Ibuprofeno 400mg 8/8h, repouso, hidratação.", 35, enf);
        consultaAntiga(maria, rafael, LocalDateTime.of(2023, 5, 15, 9, 10), NivelGravidade.URGENTE,
                "Náuseas, vômitos e diarreia.", "Gastroenterite aguda",
                "Relato de náuseas, vômitos e diarreia há 2 dias. Sinais vitais estáveis.", "Soro de reidratação oral, dieta leve, retorno se piorar.", 40, enf);

        // ---------- atendimentos dos ultimos dias, para o relatorio ter numeros ----------
        consultaAntiga(roberto, helena, agora.minusDays(1).withHour(10), NivelGravidade.MUITO_URGENTE,
                "Dor abdominal forte.", "Cólica renal", "Dor em flanco esquerdo, sem febre.", "Analgésico e antiespasmódico, hidratação.", 8, enf);
        consultaAntiga(julia, rafael, agora.minusDays(1).withHour(15), NivelGravidade.URGENTE,
                "Febre alta e dor de ouvido.", "Otite média aguda", "Criança com otalgia à direita.", "Amoxicilina 250mg 8/8h por 7 dias.", 45, enf);
        consultaAntiga(carlos, helena, agora.minusDays(3).withHour(11), NivelGravidade.POUCO_URGENTE,
                "Dor lombar.", "Lombalgia mecânica", "Dor ao levantar peso no trabalho.", "Dipirona 1g se dor, compressa morna.", 95, enf);
        consultaAntiga(joao, helena, agora.minusDays(5).withHour(20), NivelGravidade.EMERGENCIA,
                "Desmaio na rua.", "Síncope vasovagal", "Recuperou consciência na chegada. ECG normal.", "Observação por 6h, liberado.", 0, enf);
        consultaAntiga(ana, rafael, agora.minusDays(6).withHour(8), NivelGravidade.NAO_URGENTE,
                "Pedido de atestado, resfriado.", "Resfriado comum", "Coriza e espirros, sem febre.", "Repouso e hidratação.", 150, enf);
        consultaAntiga(fernanda, helena, agora.minusDays(9).withHour(16), NivelGravidade.URGENTE,
                "Crise de enxaqueca.", "Enxaqueca sem aura", "Dor pulsátil com fotofobia.", "Analgésico venoso, sala escura.", 50, enf);
    }

    // ===== metodos de apoio so para montar os dados =====

    private Paciente paciente(String nome, String cpf, String nasc, String sexo, String tel, String end) {
        Paciente p = new Paciente();
        p.setNome(nome);
        p.setCpf(cpf);
        p.setDataNascimento(LocalDate.parse(nasc));
        p.setSexo(sexo);
        p.setTelefone(tel);
        p.setEndereco(end);
        p.setCadastradoEm(LocalDateTime.now().minusDays(10));
        return pacientes.save(p);
    }

    private FichaTriagem ficha(Paciente p, String sintomas, NivelGravidade g, LocalDateTime quando, Enfermeiro enf) {
        FichaTriagem f = new FichaTriagem();
        f.setPaciente(p);
        f.setSintomas(sintomas);
        f.classificarGravidade(g);
        f.setDataHora(quando);
        f.setEnfermeiro(enf);
        return fichas.save(f);
    }

    /** cria uma ficha e coloca o paciente aguardando na fila. */
    private void aguardando(Paciente p, String sintomas, NivelGravidade g, LocalDateTime chegada, Enfermeiro enf) {
        FilaAtendimento item = new FilaAtendimento();
        item.setFicha(ficha(p, sintomas, g, chegada, enf));
        item.setEntradaEm(chegada);
        fila.save(item);
    }

    /** cria uma consulta ja finalizada: ficha, passagem pela fila e registro no historico. */
    private void consultaAntiga(Paciente p, Medico m, LocalDateTime chegada, NivelGravidade g, String sintomas,
                                String diagnostico, String obs, String prescricao, int minutosEspera, Enfermeiro enf) {
        FichaTriagem f = ficha(p, sintomas, g, chegada, enf);
        f.setMedico(m);
        FilaAtendimento item = new FilaAtendimento();
        item.setFicha(f);
        item.setMedico(m);
        item.atualizarStatus(StatusFila.FINALIZADO);
        item.setEntradaEm(chegada);
        item.setInicioAtendimento(chegada.plusMinutes(minutosEspera));
        item.setFimAtendimento(chegada.plusMinutes(minutosEspera + 20));
        fila.save(item);

        HistoricoMedico h = new HistoricoMedico();
        h.registrarAtendimento(diagnostico, obs, prescricao);
        h.setPaciente(p);
        h.setMedico(m);
        h.setFicha(f);
        h.setDataConsulta(item.getInicioAtendimento());
        historicos.save(h);
    }
}
