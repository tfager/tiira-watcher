#!/bin/bash

VERSION=$(head -1 project.clj | cut -d " " -f 3 | sed -e 's/"//g')

docker run \
    --rm \
    --name tiira-watcher \
    -e FIRESTORE_CREDENTIALS_FILE=/config/firestore.json \
    --mount type=bind,source=$HOME/tmp/tiira-config,target=/config,readonly \
    -p 8080:8080 \
    tiira-watcher:$VERSION