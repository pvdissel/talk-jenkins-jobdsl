# -*- mode: ruby -*-
# vi: set ft=ruby :

VAGRANTFILE_API_VERSION = "2"

Vagrant.configure(VAGRANTFILE_API_VERSION) do |config|
    config.vm.define :docker do |docker|
        docker.vm.box = "precise64"
        docker.vm.box_url = "http://files.vagrantup.com/precise64.box"
        docker.vm.network "forwarded_port", guest: 80, host:58080
        docker.vm.network "forwarded_port", guest: 2375, host: 2375
        $script = <<SCRIPT
wget -q -O - https://get.docker.io/gpg | apt-key add -
echo deb http://get.docker.io/ubuntu docker main > /etc/apt/sources.list.d/docker.list
apt-get update -qq 
apt-get install -q -y --force-yes lxc-docker
usermod -a -G docker vagrant
sed -e 's/DOCKER_OPTS=/DOCKER_OPTS=\"-H 0.0.0.0:2375\"/g' /etc/init/docker.conf > /vagrant/docker.conf.sed
cp /vagrant/docker.conf.sed /etc/init/docker.conf
rm -f /vagrant/docker.conf.sed
service docker restart
SCRIPT
       docker.vm.provision :shell, :inline => $script
    end
   config.vm.synced_folder ".", "/vagrant", type: "nfs"
#  config.vm.synced_folder "/Users", "/Users", type: "nfs"
  config.vm.network :private_network, ip: "10.11.12.13"
 end