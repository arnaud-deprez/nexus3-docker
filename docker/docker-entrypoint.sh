#!/bin/bash
# fail if anything errors
set -e
# fail if a function call is missing an argument
set -u

function waitNexusStarted() {
    local baseUrl=$1
    until [ $(curl -s -o /dev/null -w "%{http_code}" "$baseUrl") -eq "200" ]; do
        echo "$0: Waiting for service started..."
        sleep 10
    done
}

function createOrUpdateThenRunScript() {
    local file=$1
    local baseUrl=$2
    local username=$3
    local password=$4
    local fileName=$(basename "$file")
    local name=${fileName%.*}

    if [ $(curl -s -u "$username:$password" -o /dev/null -w "%{http_code}" "$baseUrl/service/rest/v1/script/$name") -eq 200 ]; then
        echo "$0: update and run script: $name"
        curl -s -X PUT -u "$username:$password" --header "Content-Type: application/json" "$baseUrl/service/rest/v1/script/$name" -d @"$file"
        curl -s -X POST -u "$username:$password" --header "Content-Type: text/plain" "$baseUrl/service/rest/v1/script/$name/run"
        echo
    else
        echo "$0: create and run script: $name"
        curl -s -X POST -u "$username:$password" --header "Content-Type: application/json" "$baseUrl/service/rest/v1/script" -d @"$file"
        curl -s -X POST -u "$username:$password" --header "Content-Type: text/plain" "$baseUrl/service/rest/v1/script/$name/run"
        echo
    fi
}

function init() {
    local baseUrl=$1
    local username=$2
    local password=$3

    #wait
    waitNexusStarted $baseUrl

    echo "$0: Start Nexus initialization..."

    for f in /docker-entrypoint-init.d/*; do
        case "$f" in
            *.json) echo "$0: running $f"; createOrUpdateThenRunScript "$f" "$baseUrl" "$username" "$password" ;;
            *)      echo "$0: ignoring $f" ;;
        esac
    done

    echo "$0: Nexus init process done. Ready for start up."
    echo
}

function run() {
    nexus_script="$SONATYPE_DIR/nexus/bin/nexus"
    if [ "$ORCHESTRATION_ENABLED" != true ]; then
        echo "$0: Start Nexus for initialization: $nexus_script run"
        exec $nexus_script run &
        pid="$!"

        # run actual init
        init "$baseUrl" "$username" "$password"

        #stop nexus
        if ! kill -s TERM "$pid" || ! wait "$pid"; then
            echo >&2 "$0: Nexus init process failed."
            exit 1
        fi
    fi

    echo "$0: Start $nexus_script $@"
    exec $nexus_script "$@"
}

function usage() {
    echo "{init|run|usage}"
}

baseUrl="http://${TARGET_SERVICE_NAME:-localhost}:8081"
username=admin
password=admin123

case "$1" in
    init) init "$baseUrl" "$username" "$password" ;;
    usage) usage ;;
    *) run "$@" ;;
esac