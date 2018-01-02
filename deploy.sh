#!/bin/bash -e

export RR_VERSION=1.2.11

docker stack deploy -c docker-compose.yml rr
