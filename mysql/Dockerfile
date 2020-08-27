FROM mysql/mysql-server:8.0
# need to turn off ONLY_FULL_GROUP_BY in compose file
# https://stackoverflow.com/questions/23921117/disable-only-full-group-by

COPY reactor.sql /docker-entrypoint-initdb.d/reactor.sql
