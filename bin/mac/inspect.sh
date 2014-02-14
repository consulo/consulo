#!/bin/sh
#
# ------------------------------------------------------
# Consulo offline inspection script.
# ------------------------------------------------------
#

export DEFAULT_PROJECT_PATH=`pwd`

IDE_BIN_HOME="$(dirname "$0")"
exec "$IDE_BIN_HOME/../Contents/MacOS/consulo" inspect $*
