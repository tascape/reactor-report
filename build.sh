#!/bin/bash -e

#RR_BASE_VERSION=$(git describe --abbrev=0 --tags)
#RR_BUILD_NUM=$(git rev-list --count --first-parent ${RR_BASE_VERSION}..HEAD)
#RR_VERSION=${RR_BASE_VERSION}.${RR_BUILD_NUM}

#RR=$(xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml)
#RR=$(mvn -Dexec.executable='echo' -Dexec.args='${project.version}' --non-recursive exec:exec -q)

# need git tag for major release, such as 1.3
# git tag -a 1.3 -m "version 1.3"
# git push --tag
#
RR_VERSION=$(git describe --long --tags | tr - .)
echo "reactor-report version is ${RR_VERSION}"
echo "export RR_VERSION=${RR_VERSION}" > rr_version

mvn versions:set -DnewVersion=${RR_VERSION}
mvn clean package
for IMG in nginx tomee mysql; do
  docker tag tascape/reactor-report-$IMG:${RR_VERSION} tascape/reactor-report-$IMG:latest
done
docker images
