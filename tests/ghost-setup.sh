#!/bin/bash

set -euxo pipefail

USAGE="Usage: $0 <ghost-dir> <ghost-version> <auth-type: ghost|password>"
if [[ "$#" -lt 3 ]]; then
    echo "$USAGE"
    exit 1
fi

GHOST_DIR="$1"
VERSION="$2"
AUTH_TYPE="$3"
if ! [[ "$AUTH_TYPE" = "ghost" ]] && ! [[ "$AUTH_TYPE" = "password" ]]; then
    echo "$USAGE"
    exit 1
fi

CONFIG_DIR=$GHOST_DIR/core/server/config

# install Ghost
# the old "install from npm" flow doesn't work with Ghost 1.x currently, it may work later
#npm install ghost@$VERSION
#yarn add --no-lockfile ghost@$VERSION
git clone https://github.com/tryghost/ghost.git $GHOST_DIR
pushd $GHOST_DIR
    git checkout $VERSION
    git submodule update --init --recursive

    # install Ghost deps
    yarn global add knex-migrator ember-cli grunt-cli
    yarn install

    # build client files
    grunt build

    # initialize database
    knex-migrator init
popd

# copy config files
cp tests/ghost-defaults.json $CONFIG_DIR/defaults.json
if [[ "$AUTH_TYPE" = "password" ]]; then
    # not needed for password auth, it's the default
    #cp tests/ghost-$AUTH_TYPE-auth-config.json $CONFIG_DIR/env/config.development.json
    : # null command to prevent syntax error because of empty if followed by else
else
    echo "Ghost Auth support is not implemented in this script!"
    exit 2
fi
