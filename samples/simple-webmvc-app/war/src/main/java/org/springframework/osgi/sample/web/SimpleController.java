/*
 * Copyright 2006 the original author or authors.
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
package org.springframework.osgi.sample.web;

import javax.servlet.http.HttpServletRequest;

import org.springframework.osgi.sample.app.StringReverserOSGiFacade;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
/**
 * Standard Spring-MVC annotated controller
 * 
 * @author Oleg Zhurakousky
 */
@Controller
public class SimpleController {
	private StringReverserOSGiFacade stringReverserOSGiFacade;
	/**
	 * 
	 * @param stringReverserOSGiFacade
	 */
	public SimpleController(StringReverserOSGiFacade stringReverserOSGiFacade){
		this.stringReverserOSGiFacade = stringReverserOSGiFacade;
	}
	/**
	 * 
	 * @param request
	 * @return
	 */
	@RequestMapping("/osgi.htm")
	public ModelAndView process(HttpServletRequest request){
		System.out.println("Hello OSGI MVC");
		ModelAndView mv = new ModelAndView();
		String input = request.getParameter("string"); 
		String reversedString = "Please enter the String to reverse";
		if (input == null){
			mv.addObject("reversedString", "Oleg");
		} else {
			reversedString = stringReverserOSGiFacade.reverseString(input);
			mv.addObject("reversedString", reversedString);
		}
		mv.setViewName("osgi");
		return mv;
	}
}
