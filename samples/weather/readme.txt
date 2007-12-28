==================
== Weather Demo ==
==================

1. MOTIVATION

This demo shows more advanced features of Spring-DM and OSGi.
The application creates a very simple weather information services showing
some best practices in designing an application to take advantage of the
modularity offered by OSGi.


The demo contains several maven projects:

* weather-dao
DAO bundle for the weather service. Reads information from an in-memory storage.

* weather-extension
Provides the sample with its own namespace. The extension contains a 'virtual-bundle'
service that allows creating and installing of bundles created on the fly from various
resources. The feature is also wrapped with its own namespace which becomes available
after installing this bundle.

* weather-service
Provides the actual service implementation

* weather-service-test
Simple consumer (acting as a test) for the weather service. Once installed, the bundle
will query the Weather Service which was retrieved and binded using Spring-DM.

* wiring-bundle
Project that does the OSGi assembly and service publishing/consumption as well as bundle
installation through weather-extension virtual bundle feature.


* weather-service-integration-test
Integration test based on Spring-DM testing framework. Installs all the bundles above and
checks whether the weather service has been properly installed or not by interacting with
the service.


2. BUILD AND DEPLOYMENT

This directory contains the source files.
For build, Maven 2 and JDK 1.4 are required.