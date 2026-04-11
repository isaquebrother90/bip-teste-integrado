-- Schema para a tabela de Benefícios
-- Compatível com H2 (desenvolvimento) e PostgreSQL (produção)

CREATE TABLE IF NOT EXISTS beneficio (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    descricao VARCHAR(255),
    valor DECIMAL(15,2) NOT NULL,
    ativo BOOLEAN DEFAULT TRUE,
    version BIGINT DEFAULT 0,
    criado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_valor_positivo CHECK (valor >= 0),
    CONSTRAINT chk_nome_nao_vazio CHECK (LENGTH(TRIM(nome)) > 0)
);

CREATE INDEX IF NOT EXISTS idx_beneficio_nome ON beneficio(nome);
CREATE INDEX IF NOT EXISTS idx_beneficio_ativo ON beneficio(ativo);
