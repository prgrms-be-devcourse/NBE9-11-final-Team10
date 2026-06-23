
# Terraform 자체 설정 블록입니다.
# 여기서는 어떤 Provider(클라우드/서비스 연동 플러그인)를 사용할지 정의합니다.
terraform {
  # 이 Terraform 코드에서 필요한 Provider 목록을 선언합니다.
  required_providers {
    # AWS 리소스를 생성/관리하기 위해 AWS Provider를 사용합니다.
    aws = {
      # HashiCorp에서 제공하는 공식 AWS Provider를 사용합니다.
      source = "hashicorp/aws"

      # AWS Provider의 메이저 버전 5.x를 사용하도록 고정합니다.
      # 예: 5.0 이상, 6.0 미만 버전 사용
      version = "~> 5.0"
    }
  }
}

# AWS Provider 설정입니다.
# Terraform이 AWS API를 호출할 때 사용할 리전 정보를 지정합니다.
provider "aws" {
  region  = var.region
  profile = var.profile
}

# VPC를 생성합니다.
resource "aws_vpc" "main" {
  # VPC에서 사용할 사설 IP 대역입니다.
  # 10.0.0.0/16은 10.0.x.x 범위의 IP를 이 VPC 안에서 사용할 수 있다는 의미입니다.
  cidr_block = "10.0.0.0/16"

  # VPC 내부 리소스가 DNS 해석을 사용할 수 있도록 활성화합니다.
  # 예를 들어 AWS 내부 도메인 이름을 IP로 변환할 때 필요합니다.
  enable_dns_support = true

  # VPC 내부 리소스에 DNS 호스트 이름을 부여할 수 있도록 활성화합니다.
  # EC2 Public DNS나 Private DNS 사용에 필요합니다.
  enable_dns_hostnames = true

  # AWS 리소스에 붙이는 태그입니다.
  tags = {
    Name = "team10-vpc" # AWS 콘솔에서 보이는 VPC 이름입니다.
    Team = var.team     # 팀 공통 태그입니다.
  }
}

# public subnet 설정
resource "aws_subnet" "public" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.1.0/24"
  availability_zone       = "${var.region}a"
  map_public_ip_on_launch = true

  tags = {
    Name = "team10-public-subnet"
    Team = var.team
  }
}

# private subnet 설정
resource "aws_subnet" "private" {
  vpc_id                  = aws_vpc.main.id
  cidr_block              = "10.0.2.0/24"
  availability_zone       = "${var.region}b"
  map_public_ip_on_launch = false

  tags = {
    Name = "team10-private-subnet"
    Team = var.team
  }
}

# Internet Gateway를 생성합니다.
# Internet Gateway는 VPC가 외부 인터넷과 통신할 수 있도록 연결해주는 출입문 역할을 합니다.
# Public Subnet에 위치한 EC2가 인터넷에서 요청을 받거나, 인터넷으로 나갈 때 필요합니다.
resource "aws_internet_gateway" "main" {
  # Internet Gateway를 연결할 VPC ID입니다.
  # 위에서 생성한 team10-vpc에 Internet Gateway를 연결합니다.
  vpc_id = aws_vpc.main.id

  # AWS 리소스에 붙이는 태그입니다.
  tags = {
    Name = "team10-igw" # AWS 콘솔에서 보이는 Internet Gateway 이름입니다.
    Team = var.team     # 팀 공통 태그입니다.
  }
}

# Public Route Table을 생성합니다.
# Route Table은 Subnet의 네트워크 트래픽을 어디로 보낼지 결정하는 라우팅 규칙 모음입니다.
# 이 Route Table은 Public Subnet에 연결해서 인터넷 통신이 가능하도록 사용할 예정입니다.
resource "aws_route_table" "public" {
  # 이 Route Table이 소속될 VPC ID입니다.
  vpc_id = aws_vpc.main.id

  # 외부 인터넷으로 나가는 모든 트래픽에 대한 라우팅 규칙입니다.
  route {
    # 0.0.0.0/0은 VPC 내부 대역을 제외한 모든 IPv4 목적지를 의미합니다.
    # 쉽게 말해, 인터넷으로 나가는 모든 요청을 의미합니다.
    cidr_block = "0.0.0.0/0"

    # 위 트래픽을 Internet Gateway로 보내도록 설정합니다.
    gateway_id = aws_internet_gateway.main.id
  }

  # AWS 리소스에 붙이는 태그입니다.
  tags = {
    Name = "team10-public-rt" # AWS 콘솔에서 보이는 Public Route Table 이름입니다.
    Team = var.team           # 팀 공통 태그입니다.
  }
}

# Public Subnet과 Public Route Table을 연결합니다.
# 이 연결이 있어야 Public Subnet에 있는 EC2가 0.0.0.0/0 -> IGW 라우팅 규칙을 사용할 수 있습니다.
resource "aws_route_table_association" "public" {
  # Public Route Table을 적용할 Subnet ID입니다.
  subnet_id = aws_subnet.public.id

  # Public Subnet에 연결할 Route Table ID입니다.
  route_table_id = aws_route_table.public.id
}
