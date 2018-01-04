FROM tomee:8-jre-7.0.4-plus

RUN sed '/tomcat-users>/d' /usr/local/tomee/conf/tomcat-users.xml > aa.txt && \
    echo '  <role rolename="tomee-admin" />' >> aa.txt && \
    echo '  <user username="tomee" password="tomee" roles="tomee-admin,manager-gui" />' >> aa.txt && \
    echo '</tomcat-users>' >> aa.txt && \
    mv aa.txt /usr/local/tomee/conf/tomcat-users.xml

RUN apt-get update && apt-get -y install netcat

ARG WAR_FILE
COPY target/${WAR_FILE} /usr/local/tomee/webapps/rr.war
COPY target/classes/reactor-db.properties /usr/local/reactor/reactor-db.properties
