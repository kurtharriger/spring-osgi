/*
 * Copyright 2008 the original author or authors.
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
package org.springframework.osgi.blueprint.context;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.osgi.service.blueprint.context.ModuleContext;
import org.osgi.service.blueprint.context.ModuleContextAware;

public class ModuleContextAwarePostProcessorTest {

	ModuleContextAwarePostProcessor mcaPostProcessor;
	ModuleContext fakeContext;
	
	@Before
	public void initPostProcessor() {
		this.fakeContext = new ApplicationContext2ModuleContextAdapter(null,null);
		this.mcaPostProcessor = new ModuleContextAwarePostProcessor(this.fakeContext);
	}
	
	@Test
	public void testAfterInitialization() throws Exception {
		Object bean = new Object();
		assertSame(bean,this.mcaPostProcessor.postProcessAfterInitialization(bean, ""));
	}
	
	@Test
	public void testBeforeInitialization() throws Exception {
		Object bean = new Object();
		assertSame(bean,this.mcaPostProcessor.postProcessBeforeInitialization(bean, ""));
		MCAware mcBean = new MCAware();
		assertSame(mcBean,this.mcaPostProcessor.postProcessBeforeInitialization(mcBean, ""));
		assertSame(this.fakeContext,mcBean.getModuleContext());
	}
	
	static class MCAware implements ModuleContextAware {

		ModuleContext mc;
		
		public void setModuleContext(ModuleContext mc) {
			this.mc = mc;
		}
		
		public ModuleContext getModuleContext() {
			return mc;
		}
		
	}
}
