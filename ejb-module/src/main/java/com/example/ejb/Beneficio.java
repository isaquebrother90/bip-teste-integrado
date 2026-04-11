package com.example.ejb;

import jakarta.persistence.*;
import java.math.BigDecimal;

/**
 * Entidade JPA para o módulo EJB.
 */
@Entity
@Table(name = "beneficio")
public class Beneficio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", length = 255)
    private String descricao;

    @Column(name = "valor", nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;

    /**
     * Campo de versão para Optimistic Locking.
     * Previne lost updates em operações concorrentes.
     */
    @Version
    @Column(name = "version")
    private Long version;

    // Getters e Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    /**
     * Verifica se o benefício possui saldo suficiente para débito.
     *
     * @param montante Valor a ser debitado
     * @return true se o saldo é suficiente
     */
    public boolean possuiSaldoSuficiente(BigDecimal montante) {
        return this.valor != null &&
               montante != null &&
               this.valor.compareTo(montante) >= 0;
    }
}
