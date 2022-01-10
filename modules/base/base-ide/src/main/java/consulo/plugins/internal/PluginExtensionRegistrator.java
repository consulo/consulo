/*
 * Copyright 2013-2020 consulo.io
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
package consulo.plugins.internal;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.ExtensionPoint;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import consulo.container.impl.PluginDescriptorImpl;
import consulo.container.impl.parser.ExtensionInfo;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.nodep.map.SimpleMultiMap;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author VISTALL
 * @since 2020-05-23
 */
public class PluginExtensionRegistrator {
  private static final Logger LOG = Logger.getInstance(PluginExtensionRegistrator.class);

  public static void registerExtensionPointsAndExtensions(ExtensionAreaId areaId, ExtensionsAreaImpl area) {
    Iterable<PluginDescriptor> plugins = PluginManager.getPlugins();
    List<PluginDescriptor> list = new ArrayList<>();
    for (PluginDescriptor plugin : plugins) {
      if (!PluginManagerCore.shouldSkipPlugin(plugin)) {
        list.add(plugin);
      }
    }

    registerExtensionPointsAndExtensions(areaId, area, list);
  }

  private static void registerExtensionPointsAndExtensions(ExtensionAreaId areaId, ExtensionsAreaImpl area, List<? extends PluginDescriptor> pluginDescriptors) {
    for (PluginDescriptor descriptor : pluginDescriptors) {
      registerExtensionPoints(((PluginDescriptorImpl)descriptor), areaId, area);
    }

    ExtensionPoint[] extensionPoints = area.getExtensionPoints();
    Set<String> epNames = new HashSet<>(extensionPoints.length);
    for (ExtensionPoint point : extensionPoints) {
      epNames.add(point.getName());
    }

    for (PluginDescriptor descriptor : pluginDescriptors) {
      PluginDescriptorImpl pluginDescriptor = (PluginDescriptorImpl)descriptor;

      SimpleMultiMap<String, ExtensionInfo> extensions = pluginDescriptor.getExtensions();

      for (String epName : epNames) {
        Collection<ExtensionInfo> extensionInfos = extensions.get(epName);
        for (ExtensionInfo extensionInfo : extensionInfos) {
          area.registerExtension(pluginDescriptor, extensionInfo);
        }
      }
    }
  }

  private static void registerExtensionPoints(PluginDescriptorImpl pluginDescriptor, ExtensionAreaId areaId, @Nonnull ExtensionsAreaImpl area) {
    SimpleMultiMap<String, SimpleXmlElement> extensionsPoints = pluginDescriptor.getExtensionsPoints();

    Collection<SimpleXmlElement> extensionPoints = extensionsPoints.get(areaId.name());

    for (SimpleXmlElement element : extensionPoints) {
      area.registerExtensionPoint(pluginDescriptor, element);
    }

    String oldAreaId = "CONSULO_" + areaId;
    Collection<SimpleXmlElement> oldExtensionPoints = extensionsPoints.get(oldAreaId);
    for (SimpleXmlElement oldElement : oldExtensionPoints) {
      LOG.warn("Using old area id " + oldAreaId + ": " + oldElement);

      area.registerExtensionPoint(pluginDescriptor, oldElement);
    }
  }
}
