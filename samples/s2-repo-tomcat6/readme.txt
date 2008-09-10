Prerequisite:
	Maven must be installed and configured: http://maven.apache.org/
	These samples were tested on JAVA 5/6

This sample module accomplishes two goals
It assembles Eclipse Equinox based OSGi Spring-DM Target Platform based on bundle releases available in 
SpringSource Enterprise Bundle Repository: http://www.springsource.com/repository
It also contains several sample modules demonstrating various aspects of working with, 
as well as developing applications for Spring-DM OSGi Target Platform (all compatible with this Target Platform).

In its current state the following examples are included:
	- osgi-target-platform - 	Set up and configure Eqiunox/Spring-DM/Tomcat-6 Target Platform
	- log4j-config - 			Log4J configuration Fragment Bundle
	- catalina-config - 		Catalina configuration Fragment Bundle
	- server-config - 			Tomcat startup server configuration Fragment Bundle

It also includes the following application bundles:
	- spring-ws-osgi - Spring Web Services example which was converted (OSGi-fied) from the original Spring WS tutorial available 
					   from: http://static.springframework.org/spring-ws/sites/1.5/
					   There you can also find more documentation on developing Spring Web services
	- spring-ws-osgi-test - Test case that uses Web services template to invoke a Web Service

For more detailed information on each module please refer to README file included in each module.
All modules, including the root module, are valid Eclipse projects and could be successfully imported in Eclipse: http://m2eclipse.codehaus.org/

Directory Structure:

	s2-repo-tomcat6
		applications
			spring-ws-osgi
			spring-ws-osgi-test
		catalina-config
		log4j-config
		server-config
		osgi-target-platfrm
	
Build procedure:
	Each module could be build separately and "how" information is available in the README files distributed with each module

	To build the "whole thing" navigate to the root of the parent module (s2-repo-tomcat6) and execute the following command:

	> mvn -P dependencies clean install

	You only need to execute this command once. Besides building all the modules it will set up 
	OSGi Target Platform (see more in README file of osgi-target-platform module)
	Then you can either build modules independently (as you make modifications) or navigate back to the root and execute the following command:

	> mvn  clean install
	
To start Target platform refer to README file of osgi-target-platform. 
Modules such as log4j-config, catalina-config and server-config are already included in config.ini (Equinox configuration file)
and will be started with Target Platform
Information on how to deploy individual application bundles, such as Spring WS sample is available in README file of each application module.

:-)