#!/bin/sh

MVN=mvn

$MVN deploy:deploy-file -Dfile=./lib/ID_ISC.SDK.Java-V5.5.2/sub-prj/lib/fedm-java/build/dist/OBIDISC4J.jar -DgroupId=de.feig -DartifactId=OBIDISC4J -Dversion=5.5.2 -Dpackaging=jar -Durl=file:./mvn-repository/ -DrepositoryId=maven-repository -DupdateReleaseInfo=true
