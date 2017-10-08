#!/bin/bash
# fail if anything errors
set -e
# fail if a function call is missing an argument
set -u

function waitNexusStarted() {
    local baseUrl=$1
    until [ $(curl -s -o /dev/null -w "%{http_code}" "$baseUrl") -eq "200" ]; do
        echo "Waiting for service started..."
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

    if [ $(curl -s -u "$username:$password" -o /dev/null -w "%{http_code}" "$baseUrl/service/siesta/rest/v1/script/$name") -eq 200 ]; then
        echo "update and run script: $name"
        curl -s -X PUT -u "$username:$password" --header "Content-Type: application/json" "$baseUrl/service/siesta/rest/v1/script/$name" -d @"$file"
        curl -s -X POST -u "$username:$password" --header "Content-Type: text/plain" "$baseUrl/service/siesta/rest/v1/script/$name/run"
    else
        echo "create and run script: $name"
        curl -s -X POST -u "$username:$password" --header "Content-Type: application/json" "$baseUrl/service/siesta/rest/v1/script" -d @"$file"
        curl -s -X POST -u "$username:$password" --header "Content-Type: text/plain" "$baseUrl/service/siesta/rest/v1/script/$name/run"
    fi
}

function init() {
    echo "Start Nexus initialization..."
    local baseUrl=$1
    local username=$2
    local password=$3

    #wait
    waitNexusStarted $baseUrl

    for f in /docker-entrypoint-init.d/*; do
        case "$f" in
            *.json) echo "$0: running $f"; createOrUpdateThenRunScript "$f" "$baseUrl" "$username" "$password" ;;
            *)      echo "$0: ignoring $f" ;;
        esac
    done

    echo
    echo 'Nexus init process done. Ready for start up.'
    echo
}

function run() {
    if [ "$ORCHESTRATION_ENABLED" != true ]; then
        echo "Start Nexus for initialization"
        exec bin/nexus run &
        pid="$!"

        # run actual init
        init "$baseUrl" "$username" "$password"

        #stop nexus
        if ! kill -s TERM "$pid" || ! wait "$pid"; then
            echo >&2 'Nexus init process failed.'
            exit 1
        fi
    fi

    echo "Start $(pwd)/bin/nexus $@"
    exec bin/nexus "$@"
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