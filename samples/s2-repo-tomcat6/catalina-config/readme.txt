This is a Fragment Bundle which provides default configuration for the Tomcat server
It is attached to "com.springsource.org.apache.catalina" bundle which is a part of the target platform (see MANIFEST file)

Basically any configuration file (with the exception of server.xml) you normally find in Tomcat's "conf" directory should be placed here.
See more info here: http://tomcat.apache.org/tomcat-6.0-doc/index.html
Currently it only contains default "web.xml" (as this is the only thing we need)

Since these examples follow Maven convention the "conf" directory is in the "src/main/resources" directory

To build this module separately, navigate to the module's root directory (i.e., catalina-config) and execute the following:

> mvn clean install

This will generate JAR in the "target" directory of the module

The generated JAR is already included in Equinox's "config.ini" file located in "osgi-target-platform/configuration" directory.