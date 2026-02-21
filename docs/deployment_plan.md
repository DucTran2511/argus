# Argus - Deployment & Production Plan

> **Author**: DevOps Engineer & Cloud Architect  
> **Version**: 2.0  
> **Date**: 2026-01-19  
> **Target**: AWS Lightsail + Docker Compose

---

## Executive Summary

This plan deploys Argus on a single **AWS Lightsail VPS** running all services via Docker Compose. Simple, cost-effective, and easy to manage.

| Component | Solution | Cost |
|-----------|----------|------|
| **Compute** | Lightsail 4GB | $20/month |
| **Database** | Self-hosted PostgreSQL | Included |
| **Redis** | Self-hosted Redis | Included |
| **Frontend** | Vercel | Free |
| **Total** | | **~$20/month** |

---

## Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                    AWS LIGHTSAIL ($20/month)                           │
│                      4GB RAM, 2 vCPU, 80GB SSD                         │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   ┌──────────────────── Docker Compose ────────────────────────────┐  │
│   │                                                                 │  │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐ │  │
│   │  │   Argus     │  │   Redis     │  │      PostgreSQL         │ │  │
│   │  │  (Spring)   │  │  (Cache +   │  │    (Primary DB)         │ │  │
│   │  │   ~1.2GB    │  │   Streams)  │  │       ~300MB            │ │  │
│   │  │  Port 8080  │  │   ~200MB    │  │     Port 5432           │ │  │
│   │  └──────┬──────┘  └──────┬──────┘  └───────────┬─────────────┘ │  │
│   │         │                │                     │               │  │
│   │         └────────────────┼─────────────────────┘               │  │
│   │                    Internal Network                             │  │
│   └─────────────────────────────────────────────────────────────────┘  │
│                               │                                        │
│                         Port 8080 ──────────────────┐                  │
│                               │                     │                  │
└───────────────────────────────┼─────────────────────┼──────────────────┘
                                │                     │
                    ┌───────────▼──────┐    ┌────────▼────────┐
                    │     Vercel       │    │   External APIs │
                    │    Frontend      │    │ Alchemy/CoinGecko│
                    └──────────────────┘    └─────────────────┘
```

---

## Lightsail Plan Selection

| Plan | RAM | vCPU | SSD | Price | Recommendation |
|------|-----|------|-----|-------|----------------|
| $10/month | 2GB | 1 | 60GB | $10 | ⚠️ Tight for JVM |
| **$20/month** | **4GB** | **2** | **80GB** | **$20** | ✅ **Recommended** |
| $40/month | 8GB | 2 | 160GB | $40 | For scaling |

> [!TIP]
> Start with **$20 plan**. Upgrade takes minutes via snapshot.

---

## Quick Start Deployment

### Step 1: Create Lightsail Instance

```bash
# AWS Console → Lightsail → Create Instance
# - Region: us-east-1 (or closest to you)
# - Platform: Linux/Unix
# - Blueprint: Ubuntu 24.04 LTS
# - Plan: $20/month (4GB RAM)
# - Name: argus-prod
```

### Step 2: Configure Firewall

Open ports in Lightsail Networking tab:

| Port | Protocol | Source | Purpose |
|------|----------|--------|---------|
| 22 | TCP | Your IP only | SSH |
| 80 | TCP | Anywhere | HTTP redirect |
| 443 | TCP | Anywhere | HTTPS |
| 8080 | TCP | Anywhere | API (temp, remove after HTTPS setup) |

### Step 3: SSH and Install Docker

```bash
# SSH into instance
ssh -i ~/.ssh/lightsail.pem ubuntu@YOUR_IP

# Install Docker
curl -fsSL https://get.docker.com | sh
sudo usermod -aG docker ubuntu
newgrp docker

# Install Docker Compose
sudo apt install docker-compose-plugin -y

# Verify
docker --version
docker compose version
```

### Step 4: Deploy Application

```bash
# Create app directory
mkdir -p ~/argus && cd ~/argus

# Create docker-compose.prod.yml (see below)
nano docker-compose.prod.yml

# Create .env.prod file (see below)
nano .env.prod

# Pull and start
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# View logs
docker compose -f docker-compose.prod.yml logs -f
```

---

## Docker Compose Configuration

### docker-compose.prod.yml

```yaml
version: '3.8'

services:
  # ============================================
  # PostgreSQL Database
  # ============================================
  postgres:
    image: postgres:16-alpine
    container_name: argus-postgres
    restart: always
    environment:
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./backups:/backups
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER} -d ${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - argus-network
    # Only accessible within Docker network (secure)
    expose:
      - "5432"

  # ============================================
  # Redis (Cache + Streams)
  # ============================================
  redis:
    image: redis:7-alpine
    container_name: argus-redis
    restart: always
    command: redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
    volumes:
      - redis_data:/data
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - argus-network
    expose:
      - "6379"

  # ============================================
  # Argus Backend
  # ============================================
  argus:
    # Pull from GitHub Container Registry (after CI/CD setup)
    image: ghcr.io/YOUR_GITHUB_USERNAME/argus:latest
    # Or build locally: docker build -t argus:latest .
    container_name: argus-api
    # Memory limit for shared 4GB VPS
    deploy:
      resources:
        limits:
          memory: 2G
    restart: always
    ports:
      - "8080:8080"
    environment:
      SPRING_PROFILES_ACTIVE: prod
      # Database
      POSTGRES_HOST: postgres
      POSTGRES_PORT: 5432
      POSTGRES_DB: ${POSTGRES_DB}
      POSTGRES_USER: ${POSTGRES_USER}
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
      # Redis
      REDIS_HOST: redis
      REDIS_PORT: 6379
      REDIS_DATABASE: 1
      # Blockchain
      ETH_RPC_URL: ${ETH_RPC_URL}
      # External services
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      TELEGRAM_BOT_TOKEN: ${TELEGRAM_BOT_TOKEN}
      TELEGRAM_ENABLED: ${TELEGRAM_ENABLED}
      # CORS
      FRONTEND_URL: ${FRONTEND_URL}
    depends_on:
      postgres:
        condition: service_healthy
      redis:
        condition: service_healthy
    healthcheck:
      test: ["CMD", "wget", "-q", "--spider", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - argus-network

volumes:
  postgres_data:
    driver: local
  redis_data:
    driver: local

networks:
  argus-network:
    driver: bridge
```

### .env.prod (Template)

```bash
# ============================================
# Database
# ============================================
POSTGRES_DB=argus
POSTGRES_USER=argus_user
POSTGRES_PASSWORD=CHANGE_ME_STRONG_PASSWORD_HERE

# ============================================
# Blockchain
# ============================================
ETH_RPC_URL=https://eth-mainnet.g.alchemy.com/v2/YOUR_ALCHEMY_KEY

# ============================================
# External Services
# ============================================
OPENAI_API_KEY=sk-xxxxx
TELEGRAM_BOT_TOKEN=123456:ABC-xxxxx
TELEGRAM_ENABLED=true

# ============================================
# Frontend
# ============================================
FRONTEND_URL=https://argus.vercel.app
```

---

## Dockerfile

```dockerfile
# Multi-stage build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B
COPY src ./src/
RUN ./mvnw package -DskipTests -B

# Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user
RUN addgroup -g 1001 argus && adduser -D -u 1001 -G argus argus
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R argus:argus /app
USER argus

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

EXPOSE 8080

# JVM Memory for shared 4GB VPS
# Total: 4GB → OS(500MB) + DB(400MB) + Redis(256MB) = ~1.2GB used
# Safe for JVM: 1.5-2GB heap
ENV JAVA_OPTS="-Xms512m -Xmx1536m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --spring.profiles.active=prod"]
```

---

## CI/CD with GitHub Actions

The workflow builds and pushes to **GitHub Container Registry (ghcr.io)** on every push to `main`.

### .github/workflows/build.yml

```yaml
name: Build & Push Docker Image

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: '21'
          distribution: 'temurin'
          cache: maven
      - run: ./mvnw verify -B

  build:
    needs: test
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4
      
      - uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}
      
      - uses: docker/metadata-action@v5
        id: meta
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=sha,prefix=
            type=raw,value=latest,enable={{is_default_branch}}
      
      - uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
```

### Deployment on Lightsail

After CI/CD pushes to ghcr.io, pull and deploy on your VPS:

```bash
# One-time: Login to GitHub Container Registry
echo $GITHUB_TOKEN | docker login ghcr.io -u YOUR_USERNAME --password-stdin

# Pull latest and restart
cd ~/argus
docker compose -f docker-compose.prod.yml pull argus
docker compose -f docker-compose.prod.yml up -d argus
```

> [!TIP]
> For automatic deployments, you can add a webhook or SSH step to GitHub Actions.

---

## HTTPS with Caddy (Recommended)

Add Caddy as reverse proxy for automatic HTTPS:

```yaml
# Add to docker-compose.prod.yml
  caddy:
    image: caddy:2-alpine
    container_name: argus-caddy
    restart: always
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./Caddyfile:/etc/caddy/Caddyfile
      - caddy_data:/data
      - caddy_config:/config
    networks:
      - argus-network

volumes:
  caddy_data:
  caddy_config:
```

**Caddyfile:**
```
api.argus.yourdomain.com {
    reverse_proxy argus:8080
}
```

Then remove port 8080 from `argus` service (Caddy handles external traffic).

---

## Backup Strategy

### Automated PostgreSQL Backups

Create `/home/ubuntu/argus/backup.sh`:

```bash
#!/bin/bash
BACKUP_DIR="/home/ubuntu/argus/backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/argus_$TIMESTAMP.sql.gz"

# Create backup
docker exec argus-postgres pg_dump -U argus_user argus | gzip > $BACKUP_FILE

# Keep only last 7 days
find $BACKUP_DIR -name "*.sql.gz" -mtime +7 -delete

# Optional: Upload to S3
# aws s3 cp $BACKUP_FILE s3://your-bucket/argus-backups/

echo "Backup completed: $BACKUP_FILE"
```

Add to crontab (`crontab -e`):
```bash
# Daily backup at 3 AM
0 3 * * * /home/ubuntu/argus/backup.sh >> /home/ubuntu/argus/backup.log 2>&1
```

---

## Monitoring

### Basic Health Check Script

```bash
#!/bin/bash
# /home/ubuntu/argus/healthcheck.sh

API_URL="http://localhost:8080/actuator/health"
RESPONSE=$(curl -s -o /dev/null -w "%{http_code}" $API_URL)

if [ "$RESPONSE" != "200" ]; then
    echo "$(date): API unhealthy, restarting..."
    cd /home/ubuntu/argus
    docker compose -f docker-compose.prod.yml restart argus
    # Optional: Send Telegram alert
fi
```

Add to crontab:
```bash
*/5 * * * * /home/ubuntu/argus/healthcheck.sh >> /home/ubuntu/argus/health.log 2>&1
```

### Useful Commands

```bash
# View all logs
docker compose -f docker-compose.prod.yml logs -f

# View specific service
docker compose -f docker-compose.prod.yml logs -f argus

# Check resource usage
docker stats

# Restart all services
docker compose -f docker-compose.prod.yml restart

# Update and redeploy
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

---

## Deployment Workflow

### Initial Deployment

```bash
# 1. Clone repo to Lightsail
git clone https://github.com/YOUR_USERNAME/argus.git
cd argus

# 2. Build Docker image
docker build -t argus:latest .

# 3. Create production compose file
cp docker-compose.yml docker-compose.prod.yml
# Edit docker-compose.prod.yml with production settings

# 4. Create .env.prod with secrets
nano .env.prod

# 5. Start services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# 6. Run Flyway migrations (automatic on startup)
# Check logs to confirm
docker compose -f docker-compose.prod.yml logs argus | grep -i flyway
```

### Updating Application

```bash
# 1. Pull latest code
cd ~/argus
git pull origin main

# 2. Rebuild image
docker build -t argus:latest .

# 3. Restart with new image
docker compose -f docker-compose.prod.yml up -d argus

# 4. Check logs
docker compose -f docker-compose.prod.yml logs -f argus
```

---

## Cost Comparison

| Approach | Monthly Cost | Pros | Cons |
|----------|--------------|------|------|
| **Lightsail All-in-One** | **$20** | Simple, fast, cheap | You manage backups |
| Lightsail + Supabase | $45 | Managed DB backups | Higher cost |
| ECS + RDS + ElastiCache | $100+ | Enterprise-grade | Complex, expensive |

---

## Alternative: Hybrid with Supabase

If you prefer managed database with automatic backups:

```yaml
# docker-compose.prod.yml (Hybrid)
services:
  redis:
    # Keep self-hosted Redis
    ...

  argus:
    environment:
      # Use Supabase instead of local PostgreSQL
      POSTGRES_HOST: db.xxxxx.supabase.co
      POSTGRES_PORT: 5432
      POSTGRES_DB: postgres
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: ${SUPABASE_PASSWORD}
      # Add SSL requirement
      SPRING_DATASOURCE_HIKARI_DATA_SOURCE_PROPERTIES_SSLMODE: require
```

**Cost**: $20 (Lightsail) + $25 (Supabase Pro) = **$45/month**

---

## Security Checklist

- [ ] Change default passwords in `.env.prod`
- [ ] Restrict SSH to your IP only (Lightsail firewall)
- [ ] Set up HTTPS with Caddy
- [ ] Remove port 8080 after Caddy setup (use 443 only)
- [ ] Enable Lightsail automatic snapshots ($2/month)
- [ ] Store `.env.prod` securely (not in git)

---

## Quick Reference

```bash
# Start all services
docker compose -f docker-compose.prod.yml --env-file .env.prod up -d

# Stop all services
docker compose -f docker-compose.prod.yml down

# View logs
docker compose -f docker-compose.prod.yml logs -f

# Restart specific service
docker compose -f docker-compose.prod.yml restart argus

# Manual backup
docker exec argus-postgres pg_dump -U argus_user argus > backup.sql

# Restore backup
cat backup.sql | docker exec -i argus-postgres psql -U argus_user -d argus

# Check disk space
df -h

# Check memory
free -h
```
