### case development environment

* first time setup, run the following line in console, the user/pass of the newly create Ubuntu VM is vagrant/vagrant

```
mkdir -p ~/.reactor && cd ~/.reactor && wget https://raw.githubusercontent.com/tascape/reactor-report/master/doc/reactor.sh -O reactor.sh && bash reactor.sh
```


* restart environment, after host machine reboot

```
cd ~/.reactor && vagrant up
```


* destroy environment (for re-setup)

```
cd ~/.reactor && vagrant destroy
```


(tested on OS X Yosemite 10.10.5, with Vagrant 1.7.4 and VirtualBox 5.0.2)  
