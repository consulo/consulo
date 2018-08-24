/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.openapi.components.impl;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.util.PairProcessor;
import consulo.annotations.DeprecationInfo;

import javax.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Just for exporting")
public class ServiceManagerImpl {
  private static final Logger LOGGER = Logger.getInstance(ServiceManagerImpl.class);

  public static void processAllImplementationClasses(@Nonnull ComponentManagerImpl componentManager, @Nonnull PairProcessor<Class<?>, PluginDescriptor> processor) {
    //Collection adapters = componentManager.getPicoContainer().getComponentAdapters();
    //if (adapters.isEmpty()) {
    //  return;
    //}
    //
    //for (Object o : adapters) {
    //  Class aClass;
    //  if (o instanceof ServiceComponentAdapter) {
    //    ServiceComponentAdapter adapter = (ServiceComponentAdapter)o;
    //    PluginDescriptor pluginDescriptor = adapter.getPluginDescriptor();
    //    try {
    //      ComponentAdapter delegate = adapter.getDelegateWithoutInitialize();
    //      // avoid delegation creation & class initialization
    //      if (delegate == null) {
    //        ClassLoader classLoader = pluginDescriptor == null ? ServiceManagerImpl.class.getClassLoader() : pluginDescriptor.getPluginClassLoader();
    //        aClass = Class.forName(adapter.getDescriptor().getImplementation(), false, classLoader);
    //      }
    //      else {
    //        aClass = delegate.getComponentImplementation();
    //      }
    //    }
    //    catch (Throwable e) {
    //      LOGGER.error(e);
    //      continue;
    //    }
    //
    //    if (!processor.process(aClass, pluginDescriptor)) {
    //      break;
    //    }
    //  }
    //  else if (o instanceof ComponentAdapter && !(o instanceof ExtensionComponentAdapter)) {
    //    try {
    //      aClass = ((ComponentAdapter)o).getComponentImplementation();
    //    }
    //    catch (Throwable e) {
    //      LOGGER.error(e);
    //      continue;
    //    }
    //
    //    ComponentConfig config = componentManager.getConfig(aClass);
    //    if (config != null) {
    //      processor.process(aClass, config.pluginDescriptor);
    //    }
    //  }
    //}
  }
}
