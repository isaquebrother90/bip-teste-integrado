-- Schema para o sistema de gerenciamento de benefícios
-- Compatível com H2 (desenvolvimento) e PostgreSQL (produção)

-- ============================================================
-- TABELA: beneficio
-- ============================================================
CREATE TABLE IF NOT EXISTS beneficio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    valor DECIMAL(15,2) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    -- Soft delete
    deletado_em TIMESTAMP,
    motivo_desativacao VARCHAR(255),
    CONSTRAINT chk_valor_positivo CHECK (valor >= 0),
    CONSTRAINT chk_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_beneficio_nome ON beneficio(nome);
CREATE INDEX IF NOT EXISTS idx_beneficio_ativo ON beneficio(ativo);
CREATE INDEX IF NOT EXISTS idx_beneficio_deletado ON beneficio(deletado_em);

-- ============================================================
-- TABELA: transferencia
-- Registro imutável de eventos financeiros.
-- Nunca deletar ou alterar registros desta tabela.
-- ============================================================
CREATE TABLE IF NOT EXISTS transferencia (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    correlacao_id VARCHAR(36) NOT NULL UNIQUE,
    origem_id BIGINT NOT NULL,
    destino_id BIGINT NOT NULL,
    valor DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL,
    -- Snapshots de saldo (auditoria regulatória)
    saldo_origem_antes DECIMAL(15,2),
    saldo_origem_depois DECIMAL(15,2),
    saldo_destino_antes DECIMAL(15,2),
    saldo_destino_depois DECIMAL(15,2),
    motivo_falha VARCHAR(500),
    iniciada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    finalizada_em TIMESTAMP,
    CONSTRAINT chk_transferencia_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_origem_destino_diferentes CHECK (origem_id != destino_id)
);

CREATE INDEX IF NOT EXISTS idx_transferencia_correlacao ON transferencia(correlacao_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_origem ON transferencia(origem_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_destino ON transferencia(destino_id);
CREATE INDEX IF NOT EXISTS idx_transferencia_status ON transferencia(status);
CREATE INDEX IF NOT EXISTS idx_transferencia_iniciada ON transferencia(iniciada_em);
