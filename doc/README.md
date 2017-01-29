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

* shared folder mounting error
```
vagrant plugin install vagrant-vbguest
```


(tested on macOS Sierra 10.12.3, with Vagrant 1.9.1 and VirtualBox 5.0.4)  
