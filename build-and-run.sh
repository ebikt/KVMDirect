#!/bin/bash
set -ex

export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
VERSION="$(grep '^ *version *=' gradle.properties | cut -d= -f2- | awk '{print $1}' | head -n1)"

case "$1" in
    ''|run)
      shift || :
      ./gradlew assembleDist

      rm -rf dist
      mkdir dist
      tar -C dist -xvf build/distributions/KVMDirect-$VERSION.tar KVMDirect-$VERSION/lib/

      #"$JAVA_HOME"/bin/java -cp "$( echo dist/KVMDirect/lib/*.jar | tr ' ' : )" ebik.kvm.KVMDirect "$@"
      #export JAVA_HOME=/usr/lib/jvm/java-19-openjdk-i386/
      export JAVA_HOME=/usr/lib/jvm/java-22-openjdk-amd64/
      "$JAVA_HOME"/bin/java -cp "$( echo dist/KVMDirect-$VERSION/lib/*.jar | tr ' ' : )" ebik.kvm.KVMDirect "$@"
      ;;
    ''|shadowRun)
      #FIXME copy services
      shift || :
      ./gradlew fatJar

      export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64/
      #"$JAVA_HOME"/bin/java -jar build/libs/KVMDirectFull.jar "$@"
      #FIXME get version
      java -jar build/libs/KVMDirectFull-$VERSION.jar "$@"
      ;;
    *)
      ./gradlew "$@"
      ;;
esac
