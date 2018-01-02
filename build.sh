#!/bin/bash -e

RR_BASE_VERSION=1.2

mvn clean package

docker images
echo "push images to tascape"
docker login
