CREATE TABLE IF NOT EXISTS chamados (
    -- Dados comuns a todo chamado
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    tipo                  TEXT NOT NULL CHECK (tipo IN ('INCIDENTE', 'REQUISICAO')),
    titulo                TEXT NOT NULL,
    descricao             TEXT NOT NULL,
    solicitante           TEXT NOT NULL,
    setor                 TEXT NOT NULL,
    categoria             TEXT NOT NULL,
    prioridade            TEXT CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    status                TEXT NOT NULL DEFAULT 'ABERTO'
                          CHECK (status IN ('ABERTO', 'EM_ANDAMENTO', 'CONCLUIDO', 'CANCELADO')),
    data_abertura         TEXT NOT NULL,
    prazo_limite          TEXT,
    data_fechamento       TEXT,
    motivo_cancelamento   TEXT,

    -- Só para Incidente
    sistema_indisponivel  INTEGER,
    ambiente_afetado      TEXT,

    -- Só para Requisicao
    justificativa_negocio TEXT,
    precisa_aprovacao     INTEGER,
    aprovado              INTEGER
);