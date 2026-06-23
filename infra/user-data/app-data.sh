#!/bin/bash

dnf update -y
dnf install -y docker

systemctl enable docker
systemctl start docker

fallocate -l 4G /swapfile
chmod 600 /swapfile
mkswap /swapfile
swapon /swapfile

echo '/swapfile swap swap defaults 0 0' >> /etc/fstab