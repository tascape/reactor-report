#!/bin/bash -e

docker images
source rr_version

echo "push images to tascape"
docker login
for IMG in nginx tomee mysql; do
  docker push tascape/reactor-report-$IMG:${RR_VERSION}
done
