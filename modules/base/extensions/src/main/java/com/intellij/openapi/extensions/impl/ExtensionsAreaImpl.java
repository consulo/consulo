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

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.*;
import com.intellij.util.containers.ContainerUtil;
import gnu.trove.THashMap;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Namespace;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtensionsAreaImpl implements ExtensionsArea {
  private static final Logger LOGGER = Logger.getInstance(ExtensionsAreaImpl.class);

  public static final String ATTRIBUTE_AREA = "area";

  private static final boolean DEBUG_REGISTRATION = false;

  private final Throwable myCreationTrace;
  private final Map<String, ExtensionPointImpl> myExtensionPoints = ContainerUtil.newConcurrentMap();
  private final Map<String, Throwable> myEPTraces = DEBUG_REGISTRATION ? new THashMap<>() : null;

  private final List<Runnable> mySuspendedListenerActions = new ArrayList<>();
  private boolean myAvailabilityNotificationsActive = true;

  private final AreaInstance myAreaInstance;
  private final String myAreaClass;

  public ExtensionsAreaImpl(String areaClass, AreaInstance areaInstance) {
    myCreationTrace = DEBUG_REGISTRATION ? new Throwable("Area creation trace") : null;
    myAreaClass = areaClass;
    myAreaInstance = areaInstance;
  }

  @Override
  public String getAreaClass() {
    return myAreaClass;
  }

  @Override
  public void registerExtensionPoint(@Nonnull String pluginName, @Nonnull Element extensionPointElement) {
    registerExtensionPoint(new DefaultPluginDescriptor(PluginId.getId(pluginName)), extensionPointElement);
  }

  @Override
  public void registerExtensionPoint(@Nonnull PluginDescriptor pluginDescriptor, @Nonnull Element extensionPointElement) {
    assert pluginDescriptor.getPluginId() != null;
    final String pluginId = pluginDescriptor.getPluginId().getIdString();
    String epName = extensionPointElement.getAttributeValue("qualifiedName");
    if (epName == null) {
      final String name = extensionPointElement.getAttributeValue("name");
      if (name == null) {
        throw new RuntimeException("'name' attribute not specified for extension point in '" + pluginId + "' plugin");
      }
      epName = pluginId + '.' + name;
    }

    String beanClassName = extensionPointElement.getAttributeValue("beanClass");
    String interfaceClassName = extensionPointElement.getAttributeValue("interface");
    if (beanClassName == null && interfaceClassName == null) {
      throw new RuntimeException("Neither 'beanClass' nor 'interface' attribute is specified for extension point '" + epName + "' in '" + pluginId + "' plugin");
    }
    if (beanClassName != null && interfaceClassName != null) {
      throw new RuntimeException("Both 'beanClass' and 'interface' attributes are specified for extension point '" + epName + "' in '" + pluginId + "' plugin");
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

  @Override
  public void registerExtension(@Nonnull final String pluginName, @Nonnull final Element extensionElement) {
    registerExtension(new DefaultPluginDescriptor(PluginId.getId(pluginName)), extensionElement);
  }

  @Override
  public void registerExtension(@Nonnull final PluginDescriptor pluginDescriptor, @Nonnull final Element extensionElement) {
    final PluginId pluginId = pluginDescriptor.getPluginId();

    String epName = extractEPName(extensionElement);

    ExtensionComponentAdapter adapter;
    final ExtensionPointImpl extensionPoint = getExtensionPoint(epName);
    if (extensionPoint.getKind() == ExtensionPoint.Kind.INTERFACE) {
      String implClass = extensionElement.getAttributeValue("implementation");
      if (implClass == null) {
        throw new RuntimeException("'implementation' attribute not specified for '" + epName + "' extension in '" + pluginId.getIdString() + "' plugin");
      }
      adapter = new ExtensionComponentAdapter(implClass, extensionElement, pluginDescriptor, shouldDeserializeInstance(extensionElement));
    }
    else {
      adapter = new ExtensionComponentAdapter(extensionPoint.getClassName(), extensionElement, pluginDescriptor, true);
    }
    extensionPoint.registerExtensionAdapter(adapter);
  }

  private static boolean shouldDeserializeInstance(Element extensionElement) {
    // has content
    if (!extensionElement.getContent().isEmpty()) return true;
    // has custom attributes
    for (Attribute attribute : extensionElement.getAttributes()) {
      final String name = attribute.getName();
      if (!"implementation".equals(name) && !"id".equals(name) && !"order".equals(name)) {
        return true;
      }
    }
    return false;
  }

  public static String extractEPName(final Element extensionElement) {
    String epName = extensionElement.getAttributeValue("point");

    if (epName == null) {
      final Element parentElement = extensionElement.getParentElement();
      final String ns = parentElement != null ? parentElement.getAttributeValue("defaultExtensionNs") : null;

      if (ns != null) {
        epName = ns + '.' + extensionElement.getName();
      }
      else {
        Namespace namespace = extensionElement.getNamespace();
        epName = namespace.getURI() + '.' + extensionElement.getName();
      }
    }
    return epName;
  }

  public Throwable getCreationTrace() {
    return myCreationTrace;
  }

  @Override
  public void registerExtensionPoint(@Nonnull final String extensionPointName, @Nonnull String extensionPointBeanClass) {
    registerExtensionPoint(extensionPointName, extensionPointBeanClass, ExtensionPoint.Kind.INTERFACE);
  }

  @Override
  public void registerExtensionPoint(@Nonnull @NonNls String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull ExtensionPoint.Kind kind) {
    registerExtensionPoint(extensionPointName, extensionPointBeanClass, new UndefinedPluginDescriptor(), kind);
  }

  @Override
  public void registerExtensionPoint(@Nonnull final String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull PluginDescriptor descriptor) {
    registerExtensionPoint(extensionPointName, extensionPointBeanClass, descriptor, ExtensionPoint.Kind.INTERFACE);
  }

  private void registerExtensionPoint(@Nonnull String extensionPointName, @Nonnull String extensionPointBeanClass, @Nonnull PluginDescriptor descriptor, @Nonnull ExtensionPoint.Kind kind) {
    if (hasExtensionPoint(extensionPointName)) {
      if (DEBUG_REGISTRATION) {
        final ExtensionPointImpl oldEP = getExtensionPoint(extensionPointName);
        LOGGER.error("Duplicate registration for EP: " + extensionPointName + ": original plugin " + oldEP.getDescriptor().getPluginId() + ", new plugin " + descriptor.getPluginId(),
                       myEPTraces.get(extensionPointName));
      }
      throw new RuntimeException("Duplicate registration for EP: " + extensionPointName);
    }

    registerExtensionPoint(new ExtensionPointImpl(extensionPointName, extensionPointBeanClass, kind, myAreaInstance, descriptor));
  }

  public void registerExtensionPoint(@Nonnull ExtensionPointImpl extensionPoint) {
    String name = extensionPoint.getName();
    myExtensionPoints.put(name, extensionPoint);

    if (DEBUG_REGISTRATION) {
      //noinspection ThrowableResultOfMethodCallIgnored
      myEPTraces.put(name, new Throwable("Original registration for " + name));
    }
  }

  private void queueNotificationAction(final Runnable action) {
    if (myAvailabilityNotificationsActive) {
      action.run();
    }
    else {
      mySuspendedListenerActions.add(action);
    }
  }

  @Override
  @Nonnull
  public <T> ExtensionPointImpl<T> getExtensionPoint(@Nonnull String extensionPointName) {
    //noinspection unchecked
    ExtensionPointImpl<T> extensionPoint = myExtensionPoints.get(extensionPointName);
    if (extensionPoint == null) {
      throw new IllegalArgumentException("Missing extension point: " + extensionPointName + " in area " + myAreaInstance);
    }
    return extensionPoint;
  }

  @Nonnull
  @Override
  @SuppressWarnings({"unchecked"})
  public <T> ExtensionPoint<T> getExtensionPoint(@Nonnull ExtensionPointName<T> extensionPointName) {
    return getExtensionPoint(extensionPointName.getName());
  }

  @Nonnull
  @Override
  public ExtensionPoint[] getExtensionPoints() {
    return myExtensionPoints.values().toArray(new ExtensionPoint[myExtensionPoints.size()]);
  }

  @Override
  public void unregisterExtensionPoint(@Nonnull final String extensionPointName) {
    ExtensionPoint extensionPoint = myExtensionPoints.get(extensionPointName);
    if (extensionPoint != null) {
      extensionPoint.reset();
      myExtensionPoints.remove(extensionPointName);
    }
  }

  @Override
  public boolean hasExtensionPoint(@Nonnull String extensionPointName) {
    return myExtensionPoints.containsKey(extensionPointName);
  }

  @Override
  public void suspendInteractions() {
    myAvailabilityNotificationsActive = false;
  }

  @Override
  public void resumeInteractions() {
    myAvailabilityNotificationsActive = true;
    ExtensionPoint[] extensionPoints = getExtensionPoints();
    for (ExtensionPoint extensionPoint : extensionPoints) {
      extensionPoint.getExtensions(); // creates extensions from ComponentAdapters
    }
    for (Runnable action : mySuspendedListenerActions) {
      try {
        action.run();
      }
      catch (Exception e) {
        LOGGER.error(e);
      }
    }
    mySuspendedListenerActions.clear();
  }

  @Override
  public void killPendingInteractions() {
    mySuspendedListenerActions.clear();
  }
}
