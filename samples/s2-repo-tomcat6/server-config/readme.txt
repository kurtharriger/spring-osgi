This is a Fragment Bundle which provides default server configuration for the Tomcat Start bundle
It is attached to "org.springframework.osgi.catalina.start.osgi" bundle which is a part of the target platform (see MANIFEST file)

This fragment contains "conf/server.xml" file, which contains server configurations 
such as ports (see more documentation here: http://tomcat.apache.org/tomcat-6.0-doc/config/index.html)

Since these examples follow Maven convention the "conf" directory is in the "src/main/resources" directory

To build this module separately, navigate to the module's root directory (i.e., server-config) and execute the following:

> mvn clean install

This will generate JAR in the "target" directory of the module

The generated JAR is already included in Equinox's "config.ini" file located in "osgi-target-platform/configuration" directory.