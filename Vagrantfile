Vagrant.configure("2") do |config|
  config.vm.box = "ubuntu/bionic64"
  config.vm.provider "virtualbox" do |vb|
    # vb.gui = true
    # Customize the amount of memory on the VM:
    vb.memory = "1024"
    vb.cpus = "1"
  end
  #config.vbguest.iso_path = "https://download.virtualbox.org/virtualbox/6.1.10/VBoxGuestAdditions_6.1.10.iso"
  config.vm.synced_folder "#{Dir.home}/.rsocket", "/home/vagrant/.rsocket"

  config.vm.provision "shell", inline: <<-SHELL
      add-apt-repository ppa:openjdk-r/ppa -y
      apt-get update
      echo "\n----- Installing Java 8 ------\n"
      apt-get -y install openjdk-8-jdk
      update-alternatives --config java
      curl -LSfs https://japaric.github.io/trust/install.sh | sh -s -- --git casey/just --target x86_64-unknown-linux-musl --to /usr/bin
    SHELL

  config.vm.define "broker1" do |broker|
    broker.vm.network "private_network", ip: "192.168.11.11"
  end

  config.vm.define "broker2" do |broker|
    broker.vm.network "private_network", ip: "192.168.11.12"
  end

  config.vm.define "broker3" do |broker|
    broker.vm.network "private_network", ip: "192.168.11.13"
  end
end