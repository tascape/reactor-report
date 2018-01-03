#!/bin/bash -e

export RR_VERSION=1.2.12

mkdir -p ~/.reactor/db
mkdir -p ~/.reactor/logs

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

echo "open http://127.0.0.1:30080/rr/"
open http://127.0.0.1:30080/rr/
