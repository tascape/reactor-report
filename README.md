# reactor-report
Reactor report web app

```
$ docker service ls
ID                  NAME                   MODE                REPLICAS            IMAGE                                         PORTS
7mwq9baai886        reactor_report_tomee   replicated          1/1                 tascape/reactor-report-tomee:1.3.4.g346dc32   *:38080->8080/tcp
hi1easzyumfe        reactor_report_nginx   replicated          1/1                 tascape/reactor-report-nginx:1.3.4.g346dc32   *:30080->80/tcp
nidck4hdb5sm        reactor_report_mysql   replicated          1/1                 tascape/reactor-report-mysql:1.3.4.g346dc32   *:33306->3306/tcp
```
