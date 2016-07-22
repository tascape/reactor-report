#!/bin/bash

wget "https://repository.apache.org/service/local/artifact/maven/redirect?r=snapshots&g=org.apache.openejb&a=apache-tomee&v=7.0.0-SNAPSHOT&c=plus&p=tar.gz" -O /home/vagrant/tomee.tar.gz
tar zxvf /home/vagrant/tomee.tar.gz -C /home/vagrant/
ln -s /home/vagrant/apache-tomee-plus-7.0.0-SNAPSHOT /usr/share/tomee
pushd /usr/share/tomee/conf
  sed '/tomcat-users>/d' tomcat-users.xml > aa.txt
  echo '  <role rolename="tomee-admin" />' >> aa.txt
  echo '  <user username="tomee" password="tomee" roles="tomee-admin,manager-gui" />' >> aa.txt
  echo '</tomcat-users>' >> aa.txt
  mv tomcat-users.xml tomcat-users.xml.bk
  mv aa.txt tomcat-users.xml
  wget "https://oss.sonatype.org/content/repositories/releases/com/tascape/reactor-report/1.2.0/reactor-report-1.2.0.war" -O ../webapps/reactor-report.war
  ../bin/startup.sh
popd

wget "https://raw.githubusercontent.com/tascape/reactor-report/master/doc/tomee" -O /etc/init.d/tomee
chmod 755 /etc/init.d/tomee
update-rc.d tomee defaults
