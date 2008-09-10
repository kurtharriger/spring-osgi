!!!! Please read README file at the root of s2-repo-tomcat module !!!!

This example is based on the Contract First Spring-WS example explained here: http://static.springframework.org/spring-ws/sites/1.5/reference/html/tutorial.html
Please, refer to the aforementioned tutorial for more information on Spring-WS. 

To build this example navigate to teh root directory of this module (spring-ws-osgi) and execute the following command:

	> mvn clean install

This will generate the WAR bundle in the target directory

To install bundle follow these steps:

	1. Start Target Platform
	2. Once in the OSGi console type the following:
	
			osgi> install file:<WAR file location>
			
		Example:
			osgi> install file:/Users/olegzhurakousky/Dev/spring-osgi-dev/spring-osgi/samples/s2-repo-tomcat6/spring-ws-osgi/target/spring-ws-osgi.war
	
	    This will render the output showing you the bundle ID:
	
	     	Bundle id is 63
	
    3. Start bundle by executing the following command:

		 	osgi> start <bundle id>
		 	
		Example:
			osgi> start 63

	4. You are done. You should see the output similar to this:
		
		22:56:48,924 DEBUG ContextConfig:362 - Parsing application web.xml file at jndi:/localhost/spring-ws-osgi/WEB-INF/web.xml
		22:56:48,938 DEBUG ContextConfig:1080 - Pipeline Configuration:
		22:56:48,938 DEBUG ContextConfig:1087 -   org.apache.catalina.core.StandardContextValve/1.0
		22:56:48,938 DEBUG ContextConfig:1090 - ======================
		22:56:49,005  INFO TomcatWarDeployer:94 - Successfully deployed bundle [Spring WS OSGi sample (spring_ws_osgi)] at [/spring-ws-osgi] on server org.apache.catalina.core.StandardService/1.0

	5. Access the following link to see you WSDL: http://localhost:8080/spring-ws-osgi/ws/holidayService/holiday.wsdl

	6. Now you can use your favorite SOAP client to send a message to this service. You can also use the test provided in spring-ws-osgi-test module.
	   Once the message is received you should see the following output in the OSGi console:
		
		23:09:06,723  INFO StubHumanResourceService:36 - Booking holiday for [Mon Jul 03 00:00:00 EDT 2006-Fri Jul 07 00:00:00 EDT 2006] for [John Doe] 
:-)


		