<%@ include file="/WEB-INF/jsp/includes.jsp" %>
<%@ include file="/WEB-INF/jsp/header.jsp" %>


  <h1>
  Spring-DM OSGi Web Console
  </h1>

  <h2>Introduction</h2>
	This application demonstrates the use of Spring-MVC annotations 
	inside OSGi through Spring Dynamic Modules.
	The web application acts as an OSGi console which allows you to
	interact with the OSGi environment. 
 
  <h2>OSGi web console</h2>
  
  <p>Select one of the installed bundles (listed below ) to find out information about 
	 it (such as the relevant OSGi headers, used services, imported and exported packages).
	 Additionally, resources can be searched in one of the bundle <i>spaces</i>. 
  </p>
  
  <h3>Select Bundle</h3> 
	  <form:form modelAttribute="selection">
       <table>
	      <tr>
		      <td>Available bundles:</td>
		      <td>
		      <form:select path="bundleId" items="${bundles}"/>
		      </td>
		      </tr>
			  <tr>
		      <td>Display bundles as:</td>
		      <td><form:select path="displayChoice" items="${displayOptions}"/></td>
		  	  </tr>
		  	  <tr>
	          <td colspan="2">
    			  <p class="submit"><input align="center" type="submit" value="Select Bundle"/></p>
              </td>
          </tr>
  	  </table>
  	  </form:form>
  	  
      <h4>Tip</h4>
            
      The Content Servlet can serve <i>any</i> resource from the OSGi space through Spring-DM
	  Spring-DM <tt>ResourceLoader</tt>. If no prefix is specified, resources are resolved from
	  the servlet own bundle. However, by using <tt>classpath:</tt>, <tt>osgibundlejar:</tt> you can get access to the other <i>spaces</i>
	  defined by OSGi (for more information, see OsgiBundleResourceLoader <a href="http://static.springframework.org/osgi/docs/current/api/org/springframework/osgi/io/OsgiBundleResourceLoader.html">javadoc</a>).
  <br/>
 
  <h3>Bundle Synopsis</h3>
  
  <h4>Bundle Info</h4>
  Headers
  <table id="properties">
    <tr><th>Name</th><th>Value</th></tr>
	<c:forEach var="prop" items="${bundleInfo.properties}">
	  <tr>
	    <td><c:out value="${prop.key}"/></td>
	    <td><c:out value="${prop.value}"/></td>
	  </tr>
	</c:forEach>
	<tr><td>State</td><td>${bundleInfo.state}</td></tr>
	<tr><td>LastModified</td><td><fmt:formatDate value="${bundleInfo.lastModified}" pattern="HH:mm:ss z 'on' yyyy.MM.dd"/></td></tr>
	<tr><td>Location</td><td>${bundleInfo.location}</td></tr>
  </table>
  
  <p/>
  
  <h4>Bundle Wiring</h4>
  <table>
  	<tr>
  		<td>Exported Packages</td>
  		<td>
  		  <c:forEach var="package" items="${bundleInfo.exportedPackages}">
  		  	<c:out value="${package}"/><br/>
  		  </c:forEach>
  		</td>
  	</tr>
  	<tr>
  		<td>Imported Packages<br/> 
  		(incl. the bound versions) </td>
  		<td>
  		  <c:forEach var="package" items="${bundleInfo.importedPackages}">
  		  	<c:out value="${package}"/><br/>
  		  </c:forEach>
  		</td>
  	</tr>
  </table>
  
  <h4>Services</h4>
  
  <h5>Services Registered</h5>
  <table>
  	<tr>
  	   <th>Properties</th><th>Using Bundles</th>
  	</tr>
    <c:forEach var="service" items="${bundleInfo.registeredServices}">
  	<tr>
  	  <td>
 	  <c:forEach var="prop" items="${service.properties}">
	    <c:out value="${prop.key}"/><br/>
	    <c:out value="${prop.value}"/><br/>
  	  </c:forEach>
      </td>
      <td>
  	  <c:forEach var="bundle" items="${service.usingBundles}">
  	  	<c:out value="${bundle}"/><br/>
  	  </c:forEach>
  	  </td>
  	</tr>
  	</c:forEach>
  </table>
  
  <h5>Services In Use</h5>
  <table>
   	<tr>
  	   <th>Properties</th><th>Owning Bundle</th>
  	</tr>
    <c:forEach var="service" items="${bundleInfo.servicesInUse}">
  	<tr>
  	  <td>
 	  <c:forEach var="prop" items="${service.properties}">
	    <c:out value="${prop.key}"/><br/>
	    <c:out value="${prop.value}"/><br/>
  	  </c:forEach>
      </td>
      <td>
  	    <c:out value="${service.bundle}"/>
  	  </td>
  	</tr>
  	</c:forEach>
  </table>
  
  <h3>Bundle Search</h3>
 
  <p/>
  While the servlets/pages are simplistic, they show the main functionality working inside an OSGi platform.
  
  <h2>Sources</h2><a name="sources"> </a>
  To view the Servlet and JSP sources directly from the browser, use one of the links below. The content itself
  is served through a Servlet (the Resource Servlet below) that sends to the browser the content of the files 
  found in its bundle classpath.
  
  <ul>
	<li><a href="./resourceServlet?resource=/WEB-INF/classes/org/springframework/osgi/samples/simplewebapp/servlet/HelloOsgiWorldServlet.java">Hello World Servlet</a></li>
	<li><a href="./resourceServlet?resource=/WEB-INF/classes/org/springframework/osgi/samples/simplewebapp/servlet/ResourceServingServlet.java">Resource Serving Servlet</a></li>
	<li><a href="./resourceServlet?resource=/hello-osgi-world.jsp">bare-bone JSP</a></li>
	<li><a href="./resourceServlet?resource=/jsp-tag-osgi-world.jsp">tag-based JSP</a></li>
	<li><a href="./resourceServlet?resource=/WEB-INF/web.xml">Web application <tt>web.xml</tt></a></li>
	<li><a href="./resourceServlet?resource=">WAR OSGi bundle content</a></li>
  </ul>
  
  <h2>Requirements</h2> 
  
  This sample requires:
  <ul>
    <li>an OSGi 4.0+ platform</li>
    <li>Spring-DM 1.1 + dependencies</li>
    <li>Apache Tomcat 5.5.x+</li>
    <li>Apache Jasper 2 Engine </li>
  </ul>
  
  Note that all the dependencies are automatically downloaded when running the sample.
  
<%@ include file="/WEB-INF/jsp/footer.jsp" %>