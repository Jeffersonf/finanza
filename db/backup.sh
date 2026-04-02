#!/bin/sh
# ============================================
# Finanza — Backup Automático Diário
# Roda via crond às 03:00 todos os dias
# ============================================

BACKUP_DIR="/backups"
DB_HOST="db"
DB_NAME="finanza"
DB_USER="finanza_user"
KEEP_DAYS=30  # Mantém 30 dias de backup

mkdir -p "$BACKUP_DIR"

do_backup() {
    FILENAME="finanza_$(date +%Y%m%d_%H%M%S).sql.gz"
    FILEPATH="$BACKUP_DIR/$FILENAME"

    echo "[$(date)] Iniciando backup → $FILENAME"

    pg_dump -h "$DB_HOST" -U "$DB_USER" "$DB_NAME" | gzip > "$FILEPATH"

    if [ $? -eq 0 ]; then
        SIZE=$(stat -c%s "$FILEPATH" 2>/dev/null || stat -f%z "$FILEPATH")
        echo "[$(date)] Backup concluído: $FILENAME ($SIZE bytes)"

        # Registra no banco
        psql -h "$DB_HOST" -U "$DB_USER" "$DB_NAME" \
            -c "INSERT INTO backup_log (filename, size_bytes) VALUES ('$FILENAME', $SIZE);" \
            > /dev/null 2>&1

        # Remove backups antigos
        find "$BACKUP_DIR" -name "finanza_*.sql.gz" -mtime +$KEEP_DAYS -delete
        echo "[$(date)] Backups com mais de $KEEP_DAYS dias removidos"
    else
        echo "[$(date)] ERRO: Falha no backup!"
        exit 1
    fi
}

# Agenda: todo dia às 03:00
echo "0 3 * * * /backup.sh >> /backups/backup.log 2>&1" > /etc/crontabs/root

# Roda imediatamente ao iniciar (primeiro backup)
do_backup
