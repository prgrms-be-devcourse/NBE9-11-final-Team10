
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

# Edge 서버용 Security Group을 생성합니다.
# SSH 접속은 열지 않고, 추후 EC2에 SSM IAM Role을 붙여 Session Manager로 접속합니다.
resource "aws_security_group" "edge" {
  name        = "team10-edge-sg"
  description = "Security group for edge server"
  vpc_id      = aws_vpc.main.id

  ingress {
    description = "HTTP"
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "HTTPS"
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  ingress {
    description = "Nginx Proxy Manager Admin"
    from_port   = 81
    to_port     = 81
    protocol    = "tcp"
    cidr_blocks = [var.admin_cidr]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "team10-edge-sg"
    Team = var.team
  }
}

# Public Subnet에 배치하되 외부에서 직접 접근하지 못하도록 Security Group으로 제한합니다.
# SSH 접속은 열지 않고, 추후 EC2에 SSM IAM Role을 붙여 Session Manager로 접속합니다.
resource "aws_security_group" "app_data" {
  name        = "team10-app-data-sg"
  description = "Security group for app-data server"
  vpc_id      = aws_vpc.main.id

  ingress {
    description     = "Spring App #1"
    from_port       = 8081
    to_port         = 8081
    protocol        = "tcp"
    security_groups = [aws_security_group.edge.id]
  }

  ingress {
    description     = "Spring App #2"
    from_port       = 8082
    to_port         = 8082
    protocol        = "tcp"
    security_groups = [aws_security_group.edge.id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = {
    Name = "team10-app-data-sg"
    Team = var.team
  }
}

# EC2 인스턴스에 연결할 IAM Role을 생성합니다.
# 이 Role은 EC2가 AWS Systems Manager(SSM)와 통신할 수 있도록 권한을 부여하는 용도입니다.
# SSH 포트(22)를 열지 않고 Session Manager로 EC2에 접속하기 위해 필요합니다.
resource "aws_iam_role" "ec2_ssm_role" {
  name = "team10-ec2-ssm-role"

  # 신뢰 정책입니다.
  # 이 Role을 어떤 AWS 서비스가 사용할 수 있는지 정의합니다.
  # 여기서는 EC2 서비스가 이 Role을 AssumeRole 할 수 있도록 허용합니다.
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Effect = "Allow"
        Principal = {
          Service = "ec2.amazonaws.com"
        }
        Action = "sts:AssumeRole"
      }
    ]
  })

  # AWS 리소스에 붙이는 태그입니다.
  tags = {
    Name = "team10-ec2-ssm-role" # AWS 콘솔에서 보이는 IAM Role 이름입니다.
    Team = var.team              # 팀 공통 태그입니다.
  }
}

# EC2 IAM Role에 SSM 접속에 필요한 관리형 정책을 연결합니다.
# AmazonSSMManagedInstanceCore 정책은 EC2가 Systems Manager에 등록되고,
# Session Manager를 통해 접속될 수 있도록 필요한 최소 권한을 제공합니다.
resource "aws_iam_role_policy_attachment" "ec2_ssm_core" {
  # 위에서 생성한 EC2 IAM Role 이름입니다.
  role = aws_iam_role.ec2_ssm_role.name

  # AWS에서 제공하는 SSM 관리형 정책 ARN입니다.
  # 예제 코드의 AmazonEC2RoleforSSM보다 현재는 이 정책 사용이 권장됩니다.
  policy_arn = "arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"
}

# IAM Instance Profile을 생성합니다.
# EC2는 IAM Role을 직접 붙이는 것이 아니라 Instance Profile을 통해 Role을 연결합니다.
# 이후 aws_instance 리소스에서 iam_instance_profile 값으로 이 Profile을 지정합니다.
resource "aws_iam_instance_profile" "ec2_ssm_profile" {
  # Instance Profile 이름입니다.
  name = "team10-ec2-ssm-profile"

  # Instance Profile에 연결할 IAM Role입니다.
  role = aws_iam_role.ec2_ssm_role.name

  # AWS 리소스에 붙이는 태그입니다.
  tags = {
    Name = "team10-ec2-ssm-profile" # AWS 콘솔에서 보이는 Instance Profile 이름입니다.
    Team = var.team                 # 팀 공통 태그입니다.
  }
}
