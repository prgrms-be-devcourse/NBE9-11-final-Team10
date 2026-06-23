#!/bin/bash

dnf update -y
dnf install -y docker

systemctl enable docker
systemctl start docker

mkdir -p /dockerProjects/npm/data
mkdir -p /dockerProjects/npm/letsencrypt

docker run -d \
  --name npm \
  --restart unless-stopped \
  -p 80:80 \
  -p 81:81 \
  -p 443:443 \
  -v /dockerProjects/npm/data:/data \
  -v /dockerProjects/npm/letsencrypt:/etc/letsencrypt \
  jc21/nginx-proxy-manager:latest