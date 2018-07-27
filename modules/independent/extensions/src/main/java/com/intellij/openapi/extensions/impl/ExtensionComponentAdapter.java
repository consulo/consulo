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

import com.google.inject.Injector;
import com.intellij.openapi.extensions.LoadingOrder;
import com.intellij.openapi.extensions.PluginAware;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.util.xmlb.XmlSerializer;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * @author Alexander Kireyev
 * @author VISTALL
 */
public class ExtensionComponentAdapter implements LoadingOrder.Orderable, Function<Injector, Object> {
  @Deprecated
  public static ThreadLocal<Boolean> ourUnstableMarker = ThreadLocal.withInitial(() -> Boolean.FALSE);

  public static final ExtensionComponentAdapter[] EMPTY_ARRAY = new ExtensionComponentAdapter[0];

  private final String myImplementationClassName;
  private final Element myExtensionElement;
  private final PluginDescriptor myPluginDescriptor;
  private final boolean myDeserializeInstance;
  private Class myImplementationClass;

  public ExtensionComponentAdapter(@Nonnull String implementationClass, Element extensionElement, PluginDescriptor pluginDescriptor, boolean deserializeInstance) {
    myImplementationClassName = implementationClass;
    myExtensionElement = extensionElement;
    myPluginDescriptor = pluginDescriptor;
    myDeserializeInstance = deserializeInstance;
  }

  @Override
  public Object apply(Injector injector) {
    return apply(injector, Injector::getInstance);
  }

  @Nonnull
  @SuppressWarnings("unchecked")
  public <T> T apply(@Nullable Injector injector, @Nonnull BiFunction<Injector, Class<T>, T> function) {
    try {
      ourUnstableMarker.set(Boolean.TRUE);

      T componentinstance;
      try {
        if (Element.class.equals(getComponentImplementation())) {
          componentinstance = (T)myExtensionElement;
        }
        else {
          T componentInstance = (T)function.apply(injector, myImplementationClass);

          if (myDeserializeInstance) {
            try {
              XmlSerializer.deserializeInto(componentInstance, myExtensionElement);
            }
            catch (Exception e) {
              throw new ExceptionInInitializerError(e);
            }
          }

          componentinstance = componentInstance;
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable t) {
        PluginId pluginId = myPluginDescriptor != null ? myPluginDescriptor.getPluginId() : null;
        throw new PicoPluginExtensionInitializationException(t.getMessage(), t, pluginId);
      }

      if (componentinstance instanceof PluginAware) {
        PluginAware pluginAware = (PluginAware)componentinstance;
        pluginAware.setPluginDescriptor(myPluginDescriptor);
      }
      return componentinstance;
    }
    finally {
      ourUnstableMarker.set(Boolean.FALSE);
    }
  }

  @Nonnull
  public Class getComponentImplementation() {
    return loadImplementationClass();
  }

  @Override
  public LoadingOrder getOrder() {
    return LoadingOrder.readOrder(myExtensionElement.getAttributeValue("order"));
  }

  @Override
  public String getOrderId() {
    return myExtensionElement.getAttributeValue("id");
  }

  private Element getExtensionElement() {
    return myExtensionElement;
  }

  @Override
  public Element getDescribingElement() {
    return getExtensionElement();
  }

  public PluginId getPluginName() {
    return myPluginDescriptor.getPluginId();
  }

  public PluginDescriptor getPluginDescriptor() {
    return myPluginDescriptor;
  }

  private Class loadImplementationClass() {
    if (myImplementationClass == null) {
      try {
        ClassLoader classLoader = myPluginDescriptor != null ? myPluginDescriptor.getPluginClassLoader() : getClass().getClassLoader();
        if (classLoader == null) {
          classLoader = getClass().getClassLoader();
        }
        myImplementationClass = Class.forName(myImplementationClassName, false, classLoader);
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
