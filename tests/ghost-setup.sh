#!/bin/bash

set -euxo pipefail

USAGE="Usage: $0 <ghost-version> <auth-type: ghost|password>"
if [[ "$#" -lt 2 ]]; then
    echo "$USAGE"
    exit 1
fi

VERSION=$1
AUTH_TYPE=$2
if ! [[ "$AUTH_TYPE" = "ghost" ]] && ! [[ "$AUTH_TYPE" = "password" ]]; then
    echo "$USAGE"
    exit 1
fi

GHOST_DIR=./node_modules/ghost
CONFIG_DIR=$GHOST_DIR/core/server/config

# install Ghost
#npm install ghost@$VERSION
yarn add --no-lockfile ghost@$VERSION

# copy config files
cp tests/ghost-defaults.json $CONFIG_DIR/defaults.json
cp tests/ghost-$AUTH_TYPE-auth-config.json $CONFIG_DIR/env/config.development.json

# init database
pushd $GHOST_DIR
    ../knex-migrator/bin/knex-migrator init
popd
