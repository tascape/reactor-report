FROM nginx:alpine

RUN apk add --no-cache --virtual .build-deps netcat-openbsd \
    && apk del .build-deps
COPY wait-for /usr/bin/wait-for
COPY reactor.conf /etc/nginx/conf.d/default.conf
