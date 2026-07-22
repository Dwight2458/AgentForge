FROM node:24.4.1-alpine AS build
WORKDIR /app
COPY apps/web-console/package.json apps/web-console/package-lock.json ./
RUN npm ci
COPY apps/web-console/ ./
RUN npm run build-only

FROM nginxinc/nginx-unprivileged:1.29.0-alpine
ENV NGINX_RESOLVER=127.0.0.11 \
    CONTROL_PLANE_HOST=agentforge-control-plane:8080
COPY apps/web-console/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 8080
