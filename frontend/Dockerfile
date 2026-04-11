# Multi-stage build para Angular

# Stage 1: Build
FROM node:20-alpine AS build
WORKDIR /app

# Copiar arquivos de dependência
COPY package*.json ./

# Instalar dependências
RUN npm install --legacy-peer-deps

# Copiar código fonte
COPY . .

# Build de produção
RUN npm run build:prod

# Stage 2: Runtime com Nginx
FROM nginx:alpine

# Remover configuração default
RUN rm -rf /usr/share/nginx/html/*

# Copiar build do Angular
COPY --from=build /app/dist/beneficio-app/browser /usr/share/nginx/html

# Configuração customizada do Nginx
COPY nginx.conf /etc/nginx/conf.d/default.conf

# Expor porta
EXPOSE 80

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=10s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:80 || exit 1

CMD ["nginx", "-g", "daemon off;"]
