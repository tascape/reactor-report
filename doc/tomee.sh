#!/bin/bash

wget "http://repo.maven.apache.org/maven2/org/apache/tomee/apache-tomee/7.0.1/apache-tomee-7.0.1-plus.tar.gz" -O /home/vagrant/tomee.tar.gz
tar zxvf /home/vagrant/tomee.tar.gz -C /home/vagrant/
ln -s /home/vagrant/apache-tomee-plus-7.0.1 /usr/share/tomee
pushd /usr/share/tomee/conf
  sed '/tomcat-users>/d' tomcat-users.xml > aa.txt
  echo '  <role rolename="tomee-admin" />' >> aa.txt
  echo '  <user username="tomee" password="tomee" roles="tomee-admin,manager-gui" />' >> aa.txt
  echo '</tomcat-users>' >> aa.txt
  mv tomcat-users.xml tomcat-users.xml.bk
  mv aa.txt tomcat-users.xml
  wget "https://oss.sonatype.org/content/repositories/releases/com/tascape/reactor-report/1.2.0/reactor-report-1.2.0.war" -O ../webapps/rr.war
  ../bin/startup.sh
popd

wget "https://raw.githubusercontent.com/tascape/reactor-report/master/doc/tomee" -O /etc/init.d/tomee
chmod 755 /etc/init.d/tomee
update-rc.d tomee defaults

