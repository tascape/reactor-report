#!/bin/bash

mkdir -p $HOME/.reactor
mkdir -p $HOME/.m2 
 
if (type vagrant) && (type virtualbox) then

    echo "get latest vagrant file"
    wget https://raw.githubusercontent.com/tascape/reactor-report/master/doc/Vagrantfile -O Vagrantfile
    vagrant up

    export PROP=$HOME/.reactor/reactor.properties
    echo "create reactor system properties file" $PROP
cat <<EOF > $PROP
# reactor system properties
# use -Dkey=value to override or add in commandline
reactor.db.type=mysql
reactor.db.host=localhost:23306
reactor.log.path=$HOME/.reactor/logs
reactor.case.station=localhost
reactor.JOB_NAME=local-run
EOF
cat $PROP

    echo "check report at http://localhost:28088/rr/suites_result.xhtml"
    open "http://localhost:28088/rr/suites_result.xhtml" || echo "OK"

else
    echo "you need to first install vagrant (http://www.vagrantup.com/downloads.html) and virtualbox (https://www.virtualbox.org/wiki/Downloads)"
fi
