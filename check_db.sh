#!/bin/bash

CONTAINER_NAME="job_postgres"
DB_USER="jobuser"
DB_NAME="argus"

show_usage() {
    echo "Usage: ./check_db.sh <table_name|all> [limit]"
    echo ""
    echo "Available tables:"
    docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "\dt"
}

if [ -z "$1" ]; then
    show_usage
    exit 1
fi

MODE=$1
LIMIT=${2:-20} 

if [ "$MODE" == "all" ]; then

    TABLES=$(docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -t -c "SELECT tablename FROM pg_tables WHERE schemaname = 'public';")
    
    for TABLE in $TABLES; do
        TABLE=$(echo $TABLE | xargs)
        if [ -n "$TABLE" ]; then
            echo "========================================"
            echo " TABLE: $TABLE"
            echo "========================================"
            docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "SELECT * FROM $TABLE LIMIT $LIMIT;"
            echo ""
        fi
    done
else
    TABLE_NAME=$1
    echo "Querying table: $TABLE_NAME (Limit: $LIMIT)"
    echo "----------------------------------------"
    docker exec -it $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "SELECT * FROM $TABLE_NAME LIMIT $LIMIT;"
fi
