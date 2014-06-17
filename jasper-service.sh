#!/bin/sh

DIR=$( cd "$( dirname "$0" )" && pwd )
cd "$DIR"

JAR="bin/jasper-service.jar"

JAVA=java
if test -n "$JAVA_HOME"; then
    JAVA="$JAVA_HOME/bin/java"
fi

# check log4j file presence
L4J_CFG="conf/log4j.properties"
LOG_OPTS=
if [ -f "$L4J_CFG" ]; then
    LOG_OPTS="-Dlog4j.configuration=file:$L4J_CFG"
fi

if test -n "$JAVA_HOME"; then
    JAVA="$JAVA_HOME/bin/java"
fi

exec "$JAVA" "$LOG_OPTS" -jar "$JAR" "$@"
#echo "$CMD"
#exec "$CMD"