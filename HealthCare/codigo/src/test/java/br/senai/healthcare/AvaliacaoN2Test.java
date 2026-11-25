package br.senai.healthcare;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import br.senai.healthcare.config.SegurancaInterceptor;
import br.senai.healthcare.controle.FilaController;
import br.senai.healthcare.controle.TratadorDeErros;
import br.senai.healthcare.modelo.Enfermeiro;
import br.senai.healthcare.modelo.FichaTriagem;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.NivelGravidade;
import br.senai.healthcare.modelo.Paciente;
import br.senai.healthcare.modelo.StatusFila;
import br.senai.healthcare.repositorio.FichaTriagemRepositorio;
import br.senai.healthcare.repositorio.FilaAtendimentoRepositorio;
import br.senai.healthcare.repositorio.HistoricoMedicoRepositorio;
import br.senai.healthcare.repositorio.MedicoRepositorio;
import br.senai.healthcare.servico.CadastroServico;
import br.senai.healthcare.servico.ErroApi;
import br.senai.healthcare.servico.FilaServico;

/**
 * Avaliação N2 - Testes Automatizados (SENAI FATESG | ADS 4 | Teste de Software)
 * Módulo avaliado: Triagem e Fila de Atendimento Prioritária (RF03, RF04, RD02, RD03, RNF02, RNF03)
 */
class AvaliacaoN2Test {

    private FichaTriagemRepositorio fichasRepo;
    private FilaAtendimentoRepositorio filaRepo;
    private HistoricoMedicoRepositorio historicosRepo;
    private MedicoRepositorio medicosRepo;
    private CadastroServico cadastroServico;
    private FilaServico filaServico;

    @BeforeEach
    void configurarDependencias() {
        fichasRepo = mock(FichaTriagemRepositorio.class);
        filaRepo = mock(FilaAtendimentoRepositorio.class);
        historicosRepo = mock(HistoricoMedicoRepositorio.class);
        medicosRepo = mock(MedicoRepositorio.class);
        cadastroServico = mock(CadastroServico.class);
        filaServico = new FilaServico(fichasRepo, filaRepo, historicosRepo, medicosRepo, cadastroServico, 15);
    }

    @Test
    @DisplayName("U-01 [Unitário - Funcionamento Esperado]: Calcula tempo de espera proporcional à fila e médicos livres")
    void u01_calcularTempoEsperaFuncionamentoEsperado() {
        FichaTriagem ficha = new FichaTriagem();
        ficha.classificarGravidade(NivelGravidade.URGENTE);

        FilaAtendimento item = new FilaAtendimento();
        item.setFicha(ficha);

        // 4 pacientes na frente * 15 min por consulta / 2 médicos livres = 30 min
        int tempoCalculado = item.calcularTempoEspera(4, 15, 2);

        assertEquals(30, tempoCalculado);
        assertEquals(30, item.getTempoEsperaEstimado());
    }

    @Test
    @DisplayName("U-02 [Unitário - Erro/Alternativa]: Rejeita triagem com sintomas curtos e gravidade inválida")
    void u02_rejeitarTriagemComDadosInvalidos() {
        Enfermeiro enfermeiro = new Enfermeiro("Lucas Ferreira", "enfermeiro", "enfermeiro123", "COREN-GO 123456");
        enfermeiro.setId(3L);

        Map<String, Object> dadosInvalidos = Map.of(
                "pacienteId", 1L,
                "sintomas", "ab",          // regra exige mínimo de 3 caracteres
                "gravidade", "GRAU_X"     // valor inexistente no enum NivelGravidade
        );

        ErroApi erro = assertThrows(ErroApi.class, () -> filaServico.registrarTriagem(dadosInvalidos, enfermeiro));

        assertEquals(400, erro.getStatus().value());
        assertTrue(erro.getCampos().containsKey("sintomas"));
        assertTrue(erro.getCampos().containsKey("gravidade"));
        verify(fichasRepo, never()).save(any());
        verify(filaRepo, never()).save(any());
    }

    @Test
    @DisplayName("U-03 [Unitário - Limite]: Trata piso de 0 médicos disponíveis e prioridade máxima EMERGENCIA")
    void u03_calcularTempoEsperaNosLimites() {
        // Limite 1: 0 médicos livres não pode gerar divisão por zero (assume divisor mínimo = 1)
        FichaTriagem fichaUrgente = new FichaTriagem();
        fichaUrgente.classificarGravidade(NivelGravidade.URGENTE);
        FilaAtendimento itemUrgente = new FilaAtendimento();
        itemUrgente.setFicha(fichaUrgente);

        assertEquals(60, itemUrgente.calcularTempoEspera(4, 15, 0));
        assertEquals(0, itemUrgente.calcularTempoEspera(0, 15, 2));

        // Limite 2: EMERGENCIA tem tempo de espera 0 min independentemente da lotação da fila
        FichaTriagem fichaEmergencia = new FichaTriagem();
        fichaEmergencia.classificarGravidade(NivelGravidade.EMERGENCIA);
        FilaAtendimento itemEmergencia = new FilaAtendimento();
        itemEmergencia.setFicha(fichaEmergencia);

        assertEquals(0, itemEmergencia.calcularTempoEspera(10, 15, 0));
    }

    @Test
    @DisplayName("C-01 [Complementar - Integração]: Registra triagem via API REST e reordena a fila por gravidade")
    void c01_integracaoRegistrarTriagemEConsultarFilaOrdenada() throws Exception {
        Enfermeiro enfermeiro = new Enfermeiro("Lucas Ferreira", "enfermeiro", "enfermeiro123", "COREN-GO 123456");
        enfermeiro.setId(3L);

        Paciente carlos = new Paciente();
        carlos.setId(1L);
        carlos.setNome("Carlos Eduardo Souza");
        carlos.setCpf("52998224725");
        carlos.setDataNascimento(LocalDate.of(1978, 6, 30));

        Paciente maria = new Paciente();
        maria.setId(2L);
        maria.setNome("Maria Oliveira Silva");
        maria.setCpf("12345678909");
        maria.setDataNascimento(LocalDate.of(1985, 8, 24));

        // Estado inicial da fila: Carlos (NAO_URGENTE) chegou há 40 minutos
        List<FilaAtendimento> filaEmMemoria = new ArrayList<>();
        FichaTriagem fichaCarlos = new FichaTriagem();
        fichaCarlos.setId(10L);
        fichaCarlos.setPaciente(carlos);
        fichaCarlos.setSintomas("Dor lombar leve há 3 dias");
        fichaCarlos.classificarGravidade(NivelGravidade.NAO_URGENTE);

        FilaAtendimento entradaCarlos = new FilaAtendimento();
        entradaCarlos.setId(100L);
        entradaCarlos.setFicha(fichaCarlos);
        entradaCarlos.setEntradaEm(LocalDateTime.now().minusMinutes(40));
        filaEmMemoria.add(entradaCarlos);

        // Simulação apenas da camada de persistência (repositórios JPA)
        when(cadastroServico.buscarPaciente(2L)).thenReturn(maria);
        when(medicosRepo.countByDisponivelTrue()).thenReturn(1L);
        when(filaRepo.existsByStatusInAndFichaPacienteId(anyList(), eq(2L))).thenReturn(false);
        when(fichasRepo.save(any(FichaTriagem.class))).thenAnswer(inv -> {
            FichaTriagem f = inv.getArgument(0);
            f.setId(11L);
            return f;
        });
        when(filaRepo.save(any(FilaAtendimento.class))).thenAnswer(inv -> {
            FilaAtendimento fa = inv.getArgument(0);
            fa.setId(101L);
            filaEmMemoria.add(fa);
            return fa;
        });
        when(filaRepo.findByStatus(StatusFila.AGUARDANDO)).thenAnswer(inv ->
                filaEmMemoria.stream().filter(f -> f.getStatus() == StatusFila.AGUARDANDO).toList());
        when(filaRepo.findByStatus(StatusFila.EM_ATENDIMENTO)).thenReturn(List.of());

        // Componentes reais integrados: FilaController + FilaServico + TratadorDeErros + Domínio
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FilaController(filaServico))
                .setControllerAdvice(new TratadorDeErros())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(mapper))
                .build();

        String novaTriagemJson = """
                {
                  "pacienteId": 2,
                  "sintomas": "Falta de ar intensa e saturação 86%",
                  "gravidade": "EMERGENCIA"
                }
                """;

        // 1. Ação: Enfermeiro registra triagem de EMERGENCIA via POST /api/triagens
        mockMvc.perform(post("/api/triagens")
                        .requestAttr(SegurancaInterceptor.USUARIO, enfermeiro)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(novaTriagemJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO"))
                .andExpect(jsonPath("$.tempoEsperaEstimado").value(0))
                .andExpect(jsonPath("$.ficha.gravidade").value("EMERGENCIA"));

        // 2. Conferência: GET /api/fila deve colocar Maria (EMERGENCIA) na posição 1, à frente de Carlos
        mockMvc.perform(get("/api/fila")
                        .requestAttr(SegurancaInterceptor.USUARIO, enfermeiro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens.length()").value(2))
                .andExpect(jsonPath("$.itens[0].ficha.paciente.nome").value("Maria Oliveira Silva"))
                .andExpect(jsonPath("$.itens[0].posicao").value(1))
                .andExpect(jsonPath("$.itens[0].tempoEsperaEstimado").value(0))
                .andExpect(jsonPath("$.itens[1].ficha.paciente.nome").value("Carlos Eduardo Souza"))
                .andExpect(jsonPath("$.itens[1].posicao").value(2))
                .andExpect(jsonPath("$.itens[1].tempoEsperaEstimado").value(15));
    }
}