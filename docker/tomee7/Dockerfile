
FROM tascape/reactor-report:java8

ADD http://repo.maven.apache.org/maven2/org/apache/tomee/apache-tomee/7.0.1/apache-tomee-7.0.1-plus.tar.gz tomee.tar.gz

RUN tar zxvf tomee.tar.gz && \
    rm tomee.tar.gz && \
    mv apache-tomee-plus-7.0.1 /usr/share/tomee

RUN sed '/tomcat-users>/d' /usr/share/tomee/conf/tomcat-users.xml > aa.txt && \
    echo '  <role rolename="tomee-admin" />' >> aa.txt && \
    echo '  <user username="tomee" password="tomee" roles="tomee-admin,manager-gui" />' >> aa.txt && \
    echo '</tomcat-users>' >> aa.txt && \
    mv aa.txt /usr/share/tomee/conf/tomcat-users.xml

ADD https://oss.sonatype.org/content/repositories/releases/com/tascape/reactor-report/1.2.1/reactor-report-1.2.1.war /usr/share/tomee/webapps/rr.war

EXPOSE 8080
EXPOSE 8009

WORKDIR /usr/share/tomee

ENTRYPOINT ["bin/catalina.sh"]

CMD ["run"]
