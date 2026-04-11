package com.bip.beneficio.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Configuração de cache em memória com Caffeine.
 *
 * <p><b>Regra crítica para sistemas financeiros:</b></p>
 * <p>Somente metadados estáticos são cacheados (nome, descrição, ativo).
 * O campo {@code valor} (saldo) NUNCA é cacheado — cache stale de saldo pode causar
 * aprovação indevida de transferências com saldo insuficiente.</p>
 *
 * <p>Caches disponíveis:</p>
 * <ul>
 *   <li>{@code beneficios-metadata} — busca por nome, TTL 5min</li>
 *   <li>{@code beneficios-lista} — listagem paginada, TTL 60s</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache de metadata de benefícios (nome/descrição para busca).
     * TTL curto pois nomes podem ser atualizados.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return manager;
    }
}
