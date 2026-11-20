-- =====================================================================
-- banco de dados do healthcare (postgresql)
-- este arquivo roda sozinho toda vez que o sistema inicia.
-- como usa "if not exists", ele cria as tabelas so na primeira vez
-- e depois nao mexe nos dados que ja estao gravados.
-- as tabelas seguem o diagrama de classes da documentacao (figura 2).
-- =====================================================================

-- usuarios do sistema. administrador, recepcionista, enfermeiro e medico
-- ficam todos nesta mesma tabela (heranca "tabela unica"). a coluna "tipo"
-- diz qual e a classe, e as colunas especificas ficam vazias para os outros.
CREATE TABLE IF NOT EXISTS usuario (
    id             BIGSERIAL PRIMARY KEY,
    tipo           VARCHAR(20)  NOT NULL,          -- administrador, recepcionista, enfermeiro, medico
    nome           VARCHAR(120) NOT NULL,
    login          VARCHAR(50)  UNIQUE,            -- pode ser nulo (medico sem acesso ao sistema)
    senha          VARCHAR(100),                   -- hash bcrypt, nunca a senha aberta (rnf01)
    perfil         VARCHAR(20)  NOT NULL,
    nivel_acesso   VARCHAR(30),                    -- so administrador
    matricula      VARCHAR(30),                    -- so recepcionista
    coren          VARCHAR(30),                    -- so enfermeiro
    crm            VARCHAR(30)  UNIQUE,            -- so medico
    especialidade  VARCHAR(80),                    -- so medico
    disponivel     BOOLEAN      NOT NULL DEFAULT FALSE, -- so medico (rf07)
    ativo          BOOLEAN      NOT NULL DEFAULT TRUE   -- funcionario desativado nao entra no sistema
);

-- se o banco ja existia sem a coluna ativo, acrescenta sem apagar os dados
ALTER TABLE usuario ADD COLUMN IF NOT EXISTS ativo BOOLEAN NOT NULL DEFAULT TRUE;

-- pacientes (rf01). cpf guardado so com numeros e sem repetir.
CREATE TABLE IF NOT EXISTS paciente (
    id               BIGSERIAL PRIMARY KEY,
    nome             VARCHAR(120) NOT NULL,
    cpf              CHAR(11)     NOT NULL UNIQUE,
    data_nascimento  DATE         NOT NULL,
    sexo             VARCHAR(20),
    telefone         VARCHAR(20),
    endereco         VARCHAR(200),
    cadastrado_em    TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- fichas de triagem (rf03), ligadas ao paciente, ao enfermeiro e ao medico responsavel (rnf04).
CREATE TABLE IF NOT EXISTS ficha_triagem (
    id                       BIGSERIAL PRIMARY KEY,
    data_hora                TIMESTAMP   NOT NULL DEFAULT NOW(),
    sintomas                 TEXT        NOT NULL,
    gravidade                VARCHAR(20) NOT NULL,   -- emergencia ... nao_urgente (rd01)
    tempo_maximo_espera_min  INT         NOT NULL,   -- referencia da gravidade (rd03)
    paciente_id              BIGINT      NOT NULL REFERENCES paciente(id),
    enfermeiro_id            BIGINT      REFERENCES usuario(id),
    medico_id                BIGINT      REFERENCES usuario(id)
);

-- fila de atendimento (rf04). cada ficha gera uma entrada na fila.
-- a posicao e o tempo estimado nao sao gravados: sao calculados na hora pelo sistema.
CREATE TABLE IF NOT EXISTS fila_atendimento (
    id                  BIGSERIAL PRIMARY KEY,
    ficha_id            BIGINT      NOT NULL UNIQUE REFERENCES ficha_triagem(id),
    medico_id           BIGINT      REFERENCES usuario(id),
    status              VARCHAR(20) NOT NULL DEFAULT 'AGUARDANDO',
    entrada_em          TIMESTAMP   NOT NULL DEFAULT NOW(),
    inicio_atendimento  TIMESTAMP,
    fim_atendimento     TIMESTAMP
);

-- historico medico (rf05): uma linha por consulta finalizada.
CREATE TABLE IF NOT EXISTS historico_medico (
    id             BIGSERIAL PRIMARY KEY,
    data_consulta  TIMESTAMP NOT NULL DEFAULT NOW(),
    diagnostico    VARCHAR(200) NOT NULL,
    observacoes    TEXT,
    prescricao     TEXT,
    paciente_id    BIGINT NOT NULL REFERENCES paciente(id),
    medico_id      BIGINT REFERENCES usuario(id),
    ficha_id       BIGINT REFERENCES ficha_triagem(id)
);

-- indices para a fila e o historico responderem rapido (rnf05)
CREATE INDEX IF NOT EXISTS idx_fila_status ON fila_atendimento(status);
CREATE INDEX IF NOT EXISTS idx_historico_paciente ON historico_medico(paciente_id);
CREATE INDEX IF NOT EXISTS idx_ficha_paciente ON ficha_triagem(paciente_id);
