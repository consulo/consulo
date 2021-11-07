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

import com.intellij.openapi.components.ComponentManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.text.StringUtil;
import consulo.container.impl.parser.ExtensionInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginId;
import consulo.logging.Logger;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import org.jdom.Element;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class ExtensionsAreaImpl {
  private static final Logger LOG = Logger.getInstance(ExtensionsAreaImpl.class);

  private static final boolean DEBUG_REGISTRATION = false;

  private final Throwable myCreationTrace;

  private final Map<String, Throwable> myEPTraces = DEBUG_REGISTRATION ? new HashMap<>() : null;

  private final Map<String, ExtensionPointImpl> myExtensionPoints = new HashMap<>();

  private final ComponentManager myComponentManager;

  @Nonnull
  private final Runnable myCheckCanceled;

  private boolean myLocked;

  public ExtensionsAreaImpl(ComponentManager componentManager, @Nonnull Runnable checkCanceled) {
    myCreationTrace = DEBUG_REGISTRATION ? new Throwable("Area creation trace") : null;
    myCheckCanceled = checkCanceled;
    myComponentManager = componentManager;
  }

  public void registerExtensionPoint(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull SimpleXmlElement extensionPointElement) {
    final String pluginId = pluginDescriptor.getPluginId().getIdString();
    final String name = extensionPointElement.getAttributeValue("name");
    if (name == null) {
      LOG.error("'name' attribute not specified for extension point in '" + pluginId + "' plugin");
      return;
    }

    String epName = pluginId + '.' + name;

    String beanClassName = extensionPointElement.getAttributeValue("beanClass");
    String interfaceClassName = extensionPointElement.getAttributeValue("interface");
    if (beanClassName == null && interfaceClassName == null) {
      LOG.error("Neither 'beanClass' nor 'interface' attribute is specified for extension point '" + epName + "' in '" + pluginId + "' plugin");
      return;
    }
    
    if (beanClassName != null && interfaceClassName != null) {
      LOG.error("Both 'beanClass' and 'interface' attributes are specified for extension point '" + epName + "' in '" + pluginId + "' plugin");
      return;
    }

    ExtensionPoint.Kind kind;
    String className;
    if (interfaceClassName != null) {
      className = interfaceClassName;
      kind = ExtensionPoint.Kind.INTERFACE;
    }
    else {
      className = beanClassName;
      kind = ExtensionPoint.Kind.BEAN_CLASS;
    }
    registerExtensionPoint(epName, className, pluginDescriptor, kind);
  }

  public void registerExtension(@Nonnull final PluginDescriptor pluginDescriptor, @Nonnull ExtensionInfo extensionInfo) {
    final PluginId pluginId = pluginDescriptor.getPluginId();

    String epName = extractEPName(extensionInfo);

    SimpleXmlElement element = extensionInfo.getElement();

    ExtensionComponentAdapter adapter;
    final ExtensionPointImpl extensionPoint = getExtensionPointImpl(epName);
    if (extensionPoint.getKind() == ExtensionPoint.Kind.INTERFACE) {
      String implClass = element.getAttributeValue("implementation");
      if (implClass == null) {
        LOG.error("'implementation' attribute not specified for '" + epName + "' extension in '" + pluginId.getIdString() + "' plugin");
        return;
      }
      adapter = new ExtensionComponentAdapter(implClass, mapElement(element), pluginDescriptor, shouldDeserializeInstance(element));
    }
    else {
      adapter = new ExtensionComponentAdapter(extensionPoint.getClassName(), mapElement(element), pluginDescriptor, true);
    }
    extensionPoint.registerExtensionAdapter(adapter);
  }

  private static boolean shouldDeserializeInstance(SimpleXmlElement extensionElement) {
    // has content
    if (!extensionElement.getChildren().isEmpty()) return true;
    // has custom attributes
    for (Map.Entry<String, String> attribute : extensionElement.getAttributes().entrySet()) {
      final String name = attribute.getKey();
      if (!"implementation".equals(name) && !"id".equals(name) && !"order".equals(name)) {
        return true;
      }
    }
    return false;
  }

  @Nonnull
  private static Element mapElement(SimpleXmlElement simpleXmlElement) {
    Element element = new Element(simpleXmlElement.getName());
    for (SimpleXmlElement child : simpleXmlElement.getChildren()) {
      element.addContent(mapElement(child));
    }

    for (Map.Entry<String, String> entry : simpleXmlElement.getAttributes().entrySet()) {
      element.setAttribute(entry.getKey(), entry.getValue());
    }

    String text = simpleXmlElement.getText();
    if(!StringUtil.isEmptyOrSpaces(text)) {
      element.setText(text);
    }

    return element;
  }

  public static String extractEPName(final ExtensionInfo extensionElement) {
    SimpleXmlElement element = extensionElement.getElement();
    String pluginId = extensionElement.getPluginId();
    return pluginId + "." + element.getName();
  }

  public Throwable getCreationTrace() {
    return myCreationTrace;
  }

  public void registerExtensionPoint(@Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull PluginDescriptor descriptor, @Nonnull ExtensionPoint.Kind kind) {
    if (hasExtensionPoint(extensionPointName)) {
      final ExtensionPointImpl oldEP = getExtensionPointImpl(extensionPointName);

      LOG.error("Duplicate registration for EP: " + extensionPointName + ": original plugin " + oldEP.getDescriptor().getPluginId() + ", new plugin " + descriptor.getPluginId(),
                myEPTraces.get(extensionPointName));
      return;
    }

    registerExtensionPoint(new ExtensionPointImpl(extensionPointName, extensionPointBeanClass, kind, myComponentManager, myCheckCanceled, descriptor));
  }

  public void registerExtensionPoint(@Nonnull ExtensionPointImpl extensionPoint) {
    if (myLocked) {
      throw new IllegalArgumentException("locked");
    }

    String name = extensionPoint.getName();
    myExtensionPoints.put(name, extensionPoint);

    if (DEBUG_REGISTRATION) {
      //noinspection ThrowableResultOfMethodCallIgnored
      myEPTraces.put(name, new Throwable("Original registration for " + name));
    }
  }

  public void setLocked() {
    myLocked = true;

    for (ExtensionPointImpl extensionPoint : myExtensionPoints.values()) {
      extensionPoint.setLocked();
    }
  }

  @Nonnull
  public <T> ExtensionPointImpl<T> getExtensionPointImpl(@Nonnull String extensionPointName) {
    //noinspection unchecked
    ExtensionPointImpl<T> extensionPoint = myExtensionPoints.get(extensionPointName);
    if (extensionPoint == null) {
      throw new IllegalArgumentException("Missing extension point: " + extensionPointName + " in area " + myComponentManager);
    }
    return extensionPoint;
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  public <T> ExtensionPointImpl<T> getExtensionPointImpl(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionPointImpl(extensionPointName.getName());
  }

  @Nonnull
  @SuppressWarnings({"unchecked"})
  public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName) {
    ExtensionPointImpl<T> extensionPoint = myExtensionPoints.get(extensionPointName.getName());
    if (extensionPoint == null) {
      LOG.error("Missing extension point: " + extensionPointName + " in area " + myComponentManager);
      return EmptyExtensionPoint.get();
    }
    return extensionPoint;
  }

  @Nonnull
  public ExtensionPoint[] getExtensionPoints() {
    return myExtensionPoints.values().toArray(new ExtensionPoint[myExtensionPoints.size()]);
  }

  private boolean hasExtensionPoint(@Nonnull String extensionPointName) {
    return myExtensionPoints.containsKey(extensionPointName);
  }
}
