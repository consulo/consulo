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
package com.intellij.openapi.extensions;

import consulo.container.plugin.PluginDescriptor;
import consulo.extensions.PluginAware;
import consulo.injecting.InjectingContainer;
import consulo.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author peter
 */
public abstract class AbstractExtensionPointBean implements PluginAware {
  private static final Logger LOG = Logger.getInstance(AbstractExtensionPointBean.class);
  protected PluginDescriptor myPluginDescriptor;

  @Override
  public final void setPluginDescriptor(PluginDescriptor pluginDescriptor) {
    myPluginDescriptor = pluginDescriptor;
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public final <T> Class<T> findClass(@Nonnull String className) throws ClassNotFoundException {
    return (Class<T>)Class.forName(className, true, getLoaderForClass());
  }

  @Nullable
  public final <T> Class<T> findClassNoExceptions(final String className) {
    try {
      return findClass(className);
    }
    catch (ClassNotFoundException e) {
      LOG.error("Problem loading class " + className + " from plugin " + myPluginDescriptor.getPluginId().getIdString(), e);
      return null;
    }
  }

  @Nonnull
  public ClassLoader getLoaderForClass() {
    return myPluginDescriptor == null ? getClass().getClassLoader() : myPluginDescriptor.getPluginClassLoader();
  }

  @Nonnull
  public final <T> T instantiate(final String className, @Nonnull final InjectingContainer container) throws ClassNotFoundException {
    return instantiate(this.<T>findClass(className), container);
  }

  @Nonnull
  public static <T> T instantiate(@Nonnull final Class<T> aClass, @Nonnull final InjectingContainer container) {
    return container.getUnbindedInstance(aClass);
  }
}
