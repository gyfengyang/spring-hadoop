/*
 * Copyright 2011-2012 the original author or authors.
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
package org.springframework.data.hadoop.mapreduce;

import java.util.List;

import org.apache.hadoop.util.Tool;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.CollectionUtils;

/**
 * Factory bean for executing Hadoop jars. Tries a best-effort in passing the configuration to the jar and preventing malicious behaviour (such as System.exit()).
 * See the reference documentation for more details.
 * Using the {@link Tool} interface is highly recommended in all cases.
 * 
 * <p/>Note by default, the runner is configured to execute at startup. One can customize this behaviour through {@link #setRunAtStartup(boolean)}.
 * <p/>This class is a factory bean - if {@link #setRunAtStartup(boolean)} is set to false, then the action (namely the execution of the Tool) is postponed until
 * {@link #getObject()} is called.
 * 
 * @author Costin Leau
 */
public class JarRunner extends JarExecutor implements FactoryBean<Integer>, InitializingBean, BeanFactoryAware {

	private volatile Integer result = null;
	private boolean runAtStartup = false;

	private List<String> preActions;
	private List<String> postActions;
	private BeanFactory beanFactory;


	@Override
	public Integer getObject() throws Exception {
		if (result == null) {
			invoke(preActions);
			result = runCode();
			invoke(postActions);
		}
		return result;
	}

	@Override
	public Class<?> getObjectType() {
		return int.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		super.afterPropertiesSet();

		if (runAtStartup) {
			getObject();
		}
	}

	/**
	 * Indicates whether the jar should run at container startup or not (the default).
	 *
	 * @param runAtStartup The runAtStartup to set.
	 */
	public void setRunAtStartup(boolean runAtStartup) {
		this.runAtStartup = runAtStartup;
	}

	/**
	 * Beans to be invoked before running the action.
	 * 
	 * @param beans
	 */
	public void setPreAction(String... beans) {
		this.preActions = CollectionUtils.arrayToList(beans);
	}

	/**
	 * Beans to be invoked after running the action.
	 * 
	 * @param beans
	 */
	public void setPostAction(String... beans) {
		this.postActions = CollectionUtils.arrayToList(beans);
	}

	private void invoke(List<String> beans) {
		if (beanFactory != null) {
			if (!CollectionUtils.isEmpty(beans)) {
				for (String bean : beans) {
					beanFactory.getBean(bean);
				}
			}
		}
		else {
			log.warn("No beanFactory set - cannot invoke pre/post actions [" + beans + "]");
		}
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
}