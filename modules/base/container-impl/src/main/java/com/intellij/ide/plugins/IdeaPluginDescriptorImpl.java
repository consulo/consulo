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
package com.intellij.ide.plugins;

import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtilRt;
import consulo.container.impl.parser.ExtensionInfo;
import consulo.container.impl.parser.PluginBeanParser;
import consulo.container.plugin.PluginDescriptorStub;
import consulo.container.plugin.PluginIds;
import consulo.util.nodep.map.SimpleMultiMap;
import consulo.util.nodep.xml.SimpleXmlParsingException;
import consulo.util.nodep.xml.SimpleXmlReader;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

/**
 * @author mike
 */
public class IdeaPluginDescriptorImpl extends PluginDescriptorStub {
  public static final IdeaPluginDescriptorImpl[] EMPTY_ARRAY = new IdeaPluginDescriptorImpl[0];

  private String myName;
  private PluginId myId;
  private String myResourceBundleBaseName;
  private String myChangeNotes;
  private String myVersion;
  private String myPlatformVersion;
  private String myVendor;
  private String myVendorEmail;
  private String myVendorUrl;
  private String myCategory;
  private String url;
  private final File myPath;
  private final boolean myIsPreInstalled;
  private PluginId[] myDependencies = PluginId.EMPTY_ARRAY;
  private PluginId[] myOptionalDependencies = PluginId.EMPTY_ARRAY;
  private Map<PluginId, String> myOptionalConfigs;
  private Map<PluginId, IdeaPluginDescriptorImpl> myOptionalDescriptors;
  private List<SimpleXmlElement> myActionsElements = Collections.emptyList();
  private List<ComponentConfig> myAppComponents = Collections.emptyList();
  private List<ComponentConfig> myProjectComponents = Collections.emptyList();
  private boolean myDeleted = false;
  private ClassLoader myLoader;
  private Collection<HelpSetPath> myHelpSets = Collections.emptyList();
  @Nullable
  private SimpleMultiMap<String, ExtensionInfo> myExtensions;
  @Nullable
  private SimpleMultiMap<String, SimpleXmlElement> myExtensionsPoints;
  private String myDescriptionChildText;
  private boolean myEnabled = true;
  private Boolean mySkipped;

  public IdeaPluginDescriptorImpl(@Nonnull File pluginPath, boolean isPreInstalled) {
    myPath = pluginPath;
    myIsPreInstalled = isPreInstalled;
  }

  @Nonnull
  private static List<ComponentConfig> mergeComponents(@Nonnull List<ComponentConfig> first, @Nonnull List<ComponentConfig> second) {
    if (first.isEmpty()) {
      return second;
    }
    if (second.isEmpty()) {
      return first;
    }
    List<ComponentConfig> result = new ArrayList<ComponentConfig>(first.size() + second.size());
    result.addAll(first);
    result.addAll(second);
    return result;
  }

  @Override
  public File getPath() {
    return myPath;
  }

  public void readExternal(@Nonnull InputStream stream) throws SimpleXmlParsingException {
    SimpleXmlElement element = SimpleXmlReader.parse(stream);
    readExternal(element);
  }

  public void readExternal(@Nonnull URL url) throws SimpleXmlParsingException {
    SimpleXmlElement element = SimpleXmlReader.parse(url);
    readExternal(element);
  }

  private void readExternal(@Nonnull SimpleXmlElement element) {
    final PluginBean pluginBean = PluginBeanParser.parseBean(element, null);
    assert pluginBean != null;
    url = pluginBean.url;
    myName = pluginBean.name;
    String idString = pluginBean.id;
    if (idString == null || idString.isEmpty()) {
      idString = myName;
    }
    myId = idString == null ? null : PluginId.getId(idString);
    myResourceBundleBaseName = pluginBean.resourceBundle;

    myDescriptionChildText = pluginBean.description;
    myChangeNotes = pluginBean.changeNotes;
    myVersion = pluginBean.pluginVersion;
    myPlatformVersion = pluginBean.platformVersion;
    myCategory = pluginBean.category;


    if (pluginBean.vendor != null) {
      myVendor = pluginBean.vendor.name;
      myVendorEmail = pluginBean.vendor.email;
      myVendorUrl = pluginBean.vendor.url;
    }

    // preserve items order as specified in xml (filterBadPlugins will not fail if module comes first)
    Set<PluginId> dependentPlugins = new LinkedHashSet<PluginId>();
    // we always depend to core plugin, but prevent recursion
    if (!PluginIds.CONSULO_PLATFORM_BASE.equals(myId)) {
      dependentPlugins.add(PluginIds.CONSULO_PLATFORM_BASE);
    }
    Set<PluginId> optionalDependentPlugins = new LinkedHashSet<PluginId>();
    if (pluginBean.dependencies != null) {
      myOptionalConfigs = new HashMap<PluginId, String>();
      for (PluginDependency dependency : pluginBean.dependencies) {
        String text = dependency.pluginId;
        if (!StringUtilRt.isEmpty(text)) {
          PluginId id = PluginId.getId(text);
          dependentPlugins.add(id);
          if (dependency.optional) {
            optionalDependentPlugins.add(id);
            if (!StringUtilRt.isEmpty(dependency.configFile)) {
              myOptionalConfigs.put(id, dependency.configFile);
            }
          }
        }
      }
    }

    myDependencies = dependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : dependentPlugins.toArray(new PluginId[dependentPlugins.size()]);
    myOptionalDependencies = optionalDependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependentPlugins.toArray(new PluginId[optionalDependentPlugins.size()]);

    if (!pluginBean.helpSets.isEmpty()) {
      myHelpSets = new ArrayList<HelpSetPath>(pluginBean.helpSets.size());
      List<PluginHelpSet> sets = pluginBean.helpSets;
      for (PluginHelpSet pluginHelpSet : sets) {
        myHelpSets.add(new HelpSetPath(pluginHelpSet.file, pluginHelpSet.path));
      }
    }

    myAppComponents = pluginBean.applicationComponents;
    myProjectComponents = pluginBean.projectComponents;

    List<ExtensionInfo> extensions = pluginBean.extensions;
    if (extensions != null) {
      myExtensions = SimpleMultiMap.newHashMap();
      for (ExtensionInfo extension : extensions) {
        String extId = extension.getPluginId() + "." + extension.getElement().getName();
        myExtensions.putValue(extId, extension);
      }
    }

    List<SimpleXmlElement> extensionPoints = pluginBean.extensionPoints;
    if (extensionPoints != null) {
      myExtensionsPoints = SimpleMultiMap.newHashMap();
      for (SimpleXmlElement extensionPoint : extensionPoints) {
        String areaId = extensionPoint.getAttributeValue("area", "");

        myExtensionsPoints.putValue(areaId, extensionPoint);
      }
    }

    myActionsElements = pluginBean.actions;
  }

  @Override
  public String getDescription() {
    return myDescriptionChildText;
  }

  @Override
  public String getChangeNotes() {
    return myChangeNotes;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  @Nonnull
  public PluginId[] getDependentPluginIds() {
    return myDependencies;
  }

  @Override
  @Nonnull
  public PluginId[] getOptionalDependentPluginIds() {
    return myOptionalDependencies;
  }

  @Override
  public String getVendor() {
    return myVendor;
  }

  @Override
  public String getVersion() {
    return myVersion;
  }

  @Override
  public String getPlatformVersion() {
    return myPlatformVersion;
  }

  @Override
  public String getResourceBundleBaseName() {
    return myResourceBundleBaseName;
  }

  @Override
  public String getCategory() {
    return myCategory;
  }

  /*
     This setter was explicitly defined to be able to set a category for a
     descriptor outside its loading from the xml file.
     Problem was that most commonly plugin authors do not publish the plugin's
     category in its .xml file so to be consistent in plugins representation
     (e.g. in the Plugins form) we have to set this value outside.
  */
  public void setCategory(String category) {
    myCategory = category;
  }

  @Nullable
  public SimpleMultiMap<String, SimpleXmlElement> getExtensionsPoints() {
    return myExtensionsPoints;
  }

  @Nullable
  public SimpleMultiMap<String, ExtensionInfo> getExtensions() {
    return myExtensions;
  }

  @Nonnull
  public List<File> getClassPath() {
    if (myPath.isDirectory()) {
      final List<File> result = new ArrayList<File>();

      final File[] files = new File(myPath, "lib").listFiles();
      if (files != null && files.length > 0) {
        for (final File f : files) {
          if (f.isFile()) {
            if (FileUtilRt.isJarOrZip(f)) {
              result.add(f);
            }
          }
          else {
            result.add(f);
          }
        }
      }

      return result;
    }
    else {
      return Collections.singletonList(myPath);
    }
  }

  @Override
  @Nonnull
  public List<SimpleXmlElement> getActionsDescriptionElements() {
    return myActionsElements;
  }

  @Override
  @Nonnull
  public List<ComponentConfig> getAppComponents() {
    return myAppComponents;
  }

  @Override
  @Nonnull
  public List<ComponentConfig> getProjectComponents() {
    return myProjectComponents;
  }

  @Override
  public String getVendorEmail() {
    return myVendorEmail;
  }

  @Override
  public String getVendorUrl() {
    return myVendorUrl;
  }

  @Override
  public String getUrl() {
    return url;
  }

  @Override
  @NonNls
  public String toString() {
    return "PluginDescriptor[name='" + myName + "', classpath='" + myPath + "']";
  }

  public boolean isDeleted() {
    return myDeleted;
  }

  public void setDeleted(boolean deleted) {
    myDeleted = deleted;
  }

  public void setLoader(ClassLoader loader) {
    myLoader = loader;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof IdeaPluginDescriptorImpl)) return false;

    final IdeaPluginDescriptorImpl pluginDescriptor = (IdeaPluginDescriptorImpl)o;

    return myName == null ? pluginDescriptor.myName == null : myName.equals(pluginDescriptor.myName);
  }

  @Override
  public int hashCode() {
    return myName != null ? myName.hashCode() : 0;
  }

  @Override
  @Nonnull
  public Collection<HelpSetPath> getHelpSets() {
    return myHelpSets;
  }

  @Nonnull
  @Override
  public PluginId getPluginId() {
    return myId;
  }

  @Nonnull
  @Override
  public ClassLoader getPluginClassLoader() {
    return myLoader != null ? myLoader : getClass().getClassLoader();
  }

  @Override
  public boolean isEnabled() {
    return myEnabled;
  }

  @Override
  public void setEnabled(final boolean enabled) {
    myEnabled = enabled;
  }

  @Nullable
  public Map<PluginId, String> getOptionalConfigs() {
    return myOptionalConfigs;
  }

  @Nullable
  Map<PluginId, IdeaPluginDescriptorImpl> getOptionalDescriptors() {
    return myOptionalDescriptors;
  }

  public void setOptionalDescriptors(final Map<PluginId, IdeaPluginDescriptorImpl> optionalDescriptors) {
    myOptionalDescriptors = optionalDescriptors;
  }

  void mergeOptionalConfig(final IdeaPluginDescriptorImpl descriptor) {
    if (myExtensions == null) {
      myExtensions = descriptor.myExtensions;
    }
    else if (descriptor.myExtensions != null) {
      myExtensions.putAllValues(descriptor.myExtensions);
    }

    if (myExtensionsPoints == null) {
      myExtensionsPoints = descriptor.myExtensionsPoints;
    }
    else if (descriptor.myExtensionsPoints != null) {
      myExtensionsPoints.putAllValues(descriptor.myExtensionsPoints);
    }

    if (myActionsElements == null) {
      myActionsElements = descriptor.myActionsElements;
    }
    else if (descriptor.myActionsElements != null) {
      myActionsElements.addAll(descriptor.myActionsElements);
    }

    myAppComponents = mergeComponents(myAppComponents, descriptor.myAppComponents);
    myProjectComponents = mergeComponents(myProjectComponents, descriptor.myProjectComponents);
  }

  public Boolean getSkipped() {
    return mySkipped;
  }

  public void setSkipped(final Boolean skipped) {
    mySkipped = skipped;
  }

  @Override
  public boolean isBundled() {
    return myIsPreInstalled;
  }
}
