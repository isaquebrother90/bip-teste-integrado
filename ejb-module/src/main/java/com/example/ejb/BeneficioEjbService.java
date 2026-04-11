package com.example.ejb;

import jakarta.ejb.Stateless;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * Serviço EJB para operações com Benefícios.
 *
 * CORREÇÕES IMPLEMENTADAS:
 *
 * O código original apresentava os seguintes problemas:
 *
 * 1. BUG: Sem validação de saldo - permitia saldo negativo
 *    CORREÇÃO: Adicionada validação possuiSaldoSuficiente() antes do débito
 *
 * 2. BUG: Sem locking - possível lost update em concorrência
 *    CORREÇÃO: Implementado Pessimistic Write Lock via EntityManager.find() com LockModeType
 *              + Optimistic Locking via campo @Version na entidade
 *
 * 3. BUG: Sem validação de existência de origem/destino
 *    CORREÇÃO: Lançamento de IllegalArgumentException se não encontrado
 *
 * 4. BUG: Permitia transferência para o mesmo benefício
 *    CORREÇÃO: Validação de origem != destino
 *
 * 5. BUG: Possível deadlock ao adquirir locks em ordem diferente
 *    CORREÇÃO: Ordenação de aquisição de locks (menor ID primeiro)
 *
 * 6. BUG: Sem tratamento transacional adequado
 *    CORREÇÃO: Uso de @TransactionAttribute(REQUIRED) com rollback automático
 *
 * @author Candidato BIP
 */
@Stateless
public class BeneficioEjbService {

    @PersistenceContext
    private EntityManager em;

    /**
     * Realiza transferência de valor entre benefícios de forma segura e consistente.
     *
     * @param fromId ID do benefício de origem (débito)
     * @param toId ID do benefício de destino (crédito)
     * @param amount Valor a ser transferido
     * @throws IllegalArgumentException se parâmetros inválidos
     * @throws IllegalStateException se saldo insuficiente
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRED)
    public void transfer(Long fromId, Long toId, BigDecimal amount) {
        // CORREÇÃO 1: Validação de parâmetros
        validateParameters(fromId, toId, amount);

        // CORREÇÃO 2: Ordenação de locks para evitar deadlocks
        Long firstId = Math.min(fromId, toId);
        Long secondId = Math.max(fromId, toId);

        // CORREÇÃO 3: Pessimistic Write Lock para evitar lost updates
        Beneficio first = em.find(Beneficio.class, firstId, LockModeType.PESSIMISTIC_WRITE);
        Beneficio second = em.find(Beneficio.class, secondId, LockModeType.PESSIMISTIC_WRITE);

        // CORREÇÃO 4: Validação de existência
        if (first == null) {
            throw new IllegalArgumentException("Benefício de origem não encontrado: " + firstId);
        }
        if (second == null) {
            throw new IllegalArgumentException("Benefício de destino não encontrado: " + secondId);
        }

        // Identifica origem e destino após ordenação
        Beneficio from = fromId.equals(firstId) ? first : second;
        Beneficio to = fromId.equals(firstId) ? second : first;

        // CORREÇÃO 5: Validação de saldo ANTES do débito
        if (!from.possuiSaldoSuficiente(amount)) {
            throw new IllegalStateException(
                String.format("Saldo insuficiente. Saldo atual: %s, Valor solicitado: %s",
                    from.getValor(), amount)
            );
        }

        // CORREÇÃO 6: Operações de débito e crédito
        from.setValor(from.getValor().subtract(amount));
        to.setValor(to.getValor().add(amount));

        // Persiste as alterações (merge dentro da transação)
        // O Optimistic Locking via @Version garante consistência adicional
        em.merge(from);
        em.merge(to);
    }

    /**
     * Valida os parâmetros da transferência.
     */
    private void validateParameters(Long fromId, Long toId, BigDecimal amount) {
        if (fromId == null) {
            throw new IllegalArgumentException("ID de origem não pode ser nulo");
        }
        if (toId == null) {
            throw new IllegalArgumentException("ID de destino não pode ser nulo");
        }
        if (amount == null) {
            throw new IllegalArgumentException("Valor da transferência não pode ser nulo");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Valor da transferência deve ser maior que zero");
        }
        if (Objects.equals(fromId, toId)) {
            throw new IllegalArgumentException("Origem e destino não podem ser iguais");
        }
    }
}
