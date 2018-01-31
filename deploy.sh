#!/bin/bash -e

#RR_VERSION=${RR_VERSION?please provide env var RR_VERSION}
$(curl https://raw.githubusercontent.com/tascape/reactor-report/master/rr_version)
echo "reactor-report version is ${RR_VERSION}"

mkdir -p ~/.reactor/db
mkdir -p ~/.reactor/logs
mkdir -p ~/.reactor/webui

if [[ ! -f docker-compose.yml ]]; then
  wget https://raw.githubusercontent.com/tascape/reactor-report/master/docker-compose.yml -O docker-compose.yml
fi

waitForServices() {
  docker service ls | grep reactor_report

  ONLINE=0
  echo -en "wait for services "
  while [[ $ONLINE -ne 3 ]]; do
    # docker service ls | grep reactor_report
    sleep 2 && echo -n "."
    ONLINE=$(docker service ls | grep reactor_report | grep "1/1" | wc -l)
  done
  echo ":)"

  docker service ls | grep reactor_report
  sleep 10
}

docker version || (echo "where is docker?"; exit 1)

docker stack deploy -c docker-compose.yml reactor
waitForServices

cat "reactor.db.type=mysql" >> ~/.reactor/reactor.properties
cat "reactor.db.host=localhost:33306" >> ~/.reactor/reactor.properties

echo "open http://127.0.0.1:30080/rr/"
open http://127.0.0.1:30080/rr/
