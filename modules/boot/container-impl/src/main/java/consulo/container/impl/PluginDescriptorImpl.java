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
package consulo.container.impl;

import consulo.container.impl.parser.*;
import consulo.container.plugin.*;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.map.SimpleMultiMap;
import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.SimpleXmlParsingException;
import consulo.util.nodep.xml.SimpleXmlReader;
import consulo.util.nodep.xml.node.SimpleXmlElement;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author mike
 */
public class PluginDescriptorImpl extends PluginDescriptorStub {
  public static final PluginDescriptorImpl[] EMPTY_ARRAY = new PluginDescriptorImpl[0];

  private String myName;
  private PluginId myId;
  private String myResourceBundleBaseName;
  private String myLocalize;
  private String myChangeNotes;
  private String myVersion;
  private String myPlatformVersion;
  private String myVendor;
  private String myVendorEmail;
  private String myVendorUrl;
  private String myCategory;
  private String url;
  private final File myPath;
  private final byte[] myIconBytes;
  private final byte[] myDarkIconBytes;
  private final boolean myIsPreInstalled;
  private PluginId[] myDependencies = PluginId.EMPTY_ARRAY;
  private PluginId[] myOptionalDependencies = PluginId.EMPTY_ARRAY;
  private PluginId[] myIncompatibleWithPluginds = PluginId.EMPTY_ARRAY;
  private Map<PluginId, String> myOptionalConfigs;
  private Map<PluginId, PluginDescriptorImpl> myOptionalDescriptors;
  @Nonnull
  private List<SimpleXmlElement> myActionsElements = Collections.emptyList();
  private List<ComponentConfig> myAppComponents = Collections.emptyList();
  private List<ComponentConfig> myProjectComponents = Collections.emptyList();

  private List<PluginListenerDescriptor> myApplicationListeners = Collections.emptyList();
  private List<PluginListenerDescriptor> myProjectListeners = Collections.emptyList();
  private List<PluginListenerDescriptor> myModuleListeners = Collections.emptyList();

  private Map<PluginPermissionType, PluginPermissionDescriptor> myPermissionDescriptors = Collections.emptyMap();

  private boolean myDeleted = false;
  private ClassLoader myLoader;
  private Object myModuleLayer;

  private Collection<HelpSetPath> myHelpSets = Collections.emptyList();
  @Nonnull
  private SimpleMultiMap<String, ExtensionInfo> myExtensions = SimpleMultiMap.emptyMap();
  @Nonnull
  private SimpleMultiMap<String, SimpleXmlElement> myExtensionsPoints = SimpleMultiMap.emptyMap();

  private Set<String> myTags = Collections.emptySet();

  private String myDescriptionChildText;
  private boolean myEnabled = true;
  private Boolean mySkipped;
  private boolean myExperimental;

  public PluginDescriptorImpl(@Nonnull File pluginPath, @Nonnull byte[] iconBytes, @Nonnull byte[] darkIconBytes, boolean isPreInstalled) {
    myPath = pluginPath;
    myIconBytes = iconBytes;
    myDarkIconBytes = darkIconBytes;
    myIsPreInstalled = isPreInstalled;
  }

  @Override
  public File getPath() {
    return myPath;
  }

  public void readExternal(@Nonnull InputStream stream, @Nullable ZipFile zipFile, @Nonnull ContainerLogger log) throws SimpleXmlParsingException {
    SimpleXmlElement element = SimpleXmlReader.parse(stream);
    readExternal(element, zipFile, log);
  }

  private void readExternal(@Nonnull SimpleXmlElement element, @Nullable ZipFile zipFile, @Nonnull ContainerLogger log) throws SimpleXmlParsingException {
    final PluginBean pluginBean = PluginBeanParser.parseBean(element, null);
    assert pluginBean != null;
    url = pluginBean.url;
    myName = pluginBean.name;
    String idString = pluginBean.id;
    if (idString == null || idString.isEmpty()) {
      idString = myName;
    }
    myId = idString == null ? null : PluginId.getId(idString);
    myResourceBundleBaseName = StringUtilRt.isEmpty(pluginBean.resourceBundle) ? null : pluginBean.resourceBundle;
    myLocalize = StringUtilRt.isEmpty(pluginBean.localize) ? null : pluginBean.localize;

    myDescriptionChildText = pluginBean.description;
    myChangeNotes = pluginBean.changeNotes;
    myVersion = StringUtilRt.nullize(pluginBean.pluginVersion);
    myPlatformVersion = pluginBean.platformVersion;
    myCategory = pluginBean.category;
    myExperimental = pluginBean.experimental;

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

    Set<PluginId> incompatibleWithPluginds = new LinkedHashSet<PluginId>();
    for (String pluginId : pluginBean.incompatibleWith) {
      if (!StringUtilRt.isEmpty(pluginId)) {
        incompatibleWithPluginds.add(PluginId.getId(pluginId));
      }
    }
    myIncompatibleWithPluginds = incompatibleWithPluginds.isEmpty() ? PluginId.EMPTY_ARRAY : incompatibleWithPluginds.toArray(new PluginId[incompatibleWithPluginds.size()]);

    myDependencies = dependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : dependentPlugins.toArray(new PluginId[dependentPlugins.size()]);
    myOptionalDependencies = optionalDependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependentPlugins.toArray(new PluginId[optionalDependentPlugins.size()]);

    if (!pluginBean.helpSets.isEmpty()) {
      myHelpSets = new ArrayList<HelpSetPath>(pluginBean.helpSets.size());
      List<PluginHelpSet> sets = pluginBean.helpSets;
      for (PluginHelpSet pluginHelpSet : sets) {
        myHelpSets.add(new HelpSetPath(pluginHelpSet.file, pluginHelpSet.path));
      }
    }

    if (!pluginBean.tags.isEmpty()) {
      myTags = Collections.unmodifiableSet(pluginBean.tags);
    }

    if(pluginBean.experimental) {
      Set<String> oldTags = new TreeSet<String>(myTags);
      oldTags.add(EXPERIMENTAL_TAG);

      myTags = Collections.unmodifiableSet(oldTags);
    }

    for (Map.Entry<String, Set<String>> permissionEntry : pluginBean.permissions.entrySet()) {
      try {
        PluginPermissionType permissionType = PluginPermissionType.valueOf(permissionEntry.getKey());
        if (myPermissionDescriptors.isEmpty()) {
          myPermissionDescriptors = new HashMap<PluginPermissionType, PluginPermissionDescriptor>();
        }

        myPermissionDescriptors.put(permissionType, new PluginPermissionDescriptor(permissionType, permissionEntry.getValue()));
      }
      catch (IllegalArgumentException e) {
        log.warn("Unknown permissionType " + permissionEntry.getKey() + ", plugin: " + myId);
      }
    }

    extendPlugin(pluginBean);

    if (zipFile != null) {
      for (String importPath : pluginBean.imports) {
        if (importPath.charAt(0) == '/') {
          importPath = importPath.substring(1, importPath.length());
        }

        ZipEntry entry = zipFile.getEntry(importPath);
        if (entry != null) {
          try {
            InputStream childImportSteam = zipFile.getInputStream(entry);
            SimpleXmlElement childImportElement = SimpleXmlReader.parse(childImportSteam);

            PluginBean importBean = PluginBeanParser.parseBean(childImportElement, pluginBean.id);

            if (importBean == null) {
              continue;
            }

            extendPlugin(importBean);
          }
          catch (IOException e) {
            throw new SimpleXmlParsingException(e);
          }
        }
      }
    }

    for (PluginListenerDescriptor descriptor : myApplicationListeners) {
      descriptor.pluginDescriptor = this;
    }

    for (PluginListenerDescriptor descriptor : myProjectListeners) {
      descriptor.pluginDescriptor = this;
    }

    for (PluginListenerDescriptor descriptor : myModuleListeners) {
      descriptor.pluginDescriptor = this;
    }
  }

  private void extendPlugin(PluginBean pluginBean) {
    myAppComponents = mergeElements(myAppComponents, pluginBean.applicationComponents);
    myProjectComponents = mergeElements(myProjectComponents, pluginBean.projectComponents);
    myActionsElements = mergeElements(myActionsElements, pluginBean.actions);

    myApplicationListeners = mergeElements(myApplicationListeners, pluginBean.applicationListeners);
    myProjectListeners = mergeElements(myProjectListeners, pluginBean.projectListeners);
    myModuleListeners = mergeElements(myModuleListeners, pluginBean.moduleListeners);

    List<ExtensionInfo> extensions = pluginBean.extensions;
    if (extensions != null && !extensions.isEmpty()) {
      initializeExtensions();

      for (ExtensionInfo extension : extensions) {
        String extId = extension.getPluginId() + "." + extension.getElement().getName();
        myExtensions.putValue(extId, extension);
      }
    }

    List<SimpleXmlElement> extensionPoints = pluginBean.extensionPoints;
    if (extensionPoints != null && !extensionPoints.isEmpty()) {
      initializeExtensionPoints();

      for (SimpleXmlElement extensionPoint : extensionPoints) {
        String areaId = extensionPoint.getAttributeValue("area", "");

        if (areaId.isEmpty()) {
          areaId = "APPLICATION";
        }

        myExtensionsPoints.putValue(areaId, extensionPoint);
      }
    }
  }

  @Nonnull
  private static <T> List<T> mergeElements(@Nonnull List<T> original, @Nonnull List<T> additional) {
    if (additional.isEmpty()) {
      return original;
    }

    List<T> result = original;
    if (original.isEmpty()) {
      result = new ArrayList<T>(additional);
    }
    else {
      result.addAll(additional);
    }

    return result;
  }

  public void mergeOptionalConfig(final PluginDescriptorImpl descriptor) {
    initializeExtensions();

    myExtensions.putAll(descriptor.myExtensions);

    initializeExtensionPoints();

    myExtensionsPoints.putAll(descriptor.myExtensionsPoints);

    myActionsElements = mergeElements(myActionsElements, descriptor.myActionsElements);
    myAppComponents = mergeElements(myAppComponents, descriptor.myAppComponents);
    myProjectComponents = mergeElements(myProjectComponents, descriptor.myProjectComponents);
  }

  private void initializeExtensions() {
    if (myExtensions.isEmpty()) {
      myExtensions = SimpleMultiMap.newHashMap();
    }
  }

  private void initializeExtensionPoints() {
    if (myExtensionsPoints.isEmpty()) {
      myExtensionsPoints = SimpleMultiMap.newHashMap();
    }
  }

  @Nonnull
  @Override
  public Set<String> getTags() {
    return myTags;
  }

  @Override
  public String getDescription() {
    return myDescriptionChildText;
  }

  @Override
  public String getChangeNotes() {
    return myChangeNotes;
  }

  @Nonnull
  @Override
  public byte[] getIconBytes(boolean isDarkTheme) {
    if(isDarkTheme && myDarkIconBytes.length > 0) {
      return myDarkIconBytes;
    }
    return myIconBytes;
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

  @Nonnull
  @Override
  public PluginId[] getIncompatibleWithPlugindIds() {
    return myIncompatibleWithPluginds;
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

  @Nullable
  @Override
  public String getLocalize() {
    return myLocalize;
  }

  @Override
  public String getCategory() {
    return normalizeCategory(myCategory);
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

  @Nonnull
  public SimpleMultiMap<String, SimpleXmlElement> getExtensionsPoints() {
    return myExtensionsPoints;
  }

  @Nonnull
  public SimpleMultiMap<String, ExtensionInfo> getExtensions() {
    return myExtensions;
  }

  @Nonnull
  public List<File> getClassPath() {
    if (myPath.isDirectory()) {
      final List<File> result = new ArrayList<File>();
      fillLibs(new File(myPath, "lib"), result);
      return result;
    }
    else {
      return Collections.singletonList(myPath);
    }
  }

  private static void fillLibs(File libDirectory, List<File> result) {
    final File[] files = libDirectory.listFiles();
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

  @Nonnull
  @Override
  public List<PluginListenerDescriptor> getApplicationListeners() {
    return myApplicationListeners;
  }

  @Nonnull
  @Override
  public List<PluginListenerDescriptor> getProjectListeners() {
    return myProjectListeners;
  }

  @Nonnull
  @Override
  public List<PluginListenerDescriptor> getModuleListeners() {
    return myModuleListeners;
  }

  @Nullable
  @Override
  public PluginPermissionDescriptor getPermissionDescriptor(@Nonnull PluginPermissionType permissionType) {
    return myPermissionDescriptors.get(permissionType);
  }

  @Override
  public String toString() {
    return "PluginDescriptor[name='" + myName + "', classpath='" + myPath + "']";
  }

  @Override
  public boolean isDeleted() {
    return myDeleted;
  }

  public void setDeleted(boolean deleted) {
    myDeleted = deleted;
  }

  public void setLoader(ClassLoader loader) {
    myLoader = loader;
  }

  public void setModuleLayer(Object moduleLayer) {
    myModuleLayer = moduleLayer;
  }

  public Object getModuleLayer() {
    return myModuleLayer;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof PluginDescriptorImpl)) return false;

    final PluginDescriptorImpl pluginDescriptor = (PluginDescriptorImpl)o;

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
    if (myLoader == null) {
      throw new IllegalArgumentException("Do not call #getPluginClassLoader() is plugin is not loaded");
    }
    return myLoader;
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
  public Map<PluginId, PluginDescriptorImpl> getOptionalDescriptors() {
    return myOptionalDescriptors;
  }

  public void setOptionalDescriptors(final Map<PluginId, PluginDescriptorImpl> optionalDescriptors) {
    myOptionalDescriptors = optionalDescriptors;
  }

  public Boolean getSkipped() {
    return mySkipped;
  }

  public void setSkipped(final Boolean skipped) {
    mySkipped = skipped;
  }

  @Override
  public boolean isExperimental() {
    return myExperimental;
  }

  @Override
  public boolean isLoaded() {
    return true;
  }

  @Override
  public boolean isBundled() {
    return myIsPreInstalled;
  }
}
