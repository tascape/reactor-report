#!/bin/bash

mkdir -p $HOME/.reactor
 
if (type vagrant) && (type virtualbox) then

    echo "get latest vagrant file"
    wget https://raw.githubusercontent.com/tascape/reactor-report/master/doc/Vagrantfile -O Vagrantfile
    vagrant up

    export PROP=$HOME/.reactor/reactor.properties
    echo "create reactor system properties file" $PROP
    echo "# reactor system properties" > $PROP
    echo "# use -Dkey=value to override or add in commandline " >> $PROP
    echo "reactor.db.type=mysql" >> $PROP
    echo "reactor.db.host=localhost:23306" >> $PROP
    echo "reactor.log.path=$HOME/.reactor/logs" >> $PROP
    echo "reactor.case.station=localhost" >> $PROP
    echo "reactor.JOB_NAME=local-run" >> $PROP
    cat $PROP

    echo "check report at http://localhost:28088/rr/suites_result.xhtml"
    open "http://localhost:28088/rr/suites_result.xhtml" || echo "OK"

else
    echo "you need to first install vagrant (http://www.vagrantup.com/downloads.html) and virtualbox (https://www.virtualbox.org/wiki/Downloads)"
fi
