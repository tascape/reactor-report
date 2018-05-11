#!/bin/bash -e

docker images
source rr_version

echo "push images to tascape"
docker login
for IMG in nginx tomee mysql; do
  docker tag tascape/reactor-report-$IMG:${RR_VERSION} tascape/reactor-report-$IMG:latest
  docker push tascape/reactor-report-$IMG:latest
  docker push tascape/reactor-report-$IMG:${RR_VERSION}
done
