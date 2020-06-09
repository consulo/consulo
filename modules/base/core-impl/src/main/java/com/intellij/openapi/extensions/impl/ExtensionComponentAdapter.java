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
package com.intellij.openapi.extensions.impl;

import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.xmlb.XmlSerializer;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.injecting.InjectingProblemException;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.function.Function;

/**
 * @author Alexander Kireyev
 */
public class ExtensionComponentAdapter<T> implements LoadingOrder.Orderable {
  public static final ExtensionComponentAdapter[] EMPTY_ARRAY = new ExtensionComponentAdapter[0];

  protected T myComponentInstance;
  protected Class<T> myImplementationClass;

  private final String myImplementationClassName;
  private final Element myExtensionElement;
  private final PluginDescriptor myPluginDescriptor;
  private final boolean myDeserializeInstance;

  public ExtensionComponentAdapter(@Nonnull String implementationClass, Element extensionElement, PluginDescriptor pluginDescriptor, boolean deserializeInstance) {
    myImplementationClassName = implementationClass;
    myExtensionElement = extensionElement;
    myPluginDescriptor = pluginDescriptor;
    myDeserializeInstance = deserializeInstance;
  }

  public Class getComponentImplementation() {
    return loadImplementationClass();
  }

  @SuppressWarnings("unchecked")
  public T getComponentInstance(Function<Class<T>, T> getUnbindedInstanceFunc) {
    if (myComponentInstance == null) {
      try {
        if (Element.class.equals(getComponentImplementation())) {
          myComponentInstance = (T)myExtensionElement;
        }
        else {
          T componentInstance = getUnbindedInstanceFunc.apply(loadImplementationClass());

          if (myDeserializeInstance) {
            try {
              XmlSerializer.deserializeInto(componentInstance, myExtensionElement);
            }
            catch (Exception e) {
              throw new InjectingProblemException(e);
            }
          }

          myComponentInstance = componentInstance;
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable t) {
        PluginId pluginId = myPluginDescriptor != null ? myPluginDescriptor.getPluginId() : null;
        throw new PluginExtensionInitializationException(t.getMessage(), t, pluginId);
      }

      if(myComponentInstance instanceof consulo.extensions.PluginAware) {
        consulo.extensions.PluginAware pluginAware = (consulo.extensions.PluginAware)myComponentInstance;
        pluginAware.setPluginDescriptor(myPluginDescriptor);
      }
    }

    return myComponentInstance;
  }

  public T getExtension(Function<Class<T>, T> getUnbindedInstanceFunc) {
    return getComponentInstance(getUnbindedInstanceFunc);
  }

  @Override
  public LoadingOrder getOrder() {
    return LoadingOrder.readOrder(myExtensionElement.getAttributeValue("order"));
  }

  @Override
  public String getOrderId() {
    return myExtensionElement.getAttributeValue("id");
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  @SuppressWarnings("unchecked")
  private Class<T> loadImplementationClass() {
    if (myImplementationClass == null) {
      try {
        ClassLoader classLoader = myPluginDescriptor != null ? myPluginDescriptor.getPluginClassLoader() : getClass().getClassLoader();
        if (classLoader == null) {
          classLoader = getClass().getClassLoader();
        }
        myImplementationClass = (Class<T>)Class.forName(myImplementationClassName, false, classLoader);
      }
      catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      }
    }
    return myImplementationClass;
  }

  @Override
  public String toString() {
    return "ExtensionComponentAdapter[" + myImplementationClassName + "]: plugin=" + myPluginDescriptor;
  }
}
