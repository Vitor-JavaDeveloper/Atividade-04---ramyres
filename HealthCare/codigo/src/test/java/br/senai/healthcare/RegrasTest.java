package br.senai.healthcare;

import br.senai.healthcare.modelo.Administrador;
import br.senai.healthcare.modelo.FichaTriagem;
import br.senai.healthcare.modelo.FilaAtendimento;
import br.senai.healthcare.modelo.NivelGravidade;
import br.senai.healthcare.modelo.PerfilUsuario;
import br.senai.healthcare.modelo.StatusFila;
import br.senai.healthcare.modelo.Usuario;
import br.senai.healthcare.servico.CadastroServico;
import br.senai.healthcare.servico.ErroApi;
import br.senai.healthcare.servico.UsuarioServico;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * testes unitarios das regras principais (eap 1.5.1 testes unitarios).
 * nao precisam de banco: testam so a logica das classes.
 * para rodar: mvn test.
 */
class RegrasTest {

    @Test
    void cpfValidoEInvalido() {
        assertTrue(CadastroServico.cpfValido("12345678909"));   // digitos certos
        assertFalse(CadastroServico.cpfValido("12345678900"));  // digito verificador errado
        assertFalse(CadastroServico.cpfValido("11111111111"));  // todos iguais nao vale
        assertFalse(CadastroServico.cpfValido("123"));          // tamanho errado
    }

    @Test
    void gravidadeSegueOrdemDePrioridade() {
        // rd02: a ordem do enum e a ordem da fila, emergencia primeiro
        assertTrue(NivelGravidade.EMERGENCIA.ordinal() < NivelGravidade.MUITO_URGENTE.ordinal());
        assertTrue(NivelGravidade.POUCO_URGENTE.ordinal() < NivelGravidade.NAO_URGENTE.ordinal());
        // rd03: cada nivel tem um tempo maximo de referencia
        assertEquals(0, NivelGravidade.EMERGENCIA.getTempoMaximoEsperaMin());
        assertEquals(240, NivelGravidade.NAO_URGENTE.getTempoMaximoEsperaMin());
    }

    @Test
    void tempoDeEsperaDependeDaFilaEDosMedicos() {
        FichaTriagem ficha = new FichaTriagem();
        ficha.classificarGravidade(NivelGravidade.URGENTE);
        FilaAtendimento item = new FilaAtendimento();
        item.setFicha(ficha);
        // 4 pacientes na frente, consulta de 15 min, 2 medicos livres = 30 min
        assertEquals(30, item.calcularTempoEspera(4, 15, 2));
        // sem medico livre conta como 1 para nao dividir por zero
        assertEquals(60, item.calcularTempoEspera(4, 15, 0));

        // emergencia e sempre atendimento imediato
        ficha.classificarGravidade(NivelGravidade.EMERGENCIA);
        assertEquals(0, item.calcularTempoEspera(10, 15, 1));
    }

    @Test
    void statusMarcaHorarios() {
        FilaAtendimento item = new FilaAtendimento();
        item.atualizarStatus(StatusFila.EM_ATENDIMENTO);
        assertNotNull(item.getInicioAtendimento());
        item.atualizarStatus(StatusFila.FINALIZADO);
        assertNotNull(item.getFimAtendimento());
    }

    @Test
    void autenticarConfereLoginESenhaComBcrypt() {
        Usuario u = new Usuario("Ana Souza", "ana", "segredo", PerfilUsuario.RECEPCIONISTA);
        assertTrue(u.autenticar("ana", "segredo"));
        assertTrue(u.autenticar("ANA", "segredo")); // login nao diferencia maiuscula
        assertFalse(u.autenticar("ana", "errada"));
        assertFalse(u.autenticar("outro", "segredo"));
        assertTrue(u.isAtivo()); // usuario novo entra ativo
    }

    @Test
    void naoDesativaASiMesmoNemOUltimoAdmin() {
        Administrador admin = new Administrador("A", "admin", "admin123", "TOTAL");
        admin.setId(1L);
        Administrador outro = new Administrador("B", "admin2", "admin123", "TOTAL");
        outro.setId(2L);

        ErroApi euMesmo = assertThrows(ErroApi.class, () -> UsuarioServico.conferirDesativacao(admin, admin, 2));
        assertTrue(euMesmo.getMessage().toLowerCase().contains("próprio") || euMesmo.getMessage().toLowerCase().contains("proprio"));

        ErroApi ultimo = assertThrows(ErroApi.class, () -> UsuarioServico.conferirDesativacao(admin, outro, 1));
        assertTrue(ultimo.getMessage().toLowerCase().contains("último") || ultimo.getMessage().toLowerCase().contains("ultimo"));

        // com dois admins ativos, um pode desativar o outro
        assertDoesNotThrow(() -> UsuarioServico.conferirDesativacao(admin, outro, 2));
    }
}
