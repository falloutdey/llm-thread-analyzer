# Etapa 1: Construir o projeto React
FROM node:16 AS build

# Defina o diretório de trabalho
WORKDIR /app

# Copie e instale as dependências
COPY package*.json ./
RUN npm install

# Copie os arquivos e construa o projeto
COPY . ./
RUN npm run build

# Comando para rodar o servidor de desenvolvimento (Comentar as linhas abaixo quando iniciar o servidor apache)
EXPOSE 3000
CMD ["npm", "start"] 

# Etapa 2: Servir com Apache
FROM httpd:latest

# Copie os arquivos construídos para o diretório do Apache
COPY --from=build /app/build /usr/local/apache2/htdocs/

# Adicione no Dockerfile do frontend, após a etapa de cópia:
RUN echo "ServerName localhost" >> /usr/local/apache2/conf/httpd.conf

# Exponha a porta 80
EXPOSE 80

# O Apache já está configurado para iniciar automaticamente
