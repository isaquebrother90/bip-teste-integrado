-- Dados iniciais para testes e desenvolvimento
-- Executado automaticamente no startup da aplicação em ambiente de desenvolvimento

-- Limpa dados existentes (apenas para desenvolvimento)
DELETE FROM transferencia;
DELETE FROM beneficio;

-- Insere dados de exemplo
INSERT INTO beneficio (nome, descricao, valor, ativo, version) VALUES
('Vale Alimentação', 'Benefício para alimentação diária do colaborador', 800.00, TRUE, 0),
('Vale Refeição', 'Benefício para refeições durante o expediente', 1000.00, TRUE, 0),
('Plano de Saúde', 'Cobertura médica completa para o colaborador', 500.00, TRUE, 0),
('Plano Odontológico', 'Cobertura odontológica básica', 150.00, TRUE, 0),
('Gympass', 'Acesso a academias e atividades físicas', 100.00, TRUE, 0),
('Auxílio Creche', 'Auxílio para creche de filhos até 6 anos', 400.00, TRUE, 0),
('Seguro de Vida', 'Seguro de vida em grupo', 50.00, FALSE, 0),
('Vale Transporte', 'Auxílio para deslocamento casa-trabalho', 300.00, TRUE, 0);
