This is module is not a Bundle, nor it is a Fragment Bundle
It is a simple Maven configuration which will set up Equinox Target Platform based on dependencies specified in the Parent module.
It will copy all the dependencies of the Parent module into it's own root, which is going to become the root of Equinox Target Platform
based on "config.ini" file (Equinox configuration file) http://www.eclipse.org/equinox/.

config.ini - is located at "./configuration" directory and already includes all the required bundles and fragments including:
- log4j-config - Log4J Configuration (see corresponding module)
- catalina-config - Catalina Configuration (see corresponding module)
- server-config - Server startup Configuration (see corresponding module)

To build this module separately, navigate to the module's root directory (i.e., osgi-target-platform) and execute the following:

	> mvn -P dependencies clean install

This will copy all the dependencies to its root

To start Target platform, navigate to the rot of this module and execute the following command:

	> java -jar org.eclipse.osgi-3.4.0.v20080605-1900.jar 

With default configurations provided, your output should look similar to this:

. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
20:04:04,344  INFO Catalina:538 - Initialization processed in 305 ms
20:04:04,344  INFO StandardService:508 - Starting service Catalina
20:04:04,345  INFO StandardEngine:432 - Starting Servlet Engine: Apache Tomcat/6.0.18
20:04:04,367 DEBUG HostConfig:1136 - HostConfig: Processing START
20:04:04,383  INFO Http11Protocol:209 - Starting Coyote HTTP/1.1 on http-8181
20:04:04,389  INFO Activator:102 - Succesfully started Apache Tomcat/6.0.18 @ Catalina:8181
20:04:04,399  INFO TomcatWarDeployer:91 - Found service Catalina
20:04:04,399  INFO Activator:110 - Published Apache Tomcat/6.0.18 as an OSGi service
============================

Once Equinox is started you can execute the following command to get more information on available options:

	> help

You can also get a quick short status of your environment by executing:

	> ss

which should render the output that looks like this:
. . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
56	ACTIVE      com.springsource.org.apache.commons.codec_1.3.0
57	ACTIVE      org.springframework.web_2.5.5.A
58	ACTIVE      com.springsource.javax.xml.stream_1.0.1
59	ACTIVE      com.springsource.javax.servlet.jsp.jstl_1.1.2
60	RESOLVED    log4j.config_1.0.0
	            Master=50
61	RESOLVED    catalina.config_1.0.0
	            Master=39
62	RESOLVED    server.config_1.0.0
	            Master=34

Outside of Fragment Bundles, all other bundles should be in the ACTIVE state.

To stop Target PLatform and exit from the console execute the following:

	> exit

:-)

