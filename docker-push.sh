#!/bin/bash -e

docker images
source rr_version

echo "log into tascape (https://hub.docker.com/u/tascape/)"
docker login -u tascape

echo "push images to tascape"
for IMG in nginx tomee mysql; do
  docker push tascape/reactor-report-$IMG:${RR_VERSION}
  docker push tascape/reactor-report-$IMG:latest
done
