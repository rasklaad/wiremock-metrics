#! /bin/sh
# ossrh profile should be defined in settings.xml and should contain gpg.executable and gpg.keyname properties
mvn clean deploy -Prelease -Possrh
