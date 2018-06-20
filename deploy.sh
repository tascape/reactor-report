#!/bin/bash -e

mkdir -p $HOME/.reactor/db
mkdir -p $HOME/.reactor/logs
mkdir -p $HOME/.reactor/webui

if [[ ! -f docker-stack.yaml ]]; then
  cd $HOME/.reactor/
  wget https://raw.githubusercontent.com/tascape/reactor-report/master/docker-stack.yaml -O docker-stack.yaml
fi

removeStack() {
  [[ $(docker stack rm reactor) ]] && sleep 10
  while [[ $(docker network ls | grep reactor_reactor) ]]; do
    sleep 5
  done
}

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

removeStack
docker stack deploy -c docker-stack.yaml reactor
waitForServices

echo "reactor.db.type=mysql" >> $HOME/.reactor/reactor.properties
echo "reactor.db.host=localhost:33306" >> $HOME/.reactor/reactor.properties
echo "reactor.log.path=$HOME/.reactor/logs" >> $HOME/.reactor/reactor.properties

echo "open report at http://127.0.0.1:30080/rr/"
open http://127.0.0.1:30080/rr/
