#!/bin/sh

ROOT_DIR="$(dirname "$0")"

BUILD_DIR=`ls -1v platform | tail -n 1`

CONSULO_HOME="$ROOT_DIR/platform/$BUILD_DIR"

export CONSULO_HOME
export ROOT_DIR

"$CONSULO_HOME/bin/launcher.sh"