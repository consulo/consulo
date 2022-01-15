/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.openapi.components;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.util.xmlb.annotations.Attribute;
import consulo.container.plugin.PluginDescriptor;
import consulo.extensions.PluginAware;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author mike
 */
public class ServiceBean implements PluginAware {
  private static final Logger LOG = Logger.getInstance(ServiceBean.class);

  @Attribute("serviceInterface")
  public String serviceInterface;

  private PluginDescriptor myPluginDescriptor;

  @Nonnull
  @SuppressWarnings("unchecked")
  public static <T> List<T> loadServicesFromBeans(final ExtensionPointName<ServiceBean> epName, Class<T> componentClass) {
    final List<ServiceBean> exportableBeans = epName.getExtensionList();
    final List<T> components = new ArrayList<>(exportableBeans.size());
    for (ServiceBean exportableBean : exportableBeans) {
      final String serviceClass = exportableBean.serviceInterface;
      if (serviceClass == null) {
        LOG.error("Service interface not specified in " + epName);
        continue;
      }
      try {
        final Class<?> aClass = Class.forName(serviceClass, true, exportableBean.getPluginDescriptor().getPluginClassLoader());
        final Object service = ServiceManager.getService(aClass);
        if (service == null) {
          LOG.error("Can't find service: " + serviceClass);
          continue;
        }
        if (!componentClass.isInstance(service)) {
          LOG.error("Service " + serviceClass + " is registered in " + epName.getName() + " EP, but doesn't implement " + componentClass.getName());
          continue;
        }

        components.add((T)service);
      }
      catch (ClassNotFoundException e) {
        LOG.error(e);
      }
    }
    return components;
  }

  @Override
  public void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }
}
