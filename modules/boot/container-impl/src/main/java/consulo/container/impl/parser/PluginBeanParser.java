/*
 * Copyright 2013-2019 consulo.io
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
package consulo.container.impl.parser;

import consulo.container.plugin.ComponentConfig;
import consulo.container.plugin.PluginListenerDescriptor;
import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

/**
 * @author VISTALL
 * @since 2019-03-24
 */
public class PluginBeanParser {
  private static Set<String> ourAllowedRootTags = new HashSet<String>(Arrays.asList("consulo-plugin", "idea-plugin"));

  @Nullable
  public static PluginBean parseBean(@Nonnull SimpleXmlElement rootTag, @Nullable String pluginId) {
    String rootTagName = rootTag.getName();
    if (!ourAllowedRootTags.contains(rootTagName)) {
      return null;
    }

    PluginBean pluginBean = new PluginBean();
    pluginBean.id = rootTag.getChildText("id");
    if (StringUtilRt.isEmpty(pluginBean.id)) {
      pluginBean.id = pluginId;
    }

    pluginBean.name = rootTag.getChildText("name");
    pluginBean.description = rootTag.getChildText("description");
    pluginBean.pluginVersion = rootTag.getChildText("version");
    pluginBean.platformVersion = rootTag.getChildText("platformVersion");
    pluginBean.category = rootTag.getChildText("category");
    pluginBean.resourceBundle = rootTag.getChildText("resource-bundle");
    pluginBean.localize = rootTag.getChildText("localize");
    pluginBean.changeNotes = rootTag.getChildText("change-notes");
    pluginBean.url = rootTag.getAttributeValue("url");
    pluginBean.experimental = Boolean.parseBoolean(rootTag.getChildText("experimental"));

    List<String> imports = new ArrayList<String>();

    List<SimpleXmlElement> anImport = rootTag.getChildren("import");
    if (!anImport.isEmpty()) {
      for (SimpleXmlElement element : anImport) {
        imports.add(element.getAttributeValue("path"));
      }
    }

    List<SimpleXmlElement> xiInclude = rootTag.getChildren("xi:include");
    if (!xiInclude.isEmpty()) {
      for (SimpleXmlElement element : xiInclude) {
        imports.add(element.getAttributeValue("href"));
      }
    }

    pluginBean.imports = imports;

    pluginBean.extensionPoints = expandChildren(rootTag, "extensionPoints");

    List<SimpleXmlElement> extensionsTag = rootTag.getChildren("extensions");

    List<ExtensionInfo> extensionInfos = new ArrayList<ExtensionInfo>();

    for (SimpleXmlElement extensionElement : extensionsTag) {
      // FIXME [VISTALL] rename later to pluginId?
      String defaultExtensionNs = extensionElement.getAttributeValue("defaultExtensionNs");
      if (defaultExtensionNs == null) {
        defaultExtensionNs = pluginBean.id;
      }

      for (SimpleXmlElement childExtension : extensionElement.getChildren()) {
        extensionInfos.add(new ExtensionInfo(defaultExtensionNs, childExtension));
      }
    }

    pluginBean.extensions = extensionInfos;

    pluginBean.actions = expandChildren(rootTag, "actions");

    SimpleXmlElement vendorElement = rootTag.getChild("vendor");
    if (vendorElement != null) {
      PluginVendor vendor = new PluginVendor();
      vendor.name = vendorElement.getText();
      vendor.url = vendorElement.getAttributeValue("url");
      vendor.email = vendorElement.getAttributeValue("email");

      pluginBean.vendor = vendor;
    }

    List<PluginHelpSet> pluginHelpSets = new ArrayList<PluginHelpSet>();
    for (SimpleXmlElement helpsetTag : rootTag.getChildren("helpset")) {
      PluginHelpSet pluginHelpSet = new PluginHelpSet();
      pluginHelpSet.file = helpsetTag.getAttributeValue("file");
      pluginHelpSet.path = helpsetTag.getAttributeValue("path");

      pluginHelpSets.add(pluginHelpSet);
    }

    if (!pluginHelpSets.isEmpty()) {
      pluginBean.helpSets = pluginHelpSets;
    }

    List<PluginDependency> pluginDependencies = new ArrayList<PluginDependency>();
    for (SimpleXmlElement dependsElement : rootTag.getChildren("depends")) {
      PluginDependency pluginDependency = new PluginDependency();
      pluginDependencies.add(pluginDependency);

      pluginDependency.configFile = dependsElement.getAttributeValue("config-file");
      pluginDependency.optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      pluginDependency.pluginId = dependsElement.getText();
    }

    if (!pluginDependencies.isEmpty()) {
      pluginBean.dependencies = pluginDependencies;
    }

    pluginBean.applicationListeners = readListeners(rootTag, "applicationListeners");
    pluginBean.projectListeners = readListeners(rootTag, "projectListeners");
    pluginBean.moduleListeners = readListeners(rootTag, "moduleListeners");

    Map<String, Set<String>> permissions = new HashMap<String, Set<String>>();
    for (SimpleXmlElement permissionsElement : rootTag.getChildren("permissions")) {
      for (SimpleXmlElement permissionElement : permissionsElement.getChildren("permission")) {
        String permissionType = permissionElement.getAttributeValue("type", "NOT_SET");

        Set<String> options = new LinkedHashSet<String>();

        for (SimpleXmlElement permissionOption : permissionElement.getChildren("permission-option")) {
          options.add(permissionOption.getText());
        }

        permissions.put(permissionType, options);
      }
    }

    if (!permissions.isEmpty()) {
      pluginBean.permissions = permissions;
    }

    Set<String> tags = new TreeSet<String>();
    for (SimpleXmlElement tagsElement : rootTag.getChildren("tags")) {
      for (SimpleXmlElement tagElement : tagsElement.getChildren("tag")) {
        tags.add(tagElement.getText());
      }
    }

    if(!tags.isEmpty()) {
      pluginBean.tags = tags;
    }

    List<String> incompatibleWith = new ArrayList<String>();
    for (SimpleXmlElement incompatibleWithElement : rootTag.getChildren("incompatible-with")) {
      incompatibleWith.add(incompatibleWithElement.getText());
    }

    if(!incompatibleWith.isEmpty()) {
      pluginBean.incompatibleWith = incompatibleWith;
    }

    // region deprecated stuff
    List<ComponentConfig> appComponents = new ArrayList<ComponentConfig>();
    for (SimpleXmlElement appComponentElements : rootTag.getChildren("application-components")) {
      parseComponent(appComponentElements, appComponents);
    }

    if (!appComponents.isEmpty()) {
      pluginBean.applicationComponents = appComponents;
    }

    List<ComponentConfig> projectComponents = new ArrayList<ComponentConfig>();
    for (SimpleXmlElement appComponentElements : rootTag.getChildren("project-components")) {
      parseComponent(appComponentElements, projectComponents);
    }

    if (!projectComponents.isEmpty()) {
      pluginBean.projectComponents = projectComponents;
    }

    // endregion

    return pluginBean;
  }

  private static List<PluginListenerDescriptor> readListeners(SimpleXmlElement parent, String tagName) {
    List<SimpleXmlElement> children = parent.getChildren(tagName);
    if (children.isEmpty()) {
      return Collections.emptyList();
    }

    List<PluginListenerDescriptor> list = new ArrayList<PluginListenerDescriptor>();

    for (SimpleXmlElement listenersElement : children) {
      for (SimpleXmlElement listenerElement : listenersElement.getChildren()) {
        String implClass = listenerElement.getAttributeValue("class");
        String topicClass = listenerElement.getAttributeValue("topic");
        boolean activeInTestMode = Boolean.valueOf(listenerElement.getAttributeValue("activeInTestMode", "true"));
        boolean activeInHeadlessMode = Boolean.valueOf(listenerElement.getAttributeValue("activeInHeadlessMode", "true"));

        if (implClass == null) {
          throw new IllegalArgumentException("'class' empty: " + listenerElement);
        }

        if (topicClass == null) {
          throw new IllegalArgumentException("'topic' empty: " + listenerElement);
        }

        list.add(new PluginListenerDescriptor(implClass, topicClass, activeInTestMode, activeInHeadlessMode));
      }
    }

    return list;
  }

  @Nonnull
  private static List<SimpleXmlElement> expandChildren(SimpleXmlElement element, String childTag) {
    List<SimpleXmlElement> list = Collections.emptyList();

    for (SimpleXmlElement child : element.getChildren(childTag)) {
      List<SimpleXmlElement> children = child.getChildren();
      if (!children.isEmpty()) {
        if (list.isEmpty()) {
          list = new ArrayList<SimpleXmlElement>();
        }

        list.addAll(children);
      }
    }

    return list;
  }

  private static void parseComponent(SimpleXmlElement componentsParent, List<ComponentConfig> configConsumer) {
    for (SimpleXmlElement componentElement : componentsParent.getChildren("component")) {
      ComponentConfig componentConfig = new ComponentConfig();
      componentConfig.setHeadlessImplementationClass(componentElement.getChildText("headless-implementation-class"));
      componentConfig.setImplementationClass(componentElement.getChildText("implementation-class"));
      componentConfig.setInterfaceClass(componentElement.getChildText("interface-class"));

      for (SimpleXmlElement optionElement : componentElement.getChildren("option")) {
        String name = optionElement.getAttributeValue("name");
        String value = optionElement.getAttributeValue("value");

        componentConfig.options.put(name, value);
      }

      configConsumer.add(componentConfig);
    }
  }
}
