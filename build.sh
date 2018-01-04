#!/bin/bash -e

#RR_BASE_VERSION=$(git describe --abbrev=0 --tags)
#RR_BUILD_NUM=$(git rev-list --count --first-parent ${RR_BASE_VERSION}..HEAD)
#RR_VERSION=${RR_BASE_VERSION}.${RR_BUILD_NUM}

RR_VERSION=$(git describe --long --tags | tr - .)
echo "reactor-report version is ${RR_VERSION}"
echo "${RR_VERSION}" > rr_version

mvn versions:set -DnewVersion=${RR_VERSION}
mvn clean package

docker images
echo "push images to tascape"
docker login
for IMG in nginx tomee mysql; do
  docker push tascape/reactor-report-$IMG:${RR_VERSION}
done
