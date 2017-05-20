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

import com.intellij.AbstractBundle;
import com.intellij.CommonBundle;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.components.ComponentConfig;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMUtil;
import com.intellij.openapi.util.NullableLazyValue;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.containers.StringInterner;
import com.intellij.util.xmlb.JDOMXIncluder;
import com.intellij.util.xmlb.XmlSerializer;
import consulo.Platform;
import gnu.trove.THashMap;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.*;

/**
 * @author mike
 */
public class IdeaPluginDescriptorImpl implements IdeaPluginDescriptor {
  public static final IdeaPluginDescriptorImpl[] EMPTY_ARRAY = new IdeaPluginDescriptorImpl[0];
  private static final Logger LOG = Logger.getInstance("#com.intellij.ide.plugins.PluginDescriptor");
  private final NullableLazyValue<String> myDescription = new NullableLazyValue<String>() {
    @Override
    protected String compute() {
      return computeDescription();
    }
  };

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
  private File myPath;
  private PluginId[] myDependencies = PluginId.EMPTY_ARRAY;
  private PluginId[] myOptionalDependencies = PluginId.EMPTY_ARRAY;
  private Map<PluginId, String> myOptionalConfigs;
  private Map<PluginId, IdeaPluginDescriptorImpl> myOptionalDescriptors;
  @Nullable
  private List<Element> myActionsElements;
  private ComponentConfig[] myAppComponents = ComponentConfig.EMPTY_ARRAY;
  private ComponentConfig[] myProjectComponents = ComponentConfig.EMPTY_ARRAY;
  private ComponentConfig[] myModuleComponents = ComponentConfig.EMPTY_ARRAY;
  private boolean myDeleted = false;
  private ClassLoader myLoader;
  private HelpSetPath[] myHelpSets;
  @Nullable
  private MultiMap<String, Element> myExtensions;
  @Nullable
  private MultiMap<String, Element> myExtensionsPoints;
  private String myDescriptionChildText;
  private String myDownloadCounter;
  private long myDate;
  private boolean myEnabled = true;
  private Boolean mySkipped;
  private List<String> myModules = null;

  public IdeaPluginDescriptorImpl(@NotNull File pluginPath) {
    myPath = pluginPath;
  }

  @Nullable
  private static List<Element> copyElements(@Nullable Element[] elements, StringInterner interner) {
    if (elements == null || elements.length == 0) {
      return null;
    }

    List<Element> result = new SmartList<>();
    for (Element extensionsRoot : elements) {
      for (Element element : extensionsRoot.getChildren()) {
        JDOMUtil.internElement(element, interner);
        result.add(element);
      }
    }
    return result;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private static String createDescriptionKey(final PluginId id) {
    return "plugin." + id + ".description";
  }

  private static ComponentConfig[] mergeComponents(ComponentConfig[] first, ComponentConfig[] second) {
    if (first == null) {
      return second;
    }
    if (second == null) {
      return first;
    }
    return ArrayUtil.mergeArrays(first, second);
  }

  @Override
  public File getPath() {
    return myPath;
  }

  public void setPath(@NotNull File path) {
    myPath = path;
  }

  public void setName(String name) {
    myName = name;
  }

  public void readExternal(@NotNull Document document, @NotNull URL url) throws InvalidDataException, FileNotFoundException {
    Application application = ApplicationManager.getApplication();
    readExternal(document, url, application != null && application.isUnitTestMode());
  }

  public void readExternal(@NotNull Document document, @NotNull URL url, boolean ignoreMissingInclude) throws InvalidDataException, FileNotFoundException {
    document = JDOMXIncluder.resolve(document, url.toExternalForm(), ignoreMissingInclude);
    Element rootElement = document.getRootElement();
    JDOMUtil.internElement(rootElement, new StringInterner());
    readExternal(document.getRootElement());
  }

  public void readExternal(@NotNull URL url) throws InvalidDataException, FileNotFoundException {
    try {
      Document document = JDOMUtil.loadDocument(url);
      readExternal(document, url);
    }
    catch (FileNotFoundException e) {
      throw e;
    }
    catch (IOException | JDOMException e) {
      throw new InvalidDataException(e);
    }
  }

  private void readExternal(@NotNull Element element) {
    final PluginBean pluginBean = XmlSerializer.deserialize(element, PluginBean.class);
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
    Set<PluginId> dependentPlugins = new LinkedHashSet<>();
    // we always depend to core plugin, but prevent recursion
    if (!PluginManagerCore.CORE_PLUGIN.equals(myId)) {
      dependentPlugins.add(PluginManagerCore.CORE_PLUGIN);
    }
    Set<PluginId> optionalDependentPlugins = new LinkedHashSet<>();
    if (pluginBean.dependencies != null) {
      myOptionalConfigs = new THashMap<>();
      for (PluginDependency dependency : pluginBean.dependencies) {
        String text = dependency.pluginId;
        if (!StringUtil.isEmpty(text)) {
          PluginId id = PluginId.getId(text);
          dependentPlugins.add(id);
          if (dependency.optional) {
            optionalDependentPlugins.add(id);
            if (!StringUtil.isEmpty(dependency.configFile)) {
              myOptionalConfigs.put(id, dependency.configFile);
            }
          }
        }
      }
    }

    myDependencies = dependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : dependentPlugins.toArray(new PluginId[dependentPlugins.size()]);
    myOptionalDependencies =
            optionalDependentPlugins.isEmpty() ? PluginId.EMPTY_ARRAY : optionalDependentPlugins.toArray(new PluginId[optionalDependentPlugins.size()]);

    if (pluginBean.helpSets == null || pluginBean.helpSets.length == 0) {
      myHelpSets = HelpSetPath.EMPTY;
    }
    else {
      myHelpSets = new HelpSetPath[pluginBean.helpSets.length];
      PluginHelpSet[] sets = pluginBean.helpSets;
      for (int i = 0, n = sets.length; i < n; i++) {
        PluginHelpSet pluginHelpSet = sets[i];
        myHelpSets[i] = new HelpSetPath(pluginHelpSet.file, pluginHelpSet.path);
      }
    }

    myAppComponents = pluginBean.applicationComponents;
    myProjectComponents = pluginBean.projectComponents;
    myModuleComponents = pluginBean.moduleComponents;

    if (myAppComponents == null) myAppComponents = ComponentConfig.EMPTY_ARRAY;
    if (myProjectComponents == null) myProjectComponents = ComponentConfig.EMPTY_ARRAY;
    if (myModuleComponents == null) myModuleComponents = ComponentConfig.EMPTY_ARRAY;

    StringInterner interner = new StringInterner();
    List<Element> extensions = copyElements(pluginBean.extensions, interner);
    if (extensions != null) {
      myExtensions = MultiMap.createSmart();
      for (Element extension : extensions) {
        myExtensions.putValue(ExtensionsAreaImpl.extractEPName(extension), extension);
      }
    }

    List<Element> extensionPoints = copyElements(pluginBean.extensionPoints, interner);
    if (extensionPoints != null) {
      myExtensionsPoints = MultiMap.createSmart();
      for (Element extensionPoint : extensionPoints) {
        String areaId = StringUtil.notNullize(extensionPoint.getAttributeValue(ExtensionsAreaImpl.ATTRIBUTE_AREA), ExtensionAreas.APPLICATION);
        if ("CONSULO_PROJECT".equals(areaId)) {
          LOG.warn("Deprecated areaId '" + areaId + "' for extensionPoint: " + buildEpId(extensionPoint));
          areaId = ExtensionAreas.PROJECT;
        }
        else if ("CONSULO_MODULE".equals(areaId)) {
          LOG.warn("Deprecated areaId '" + areaId + "' for extensionPoint: " + buildEpId(extensionPoint));
          areaId = ExtensionAreas.MODULE;
        }

        if (!ArrayUtil.contains(areaId, ExtensionAreas.ourAllowedAres)) {
          LOG.error("Bad areaId for extensionPoint: " + buildEpId(extensionPoint));
          continue;
        }

        myExtensionsPoints.putValue(areaId, extensionPoint);
      }
    }

    myActionsElements = copyElements(pluginBean.actions, interner);
  }

  private String buildEpId(Element element) {
    final String pluginId = getPluginId().getIdString();
    String epName = element.getAttributeValue("qualifiedName");
    if (epName == null) {
      final String name = element.getAttributeValue("name");
      if (name == null) {
        throw new RuntimeException("'name' attribute not specified for extension point in '" + pluginId + "' plugin");
      }
      epName = pluginId + '.' + name;
    }
    return epName;
  }

  public void registerExtensionPoints(@NotNull ExtensionsArea area) {
    if (myExtensionsPoints != null) {
      for (Element element : myExtensionsPoints.get(StringUtil.notNullize(area.getAreaClass()))) {
        area.registerExtensionPoint(this, element);
      }
    }
  }

  void registerExtensions(@NotNull ExtensionsArea area, @NotNull String epName) {
    if (myExtensions != null) {
      for (Element element : myExtensions.get(epName)) {
        area.registerExtension(this, element);
      }
    }
  }

  @Override
  public String getDescription() {
    return myDescription.getValue();
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
  @NotNull
  public PluginId[] getDependentPluginIds() {
    return myDependencies;
  }

  @Override
  @NotNull
  public PluginId[] getOptionalDependentPluginIds() {
    return myOptionalDependencies;
  }

  @Override
  public String getVendor() {
    return myVendor;
  }

  public void setVendor(final String val) {
    myVendor = val;
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

  @SuppressWarnings("UnusedDeclaration")
  @Nullable
  public MultiMap<String, Element> getExtensionsPoints() {
    return myExtensionsPoints;
  }

  @SuppressWarnings("UnusedDeclaration")
  @Nullable
  public MultiMap<String, Element> getExtensions() {
    return myExtensions;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @NotNull
  public List<File> getClassPath() {
    if (getPluginId().equals(Platform.get().getPluginId())) {
      return Collections.emptyList();
    }

    // special hack for unit test loader
    if (PluginManagerCore.UNIT_TEST_PLUGIN.equals(getPluginId())) {
      final List<File> result = new ArrayList<>();
      String testClasspath = System.getProperty("consulo.test.classpath");
      if (!StringUtil.isEmpty(testClasspath)) {
        List<String> paths = StringUtil.split(testClasspath, ";");
        for (String path : paths) {
          result.add(new File(path));
        }
      }
      return result;
    }

    if (myPath.isDirectory()) {
      final List<File> result = new ArrayList<>();
      final File classesDir = new File(myPath, "classes");

      if (classesDir.exists()) {
        result.add(classesDir);
      }

      final File[] files = new File(myPath, "lib").listFiles();
      if (files != null && files.length > 0) {
        for (final File f : files) {
          if (f.isFile()) {
            final String name = f.getName();
            if (StringUtil.endsWithIgnoreCase(name, ".jar") || StringUtil.endsWithIgnoreCase(name, ".zip")) {
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
  @Nullable
  public List<Element> getActionsDescriptionElements() {
    return myActionsElements;
  }

  @Override
  @NotNull
  public ComponentConfig[] getAppComponents() {
    return myAppComponents;
  }

  @Override
  @NotNull
  public ComponentConfig[] getProjectComponents() {
    return myProjectComponents;
  }

  @Override
  @NotNull
  public ComponentConfig[] getModuleComponents() {
    return myModuleComponents;
  }

  @Override
  public String getVendorEmail() {
    return myVendorEmail;
  }

  public void setVendorEmail(final String val) {
    myVendorEmail = val;
  }

  @Override
  public String getVendorUrl() {
    return myVendorUrl;
  }

  public void setVendorUrl(final String val) {
    myVendorUrl = val;
  }

  @Override
  public String getUrl() {
    return url;
  }

  public void setUrl(final String val) {
    url = val;
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
  @NotNull
  public HelpSetPath[] getHelpSets() {
    return myHelpSets;
  }

  @Override
  public PluginId getPluginId() {
    return myId;
  }

  /*
     This setter was explicitly defined to be able to set downloads count for a
     descriptor outside its loading from the xml file since this information
     is available only from the site.
  */
  public void setDownloadsCount(String downloadsCount) {
    myDownloadCounter = downloadsCount;
  }

  @Override
  public String getDownloads() {
    return myDownloadCounter;
  }

  public long getDate() {
    return myDate;
  }

  /*
     This setter was explicitly defined to be able to set date for a
     descriptor outside its loading from the xml file since this information
     is available only from the site.
  */
  public void setDate(long date) {
    myDate = date;
  }

  @Override
  public ClassLoader getPluginClassLoader() {
    return myLoader != null ? myLoader : getClass().getClassLoader();
  }

  private String computeDescription() {
    ResourceBundle bundle = null;
    if (myResourceBundleBaseName != null) {
      try {
        bundle = AbstractBundle.getResourceBundle(myResourceBundleBaseName, getPluginClassLoader());
      }
      catch (MissingResourceException e) {
        LOG.info("Cannot find plugin " + myId + " resource-bundle: " + myResourceBundleBaseName);
      }
    }

    if (bundle == null) {
      return myDescriptionChildText;
    }

    return CommonBundle.messageOrDefault(bundle, createDescriptionKey(myId), myDescriptionChildText == null ? "" : myDescriptionChildText);
  }

  public void setId(PluginId id) {
    myId = id;
  }

  public void setDependencies(PluginId[] dependencies) {
    myDependencies = dependencies;
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
  Map<PluginId, String> getOptionalConfigs() {
    return myOptionalConfigs;
  }

  @Nullable
  Map<PluginId, IdeaPluginDescriptorImpl> getOptionalDescriptors() {
    return myOptionalDescriptors;
  }

  void setOptionalDescriptors(final Map<PluginId, IdeaPluginDescriptorImpl> optionalDescriptors) {
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
    myModuleComponents = mergeComponents(myModuleComponents, descriptor.myModuleComponents);
  }

  public Boolean getSkipped() {
    return mySkipped;
  }

  public void setSkipped(final Boolean skipped) {
    mySkipped = skipped;
  }

  @Override
  public boolean isBundled() {
    String path;
    try {
      //to avoid paths like this /home/kb/IDEA/bin/../config/plugins/APlugin
      path = getPath().getCanonicalPath();
    }
    catch (IOException e) {
      path = getPath().getAbsolutePath();
    }
    return path.startsWith(PathManager.getPreInstalledPluginsPath());
  }

  @Nullable
  public List<String> getModules() {
    return myModules;
  }
}
