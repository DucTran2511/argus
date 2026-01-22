#!/bin/bash

CONTAINER_NAME="job_postgres"
DB_USER="jobuser"
DB_NAME="argus"
DEFAULT_LIMIT=50

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m' # No Color

run_query() {
    docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -c "$1"
}

run_query_raw() {
    docker exec -i $CONTAINER_NAME psql -U $DB_USER -d $DB_NAME -t -c "$1" | tr -d '\r'
}

get_tables() {
    run_query_raw "SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;"
}

get_row_count() {
    run_query_raw "SELECT COUNT(*) FROM $1;" | xargs
}

show_usage() {
    echo -e "${CYAN}Usage: ./check_db.sh <command> [options]${NC}"
    echo ""
    echo -e "${GREEN}Commands:${NC}"
    echo "  all [limit]        Show all tables (default: $DEFAULT_LIMIT rows)"
    echo "  <table> [limit]    Show specific table"
    echo "  count              Show row counts for all tables"
    echo "  summary            Quick summary of database"
    echo "  describe <table>   Show table schema/structure"
    echo "  relations          Show all foreign keys & relationships"
    echo "  indexes            Show all indexes"
    echo "  tables             List all tables"
    echo ""
    echo -e "${GREEN}Limit options:${NC}"
    echo "  <number>           Limit to N rows (default: $DEFAULT_LIMIT)"
    echo "  0 or unlimited     Show ALL rows"
    echo ""
    echo -e "${GREEN}Examples:${NC}"
    echo "  ./check_db.sh all              # $DEFAULT_LIMIT rows per table"
    echo "  ./check_db.sh all 0            # All rows (no limit)"
    echo "  ./check_db.sh wallets 100      # 100 rows from wallets"
    echo "  ./check_db.sh count            # Row counts only"
    echo "  ./check_db.sh describe signals # Show signals table schema"
    echo "  ./check_db.sh relations        # Show table relationships"
    echo ""
}

if [ -z "$1" ]; then
    show_usage
    echo -e "${YELLOW}Available tables:${NC}"
    run_query "\dt"
    exit 1
fi

MODE=$1

# === COUNT MODE ===
if [ "$MODE" == "count" ]; then
    echo -e "${CYAN}========================================"
    echo -e " DATABASE ROW COUNTS"
    echo -e "========================================${NC}"
    echo ""
    printf "%-25s %10s\n" "TABLE" "ROWS"
    echo "-------------------------------------"
    TOTAL=0
    for TABLE in $(get_tables); do
        TABLE=$(echo $TABLE | xargs)
        if [ -n "$TABLE" ]; then
            COUNT=$(get_row_count $TABLE)
            if [ "$COUNT" == "0" ]; then
                printf "%-25s ${RED}%10s${NC}\n" "$TABLE" "$COUNT"
            else
                printf "%-25s ${GREEN}%10s${NC}\n" "$TABLE" "$COUNT"
            fi
            TOTAL=$((TOTAL + COUNT))
        fi
    done
    echo "-------------------------------------"
    printf "%-25s ${CYAN}%10s${NC}\n" "TOTAL" "$TOTAL"
    exit 0
fi

# === SUMMARY MODE ===
if [ "$MODE" == "summary" ]; then
    echo -e "${CYAN}========================================"
    echo -e " DATABASE SUMMARY"
    echo -e "========================================${NC}"
    echo ""
    TABLE_COUNT=$(get_tables | wc -l | xargs)
    echo -e "Tables: ${GREEN}$TABLE_COUNT${NC}"
    echo ""
    echo -e "${YELLOW}Row counts:${NC}"
    for TABLE in $(get_tables); do
        TABLE=$(echo $TABLE | xargs)
        if [ -n "$TABLE" ]; then
            COUNT=$(get_row_count $TABLE)
            if [ "$COUNT" != "0" ]; then
                echo -e "  $TABLE: ${GREEN}$COUNT${NC}"
            fi
        fi
    done
    exit 0
fi

# === TABLES LIST MODE ===
if [ "$MODE" == "tables" ]; then
    run_query "\dt"
    exit 0
fi

# === RELATIONS MODE ===
if [ "$MODE" == "relations" ] || [ "$MODE" == "rels" ] || [ "$MODE" == "fk" ]; then
    echo -e "${CYAN}========================================"
    echo -e " TABLE RELATIONSHIPS (Foreign Keys)"
    echo -e "========================================${NC}"
    echo ""
    
    # Query foreign keys from PostgreSQL catalog
    FK_QUERY="
    SELECT 
        tc.table_name AS from_table,
        kcu.column_name AS from_column,
        ccu.table_name AS to_table,
        ccu.column_name AS to_column,
        tc.constraint_name
    FROM information_schema.table_constraints AS tc
    JOIN information_schema.key_column_usage AS kcu
        ON tc.constraint_name = kcu.constraint_name
    JOIN information_schema.constraint_column_usage AS ccu
        ON ccu.constraint_name = tc.constraint_name
    WHERE tc.constraint_type = 'FOREIGN KEY'
    ORDER BY tc.table_name;"
    
    FK_COUNT=$(run_query_raw "SELECT COUNT(*) FROM information_schema.table_constraints WHERE constraint_type = 'FOREIGN KEY';" | xargs)
    
    if [ "$FK_COUNT" == "0" ]; then
        echo -e "${YELLOW}No foreign key constraints found in database.${NC}"
        echo ""
        echo -e "${CYAN}Logical relationships (from schema):${NC}"
        echo "  wallets.id        → transactions.wallet_id"
        echo "  wallets.address   → asset_transfers.wallet_address"
        echo "  wallets.id        → signals.wallet_id"
        echo "  users.id          → alert_rules.user_id"
    else
        run_query "$FK_QUERY"
    fi
    
    echo ""
    echo -e "${CYAN}========================================"
    echo -e " UNIQUE CONSTRAINTS"
    echo -e "========================================${NC}"
    run_query "
    SELECT 
        tc.table_name,
        tc.constraint_name,
        string_agg(kcu.column_name, ', ') AS columns
    FROM information_schema.table_constraints tc
    JOIN information_schema.key_column_usage kcu 
        ON tc.constraint_name = kcu.constraint_name
    WHERE tc.constraint_type IN ('UNIQUE', 'PRIMARY KEY')
        AND tc.table_schema = 'public'
    GROUP BY tc.table_name, tc.constraint_name
    ORDER BY tc.table_name;"
    exit 0
fi

# === INDEXES MODE ===
if [ "$MODE" == "indexes" ] || [ "$MODE" == "idx" ]; then
    echo -e "${CYAN}========================================"
    echo -e " DATABASE INDEXES"
    echo -e "========================================${NC}"
    echo ""
    run_query "
    SELECT 
        tablename AS table_name,
        indexname AS index_name,
        indexdef AS definition
    FROM pg_indexes
    WHERE schemaname = 'public'
    ORDER BY tablename, indexname;"
    exit 0
fi


# === DESCRIBE MODE ===
if [ "$MODE" == "describe" ] || [ "$MODE" == "desc" ]; then
    if [ -z "$2" ]; then
        echo -e "${RED}Error: Please specify a table name${NC}"
        echo "Usage: ./check_db.sh describe <table_name>"
        exit 1
    fi
    TABLE_NAME=$2
    echo -e "${CYAN}========================================"
    echo -e " SCHEMA: $TABLE_NAME"
    echo -e "========================================${NC}"
    run_query "\d $TABLE_NAME"
    exit 0
fi

# === ALL TABLES MODE ===
LIMIT=${2:-$DEFAULT_LIMIT}

if [ "$LIMIT" == "0" ] || [ "$LIMIT" == "unlimited" ]; then
    LIMIT_CLAUSE=""
    LIMIT_MSG="(all rows)"
else
    LIMIT_CLAUSE="LIMIT $LIMIT"
    LIMIT_MSG="(limit: $LIMIT)"
fi

if [ "$MODE" == "all" ]; then
    for TABLE in $(get_tables); do
        TABLE=$(echo $TABLE | xargs)
        if [ -n "$TABLE" ]; then
            COUNT=$(get_row_count $TABLE)
            echo -e "${CYAN}========================================${NC}"
            if [ "$COUNT" == "0" ]; then
                echo -e " TABLE: ${YELLOW}$TABLE${NC} - ${RED}No rows${NC}"
            else
                echo -e " TABLE: ${YELLOW}$TABLE${NC} - ${GREEN}$COUNT rows${NC} $LIMIT_MSG"
            fi
            echo -e "${CYAN}========================================${NC}"
            if [ "$COUNT" != "0" ]; then
                run_query "SELECT * FROM $TABLE $LIMIT_CLAUSE;"
            fi
            echo ""
        fi
    done
else
    # === SINGLE TABLE MODE ===
    TABLE_NAME=$1
    COUNT=$(get_row_count $TABLE_NAME 2>/dev/null)
    if [ $? -ne 0 ]; then
        echo -e "${RED}Error: Table '$TABLE_NAME' not found${NC}"
        echo ""
        echo "Available tables:"
        run_query "\dt"
        exit 1
    fi
    echo -e "${CYAN}Querying table: ${YELLOW}$TABLE_NAME${NC} - ${GREEN}$COUNT rows${NC} $LIMIT_MSG"
    echo "----------------------------------------"
    if [ "$COUNT" == "0" ]; then
        echo -e "${YELLOW}(No rows in this table)${NC}"
    else
        run_query "SELECT * FROM $TABLE_NAME $LIMIT_CLAUSE;"
    fi
fi


