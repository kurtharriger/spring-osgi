SPRING OSGI 1.0-M1 (April 2007)
-------------------------------
http://www.springframework.org/osgi

1. INTRODUCTION

The Spring Framework is the leading full-stack Java/JEE application framework. It provides
a lightweight container and a non-invasive programming model enabled by the use of 
dependency injection, aop, and portable service abstractions. OSGi offers a dynamic 
application execution environment in which components (bundles) can be installed, 
updated, or removed on the fly. It also has excellent support for modularity and 
versioning.

The goal of Spring’s OSGi support is to make it as easy as possible to write Spring
applications that can be deployed in an OSGi execution environment, and that can take
advantage of the services offered by the OSGi framework. Spring’s OSGi support also 
makes development of OSGi applications simpler and more productive by building on the
ease-of-use and power of the Spring Framework. For enterprise applications, we envisage
this will offer the following benefits:

    * Better separation of application logic into modules
    * The ability to deploy multiple versions of a module concurrently
    * The ability to dynamically discover and use services provided by other modules in 
        the system
    * The ability to dynamically deploy, update and undeploy modules in a running system
    * Use of the Spring Framework to instantiate, configure, assemble, and decorate components 
        within and across modules.
    * A simple and familiar programming model for enterprise developers to exploit the
        features of the OSGi platform.

We believe that the combination of OSGi and Spring offers the most comprehensive model
available for building enterprise applications.

It is not a goal of Spring’s OSGi support to provide a universal model for the development
of any OSGi-based application, though some OSGi developers may of course find the Spring model
attractive and choose to adopt it. Existing OSGi bundles and any services they may export are
easily integrated into applications using the Spring OSGi support, as are existing Spring
configurations.

2. RELEASE INFO

The Spring OSGi is targeted at OSGi R4 and above, and JDK level 1.4 and above.

Release contents:
* "src" contains the Java source files for the framework
* "src/samples" contains the samples Java source files
* "dist" contains various Spring OSGi distribution jar files
* "lib" contains all third-party libraries needed for running the samples and/or building the framework
* "docs" contains general documentation and API javadocs

The "lib" directory is just included in the "-with-dependencies" download. Make sure to download this full
distribution ZIP file if you want to run the sample applications and/or build the framework yourself.
Maven 2 pom.xml are provided for building the sources.
 
Latest info is available at the public website: http://www.springframework.org/osgi
Project info at the SourceForge site: http://sourceforge.net/projects/springframework

Spring OSGi is released under the terms of the Apache Software License (see license.txt).
All libraries included in the "-with-dependencies" download are subject to their respective licenses.

4. WHERE TO START

This distribution contains API documentation and two sample applications illustrating the current features of Spring OSGi.
The Spring OSGi specification can be found at http://www.springframework.org/osgi/specification

A great way to get started is to review and run the sample applications, supplementing with reference manual
material as needed. You will require Maven 2, which can be downloaded from http://maven.apache.org/, for building
Spring OSGi.

5. ADDITIONAL RESOURCES

The Spring OSGi homepage is located at:

    http://www.springframework.org/osgi

The Spring Framework portal is located at:

	http://www.springframework.org	