FROM nginx:1.18

RUN apt-get update && \
    apt-get install --no-install-recommends --no-install-suggests -y curl
COPY reactor.conf /etc/nginx/conf.d/default.conf
