#!/bin/bash

#lein clean
#lein uberjar

VERSION=$(head -1 project.clj | cut -d " " -f 3 | sed -e 's/"//g')
LOCAL_TAG="tiira-watcher:${VERSION}"
PROJECT="tiira-watcher-dev"

mkdir -p docker/target
mv target/tiira-watcher-${{ env.VERSION }}-standalone.jar docker/target/tiira-watcher.jar

#docker build --pull --build-arg VERSION="$VERSION" -t "$LOCAL_TAG" .

if [ "_$1" == "_push" ]
then
    ACCOUNT="tiira-repo-account@tiira-watcher-dev.iam.gserviceaccount.com"
    REMOTE_TAG=europe-north1-docker.pkg.dev/${PROJECT}/tiira-watcher-repo/tiira-watcher:${VERSION}
    gcloud auth print-access-token --impersonate-service-account ${ACCOUNT} | \
      docker login -u oauth2accesstoken --password-stdin https://europe-north1-docker.pkg.dev
    docker tag "$LOCAL_TAG" "$REMOTE_TAG"
    docker push "$REMOTE_TAG"
fi
