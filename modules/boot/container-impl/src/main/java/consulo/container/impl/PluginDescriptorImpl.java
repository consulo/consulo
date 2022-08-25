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

import consulo.container.impl.parser.PluginBean;
import consulo.container.impl.parser.PluginBeanParser;
import consulo.container.impl.parser.PluginDependency;
import consulo.container.plugin.*;
import consulo.util.nodep.io.FileUtilRt;
import consulo.util.nodep.text.StringUtilRt;
import consulo.util.nodep.xml.SimpleXmlParsingException;
import consulo.util.nodep.xml.SimpleXmlReader;
import consulo.util.nodep.xml.node.SimpleXmlElement;

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

  private List<SimpleXmlElement> myActionsElements = Collections.emptyList();

  private Map<PluginPermissionType, PluginPermissionDescriptor> myPermissionDescriptors = Collections.emptyMap();

  private boolean myDeleted = false;
  private ClassLoader myLoader;
  private ModuleLayer myModuleLayer;

  private Set<String> myTags = Collections.emptySet();

  private String myDescriptionChildText;
  private boolean myEnabled = true;
  private Boolean mySkipped;
  private boolean myExperimental;

  public PluginDescriptorImpl(File pluginPath, byte[] iconBytes, byte[] darkIconBytes, boolean isPreInstalled) {
    myPath = pluginPath;
    myIconBytes = iconBytes;
    myDarkIconBytes = darkIconBytes;
    myIsPreInstalled = isPreInstalled;
  }

  @Override
  public File getPath() {
    return myPath;
  }

  public void readExternal(InputStream stream, ZipFile zipFile, ContainerLogger log) throws SimpleXmlParsingException {
    SimpleXmlElement element = SimpleXmlReader.parse(stream);
    readExternal(element, zipFile, log);
  }

  private void readExternal(SimpleXmlElement element, ZipFile zipFile, ContainerLogger log) throws SimpleXmlParsingException {
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
    if (!PluginIds.CONSULO_BASE.equals(myId)) {
      dependentPlugins.add(PluginIds.CONSULO_BASE);
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

    if (!pluginBean.tags.isEmpty()) {
      myTags = Collections.unmodifiableSet(pluginBean.tags);
    }

    if (pluginBean.experimental) {
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
  }

  private void extendPlugin(PluginBean pluginBean) {
    myActionsElements = mergeElements(myActionsElements, pluginBean.actions);
  }


  private static <T> List<T> mergeElements(List<T> original, List<T> additional) {
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
    myActionsElements = mergeElements(myActionsElements, descriptor.myActionsElements);
  }

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

  @Override
  public byte[] getIconBytes(boolean isDarkTheme) {
    if (isDarkTheme && myDarkIconBytes.length > 0) {
      return myDarkIconBytes;
    }
    return myIconBytes;
  }

  @Override
  public String getName() {
    return myName;
  }

  @Override
  public PluginId[] getDependentPluginIds() {
    return myDependencies;
  }

  @Override
  public PluginId[] getOptionalDependentPluginIds() {
    return myOptionalDependencies;
  }

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

  public List<File> getClassPath(Set<PluginId> enabledPluginIds) {
    if (myPath.isDirectory()) {
      final List<File> result = new ArrayList<File>();
      File libDir = new File(myPath, "lib");
      fillLibs(libDir, result);

      PluginId[] dependentPluginIds = getDependentPluginIds();
      // first plugin it's platform
      if (!enabledPluginIds.isEmpty() && dependentPluginIds.length > 1) {
        for (PluginId pluginId : dependentPluginIds) {
          if (PluginIds.isPlatformPlugin(pluginId)) {
            continue;
          }
          
          File pluginDepDir = new File(libDir, pluginId.getIdString());
          if (pluginDepDir.exists()) {
            fillLibs(pluginDepDir, result);
          }
        }
      }
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
  public List<SimpleXmlElement> getActionsDescriptionElements() {
    return myActionsElements;
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
  public PluginPermissionDescriptor getPermissionDescriptor(PluginPermissionType permissionType) {
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

  public void setModuleLayer(ModuleLayer moduleLayer) {
    myModuleLayer = moduleLayer;
  }

  @Override
  public ModuleLayer getModuleLayer() {
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
  public PluginId getPluginId() {
    return myId;
  }

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


  public Map<PluginId, String> getOptionalConfigs() {
    return myOptionalConfigs;
  }


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
