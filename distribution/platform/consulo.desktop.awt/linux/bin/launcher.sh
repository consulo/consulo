#!/bin/sh
#
# ---------------------------------------------------------------------
# Consulo startup script.
# ---------------------------------------------------------------------
#

message()
{
  TITLE="Cannot start Consulo"
  if [ -t 1 ]; then
    echo "ERROR: $TITLE\n$1"
  elif [ -n `which zenity` ]; then
    zenity --error --title="$TITLE" --text="$1"
  elif [ -n `which kdialog` ]; then
    kdialog --error --title "$TITLE" "$1"
  elif [ -n `which xmessage` ]; then
    xmessage -center "ERROR: $TITLE: $1"
  elif [ -n `which notify-send` ]; then
    notify-send "ERROR: $TITLE: $1"
  else
    echo "ERROR: $TITLE\n$1"
  fi
}

UNAME=`which uname`
GREP=`which egrep`
GREP_OPTIONS=""
CUT=`which cut`
READLINK=`which readlink`
MKTEMP=`which mktemp`
RM=`which rm`
CAT=`which cat`
TR=`which tr`

if [ -z "$UNAME" -o -z "$GREP" -o -z "$CUT" -o -z "$MKTEMP" -o -z "$RM" -o -z "$CAT" -o -z "$TR" ]; then
  message "Required tools are missing - check beginning of \"$0\" file for details."
  exit 1
fi

OS_TYPE=`"$UNAME" -s`

IDE_HOME="$CONSULO_HOME"
IDE_BIN_HOME="$CONSULO_HOME/bin"

# ---------------------------------------------------------------------
# Locate a JDK installation directory which will be used to run the IDE.
# Try (in order): CONSULO_JRE, bundled jdk, JDK_HOME, JAVA_HOME, "java" in PATH.
# ---------------------------------------------------------------------
if [ -n "$CONSULO_JRE" -a -x "$CONSULO_JRE/bin/java" ]; then
  JDK="$CONSULO_JRE"
elif [ -x "$IDE_HOME/jre/bin/java" ] && "$IDE_HOME/jre/bin/java" -version > /dev/null 2>&1 ; then
  JDK="$IDE_HOME/jre"
elif [ -n "$JDK_HOME" -a -x "$JDK_HOME/bin/java" ]; then
  JDK="$JDK_HOME"
elif [ -n "$JAVA_HOME" -a -x "$JAVA_HOME/bin/java" ]; then
  JDK="$JAVA_HOME"
else
  JAVA_BIN_PATH=`which java`
  if [ -n "$JAVA_BIN_PATH" ]; then
    if [ "$OS_TYPE" = "FreeBSD" -o "$OS_TYPE" = "MidnightBSD" ]; then
      JAVA_LOCATION=`JAVAVM_DRYRUN=yes java | "$GREP" '^JAVA_HOME' | "$CUT" -c11-`
      if [ -x "$JAVA_LOCATION/bin/java" ]; then
        JDK="$JAVA_LOCATION"
      fi
    elif [ "$OS_TYPE" = "SunOS" ]; then
      JAVA_LOCATION="/usr/jdk/latest"
      if [ -x "$JAVA_LOCATION/bin/java" ]; then
        JDK="$JAVA_LOCATION"
      fi
    elif [ "$OS_TYPE" = "Darwin" ]; then
      JAVA_LOCATION=`/usr/libexec/java_home`
      if [ -x "$JAVA_LOCATION/bin/java" ]; then
        JDK="$JAVA_LOCATION"
      fi
    fi

    if [ -z "$JDK" -a -x "$READLINK" ]; then
      JAVA_LOCATION=`"$READLINK" -f "$JAVA_BIN_PATH"`
      case "$JAVA_LOCATION" in
        */jre/bin/java)
          JAVA_LOCATION=`echo "$JAVA_LOCATION" | xargs dirname | xargs dirname | xargs dirname` ;;
        *)
          JAVA_LOCATION=`echo "$JAVA_LOCATION" | xargs dirname | xargs dirname` ;;
      esac
      if [ -x "$JAVA_LOCATION/bin/java" ]; then
        JDK="$JAVA_LOCATION"
      fi
    fi
  fi
fi

if [ -z "$JDK" ]; then
  message "No JDK found. Please validate either CONSULO_JRE, JDK_HOME or JAVA_HOME environment variable points to valid JDK installation."
  exit 1
fi

VERSION_LOG=`"$MKTEMP" -t java.version.log.XXXXXX`
"$JDK/bin/java" -version 2> "$VERSION_LOG"
"$GREP" "64-Bit|x86_64" "$VERSION_LOG" > /dev/null
BITS=$?
"$RM" -f "$VERSION_LOG"
if [ $BITS -eq 0 ]; then
  BITS="64"
else
  BITS=""
fi

# ---------------------------------------------------------------------
# Collect JVM options and properties.
# ---------------------------------------------------------------------

MAIN_CLASS_NAME="-m consulo.desktop.bootstrap/consulo.desktop.boot.main.Main"

VM_OPTIONS_FILE="$ROOT_DIR/consulo$BITS.vmoptions"

if [ -r "$VM_OPTIONS_FILE" ]; then
  VM_OPTIONS=`"$CAT" "$VM_OPTIONS_FILE" | "$GREP" -v "^#.*" | "$TR" '\n' ' '`
  VM_OPTIONS="$VM_OPTIONS -Djb.vmOptionsFile=\"$VM_OPTIONS_FILE\" -Dconsulo.vm.options.file=\"$VM_OPTIONS_FILE\""
fi

APP_OPTIONS_FILE="$IDE_HOME/bin/app.vmoptions"
APP_OPTIONS=""

if [ -r "$APP_OPTIONS_FILE" ]; then
  APP_OPTIONS=`"$CAT" "$APP_OPTIONS_FILE" | "$GREP" -v "^#.*" | "$TR" '\n' ' '`
fi

COMMON_JVM_ARGS="-p \"$IDE_HOME/boot\" -Dconsulo.module.path.boot=true -Dconsulo.home.path=\"$IDE_HOME\" -Didea.home.path=\"$IDE_HOME\" -Didea.properties.file=\"$ROOT_DIR/consulo.properties\" -Dconsulo.properties.file=\"$ROOT_DIR/consulo.properties\""
IDE_JVM_ARGS=""
ALL_JVM_ARGS="$APP_OPTIONS $VM_OPTIONS $COMMON_JVM_ARGS $IDE_JVM_ARGS $AGENT $REQUIRED_JVM_ARGS"

LD_LIBRARY_PATH="$IDE_BIN_HOME:$LD_LIBRARY_PATH"
export LD_LIBRARY_PATH

export ALL_JVM_ARGS
export MAIN_CLASS_NAME
export JDK
