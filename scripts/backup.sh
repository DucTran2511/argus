#!/bin/bash
# ============================================
# Argus PostgreSQL Backup Script
# ============================================
# Usage: ./backup.sh
# Cron:  0 3 * * * /home/ubuntu/argus/scripts/backup.sh >> /home/ubuntu/argus/backup.log 2>&1

set -e

# Configuration
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/argus/backups}"
CONTAINER_NAME="argus-postgres"
DB_NAME="${POSTGRES_DB:-argus}"
DB_USER="${POSTGRES_USER:-argus_user}"
RETENTION_DAYS=7
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
BACKUP_FILE="$BACKUP_DIR/argus_$TIMESTAMP.sql.gz"

# Create backup directory if not exists
mkdir -p "$BACKUP_DIR"

echo "[$(date)] Starting backup..."

# Create backup
docker exec "$CONTAINER_NAME" pg_dump -U "$DB_USER" "$DB_NAME" | gzip > "$BACKUP_FILE"

# Verify backup
if [ -s "$BACKUP_FILE" ]; then
    SIZE=$(du -h "$BACKUP_FILE" | cut -f1)
    echo "[$(date)] Backup successful: $BACKUP_FILE ($SIZE)"
else
    echo "[$(date)] ERROR: Backup file is empty!"
    rm -f "$BACKUP_FILE"
    exit 1
fi

# Delete old backups
echo "[$(date)] Removing backups older than $RETENTION_DAYS days..."
find "$BACKUP_DIR" -name "argus_*.sql.gz" -mtime +$RETENTION_DAYS -delete

# List current backups
echo "[$(date)] Current backups:"
ls -lh "$BACKUP_DIR"/argus_*.sql.gz 2>/dev/null || echo "No backups found"

# Optional: Upload to S3 (uncomment if using AWS S3)
# aws s3 cp "$BACKUP_FILE" "s3://your-bucket/argus-backups/"

echo "[$(date)] Backup completed successfully"
