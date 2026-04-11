package com.bip.beneficio.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.servlet.context-path:/api}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Benefício API")
                        .description("""
                                API REST para gerenciamento de benefícios corporativos.

                                ## Funcionalidades

                                - **CRUD completo** de benefícios
                                - **Transferência de valores** entre benefícios com validação de saldo
                                - **Controle de concorrência** via Optimistic Locking
                                - **Paginação e filtros** nas listagens

                                ## Arquitetura

                                A API segue arquitetura em camadas:
                                - **Controller**: Endpoints REST
                                - **Service**: Regras de negócio
                                - **Repository**: Acesso a dados (JPA)
                                - **Entity**: Modelo de domínio

                                ## Tratamento de Erros

                                Todos os erros seguem formato padronizado com:
                                - Timestamp
                                - Código HTTP
                                - Código de erro interno
                                - Mensagem descritiva
                                - Detalhes de validação (quando aplicável)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Equipe de Desenvolvimento")
                                .email("dev@bip.com.br")
                                .url("https://www.bip.com.br"))
                        .license(new License()
                                .name("Proprietário")
                                .url("https://www.bip.com.br/termos")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080" + contextPath)
                                .description("Servidor de Desenvolvimento"),
                        new Server()
                                .url("https://api.bip.com.br" + contextPath)
                                .description("Servidor de Produção")
                ));
    }
}
