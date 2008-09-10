This is a Fragment Bundle which provides Log4J configuration for the entire target platform
It is attached to "com.springsource.org.apache.log4j" bundle which is a part of the target platform (see MANIFEST file)

For more Log4J info refere to: http://logging.apache.org/log4j/1.2/index.html
Currently it only contains sample configuration (log4j.properties) which you can modify any way you want to

Since these examples follow Maven convention the file is located in the "src/main/resources" directory

To build this module separately, navigate to the module's root directory (i.e., log4j-config) and execute the following:

> mvn clean install

This will generate JAR in the "target" directory of the module

The generated JAR is already included in Equinox's "config.ini" file located in "osgi-target-platform/configuration" directory.