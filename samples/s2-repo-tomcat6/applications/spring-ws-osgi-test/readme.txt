!!!! Please read README file at the root of s2-repo-tomcat module !!!!
!!!! Also read README file in spring-ws-osgi module !!!!

This file contains the test case to test a web service using WebServiceTemplate.
To run the test make sure:

1. Target Platform is started
2. spring-ws-osgi module is deployed and started (see README)

Start a new console window, navigate to the root of spring-ws-osgi-test and execute the following command:

	> mvn clean test
	
Your test should run successfully and Target Platform's consle should show the following output:

   01:17:36,173  INFO StubHumanResourceService:35 - Booking holiday for [Mon Jul 03 00:00:00 EDT 2006-Fri Jul 07 00:00:00 EDT 2006] for [John Doe] via Spring-DM deployed Endpoint
	
:-)