================================
== Simple WebApplication Demo ==
================================

1. MOTIVATION

As the name implies, this is a simple web application that runs
inside OSGi through Spring-DM.

The demo contains 2 maven projects:

* war
which contains the actual demo war. The bundle is just an archive that
contains one static resource (index.html), some servlets and some JSP. 
Additionally, the project contains a manifest suitable for web development.

* integration-test
which uses Spring-DM testing framework to execute an integration test. The test
will check the existence of the web application by checking the availability
of the html page, the servlet and JSP.

2. BUILD AND DEPLOYMENT

This directory contains the source files.
For building, Maven 2 and JDK 1.4 are required.

To manually connect to the server, modify the integration test commented method
to wait for user input from the console, run the test and connect your browser
at http://localhost:8080/simple-web-app/

Additionally, you can install the project bundles into the target OSGi platform
and start it.