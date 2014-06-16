#!/bin/sh

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
echo "script dir: $DIR"
JAR="$DIR/bin/jasper-service.jar"

JAVA=java
if test -n "$JAVA_HOME"; then
    JAVA="$JAVA_HOME/bin/java"
fi

# check log4j file presence
L4J_CFG="$DIR/conf/log4j.properties"
LOG_OPTS=
if [ -f "$L4J_CFG" ]; then
    LOG_OPTS="-Dlog4j.configuration=file:$L4J_CFG"
fi

if test -n "$JAVA_HOME"; then
    JAVA="$JAVA_HOME/bin/java"
fi

CMD="$JAVA $LOG_OPTS -jar $JAR $@"
echo "$CMD"
exec "$CMD"