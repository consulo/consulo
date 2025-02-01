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
package consulo.container.internal.plugin;

import consulo.container.internal.plugin.PluginBean;
import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import java.util.*;

/**
 * @author VISTALL
 * @since 2019-03-24
 */
public class PluginBeanParser {
  private static Set<String> ourAllowedRootTags = new HashSet<String>(Arrays.asList("consulo-plugin"));

  public static PluginBean parseBean(SimpleXmlElement rootTag, String pluginId) {
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
    pluginBean.actions = expandChildren(rootTag, "actions");

    SimpleXmlElement vendorElement = rootTag.getChild("vendor");
    if (vendorElement != null) {
      PluginVendor vendor = new PluginVendor();
      vendor.name = vendorElement.getText();
      vendor.url = vendorElement.getAttributeValue("url");
      vendor.email = vendorElement.getAttributeValue("email");

      pluginBean.vendor = vendor;
    }

    List<PluginDependency> pluginDependencies = new ArrayList<PluginDependency>();
    for (SimpleXmlElement dependsElement : rootTag.getChildren("depends")) {
      PluginDependency pluginDependency = new PluginDependency();
      pluginDependencies.add(pluginDependency);

      pluginDependency.optional = Boolean.parseBoolean(dependsElement.getAttributeValue("optional", "false"));
      pluginDependency.pluginId = dependsElement.getText();
    }

    if (!pluginDependencies.isEmpty()) {
      pluginBean.dependencies = pluginDependencies;
    }

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

    if (!tags.isEmpty()) {
      pluginBean.tags = tags;
    }

    List<String> incompatibleWith = new ArrayList<String>();
    for (SimpleXmlElement incompatibleWithElement : rootTag.getChildren("incompatible-with")) {
      incompatibleWith.add(incompatibleWithElement.getText());
    }

    if (!incompatibleWith.isEmpty()) {
      pluginBean.incompatibleWith = incompatibleWith;
    }

   return pluginBean;
  }

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
}
