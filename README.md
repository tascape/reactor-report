# reactor-report
Reactor report web app (deployed in single-node docker swarm)  
Install the latest [Docker CE](https://store.docker.com/editions/community/docker-ce-desktop-mac), and run ```docker swarm init``` to enable swarm mode.


## 1. deploy with source code
clone repo, run 
```
./build.sh && ./deploy.sh
``` 


## 2. deploy without source code
run ```docker swarm init```, if your docker host in not in swarm mode

```
curl https://raw.githubusercontent.com/tascape/reactor-report/master/deploy.sh | bash
```


## 3. deployment
```
$ docker service ls
ID                  NAME                   MODE                REPLICAS            IMAGE                                         PORTS
7mwq9baai886        reactor_report_tomee   replicated          1/1                 tascape/reactor-report-tomee:1.3.4.g346dc32   *:38080->8080/tcp
hi1easzyumfe        reactor_report_nginx   replicated          1/1                 tascape/reactor-report-nginx:1.3.4.g346dc32   *:30080->80/tcp
nidck4hdb5sm        reactor_report_mysql   replicated          1/1                 tascape/reactor-report-mysql:1.3.4.g346dc32   *:33306->3306/tcp
```


#### After deployment, open reactor-report at http://127.0.0.1:30080/rr/.
