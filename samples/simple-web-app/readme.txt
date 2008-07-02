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

To start the sample, run the following commands:

a) Download the needed libraries. This step will download Equinox OSGi platform
as well as the sample dependencies. This step is required only once

# mvn -P dependencies,equinox package

The equinox platform and the project dependencies should be available under the libs/ folder.

b) start Equinox platform using the downloaded libraries.
The sample already contains a proper Equinox config under configuration/ folder.
To start the sample and interact with the Equinox platform run:

# java -jar libs/org.eclipse.osgi.jar

Note that you can interact with the osgi platform. Type h or help for more information

c) Connect to the web app by pointing your browser at
http://localhost:8080/simple-web-app/
