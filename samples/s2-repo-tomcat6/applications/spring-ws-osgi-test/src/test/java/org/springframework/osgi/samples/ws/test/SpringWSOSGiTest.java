/*
 * Copyright 2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.osgi.samples.ws.test;

import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import junit.framework.TestCase;

import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.client.core.WebServiceTemplate;
/**
 * This test will use WebServicesTemplate to execute WS request.
 * The request is read from the provided XML file. 
 * WebServiceTemplate will wrap it into a SOAP message. Results will be printed to the console.
 *
 * @author Oleg Zhurakousky
 */
public class SpringWSOSGiTest extends TestCase {
	  
	public void testService() throws Exception{
		WebServiceTemplate template = new WebServiceTemplate();
		String uri = "http://localhost:8080/spring-ws-osgi/ws/holidayService/";
		InputStream is = new ClassPathResource("holidayRequest.xml").getInputStream();
		Source source = new StreamSource(is);  
		Result result = new StreamResult(System.out);
		template.sendSourceAndReceiveToResult(uri, source, result);
	}

}
