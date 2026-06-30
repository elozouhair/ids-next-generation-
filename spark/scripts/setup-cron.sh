#!/bin/bash
# Setup cron job for auto-retraining (run every 30 minutes)
set -e

SCRIPT_DIR="/opt/spark/app/scripts"
CRON_FILE="/tmp/retrain-cron"

echo "*/30 * * * * spark /opt/spark/app/scripts/retrain.sh" > "$CRON_FILE"

# Install crontab
crontab -u spark "$CRON_FILE" 2>/dev/null

# Start cron daemon if not running
if ! pgrep -x crond > /dev/null; then
    crond -b 2>/dev/null || true
fi

echo "Cron job installed (retrain every 30 min)"
