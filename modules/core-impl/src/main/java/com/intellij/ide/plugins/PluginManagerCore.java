/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import com.intellij.ide.ClassUtilCore;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.StartupProgress;
import com.intellij.ide.plugins.cl.PluginClassLoader;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.application.impl.ApplicationInfoImpl;
import com.intellij.openapi.components.ExtensionAreas;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.*;
import com.intellij.openapi.util.*;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.graph.CachingSemiGraph;
import com.intellij.util.graph.DFSTBuilder;
import com.intellij.util.graph.Graph;
import com.intellij.util.graph.GraphGenerator;
import com.intellij.util.lang.UrlClassLoader;
import com.intellij.util.xmlb.XmlSerializationException;
import consulo.Platform;
import consulo.application.ApplicationProperties;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntProcedure;
import org.jdom.Document;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class PluginManagerCore {
  @NonNls
  public static final String DISABLED_PLUGINS_FILENAME = "disabled_plugins.txt";
  @NonNls
  public static final String CORE_PLUGIN_ID = "com.intellij";
  @NonNls
  public static final PluginId CORE_PLUGIN = PluginId.getId(CORE_PLUGIN_ID);
  @NonNls
  public static final String UNIT_TEST_PLUGIN_ID = "consulo.unittest";
  @NonNls
  public static final PluginId UNIT_TEST_PLUGIN = PluginId.getId(UNIT_TEST_PLUGIN_ID);

  @NonNls
  public static final String META_INF = "META-INF";
  @NonNls
  public static final String PLUGIN_XML = "plugin.xml";
  public static final float PLUGINS_PROGRESS_MAX_VALUE = 0.3f;
  static final Map<PluginId, Integer> ourId2Index = new THashMap<>();
  static final PluginClassCache ourPluginClasses = new PluginClassCache();

  static final String DISABLE = "disable";
  static final String ENABLE = "enable";
  static final String EDIT = "edit";
  @NonNls
  private static final String PROPERTY_PLUGIN_PATH = "plugin.path";
  static List<String> ourDisabledPlugins = null;
  static IdeaPluginDescriptor[] ourPlugins;
  static List<String> ourPluginErrors = null;
  static List<String> myPlugins2Disable = null;
  static LinkedHashSet<String> myPlugins2Enable = null;
  public static String BUILD_NUMBER;

  /**
   * do not call this method during bootstrap, should be called in a copy of PluginManager, loaded by IdeaClassLoader
   */
  public static synchronized IdeaPluginDescriptor[] getPlugins() {
    if (ourPlugins == null) {
      initPlugins(null);
    }
    return ourPlugins;
  }

  public static void loadDisabledPlugins(final String configPath, final Collection<String> disabledPlugins) {
    final File file = new File(configPath, DISABLED_PLUGINS_FILENAME);
    if (file.isFile()) {
      try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
        String id;
        while ((id = reader.readLine()) != null) {
          disabledPlugins.add(id.trim());
        }
      }
      catch (IOException ignored) {
      }
    }
  }

  @NotNull
  public static List<String> getDisabledPlugins() {
    if (ourDisabledPlugins == null) {
      ourDisabledPlugins = new ArrayList<>();
      if (System.getProperty("idea.ignore.disabled.plugins") == null && !isUnitTestMode()) {
        loadDisabledPlugins(PathManager.getConfigPath(), ourDisabledPlugins);
      }
    }
    return ourDisabledPlugins;
  }

  static boolean isUnitTestMode() {
    final Application app = ApplicationManager.getApplication();
    return app != null && app.isUnitTestMode();
  }

  public static void savePluginsList(Collection<String> ids, boolean append, File plugins) throws IOException {
    if (!plugins.isFile()) {
      FileUtil.ensureCanCreateFile(plugins);
    }
    try (PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(plugins, append)))) {
      for (String id : ids) {
        printWriter.println(id);
      }
      printWriter.flush();
    }
  }

  public static boolean disablePlugin(String id) {
    if (getDisabledPlugins().contains(id)) return false;
    getDisabledPlugins().add(id);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static boolean enablePlugin(String id) {
    if (!getDisabledPlugins().contains(id)) return false;
    getDisabledPlugins().remove(id);
    try {
      saveDisabledPlugins(getDisabledPlugins(), false);
    }
    catch (IOException e) {
      return false;
    }
    return true;
  }

  public static void saveDisabledPlugins(Collection<String> ids, boolean append) throws IOException {
    File plugins = new File(PathManager.getConfigPath(), DISABLED_PLUGINS_FILENAME);
    savePluginsList(ids, append, plugins);
    ourDisabledPlugins = null;
  }

  public static Logger getLogger() {
    return LoggerHolder.ourLogger;
  }

  public static int getPluginLoadingOrder(PluginId id) {
    return ourId2Index.get(id);
  }

  public static void checkDependants(final IdeaPluginDescriptor pluginDescriptor,
                                     final Function<PluginId, IdeaPluginDescriptor> pluginId2Descriptor,
                                     final Condition<PluginId> check) {
    checkDependants(pluginDescriptor, pluginId2Descriptor, check, new HashSet<>());
  }

  private static boolean checkDependants(final IdeaPluginDescriptor pluginDescriptor,
                                         final Function<PluginId, IdeaPluginDescriptor> pluginId2Descriptor,
                                         final Condition<PluginId> check,
                                         final Set<PluginId> processed) {
    processed.add(pluginDescriptor.getPluginId());
    final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
    final Set<PluginId> optionalDependencies = new HashSet<>(Arrays.asList(pluginDescriptor.getOptionalDependentPluginIds()));
    for (final PluginId dependentPluginId : dependentPluginIds) {
      if (processed.contains(dependentPluginId)) continue;

      if (!optionalDependencies.contains(dependentPluginId)) {
        if (!check.value(dependentPluginId)) {
          return false;
        }
        final IdeaPluginDescriptor dependantPluginDescriptor = pluginId2Descriptor.fun(dependentPluginId);
        if (dependantPluginDescriptor != null && !checkDependants(dependantPluginDescriptor, pluginId2Descriptor, check, processed)) {
          return false;
        }
      }
    }
    return true;
  }

  public static void addPluginClass(PluginId pluginId) {
    ourPluginClasses.addPluginClass(pluginId);
  }

  @Nullable
  public static PluginId getPluginByClassName(@NotNull String className) {
    if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("kotlin.") || className.startsWith("groovy.")) {
      return null;
    }

    for (IdeaPluginDescriptor descriptor : getPlugins()) {
      if (hasLoadedClass(className, descriptor.getPluginClassLoader())) {
        PluginId id = descriptor.getPluginId();
        return CORE_PLUGIN_ID.equals(id.getIdString()) ? null : id;
      }
    }
    return null;
  }

  private static boolean hasLoadedClass(@NotNull String className, ClassLoader loader) {
    if (loader instanceof UrlClassLoader) return ((UrlClassLoader)loader).hasLoadedClass(className);

    // it can be an UrlClassLoader loaded by another class loader, so instanceof doesn't work
    try {
      return ((Boolean)loader.getClass().getMethod("hasLoadedClass", String.class).invoke(loader, className)).booleanValue();
    }
    catch (Exception e) {
      return false;
    }
  }

  @Nullable
  public static PluginId getPluginId(@NotNull Class<?> clazz) {
    ClassLoader loader = clazz.getClassLoader();
    if (!(loader instanceof PluginClassLoader)) {
      return null;
    }
    return ((PluginClassLoader)loader).getPluginId();
  }

  public static void dumpPluginClassStatistics() {
    ourPluginClasses.dumpPluginClassStatistics();
  }

  static boolean isDependent(final IdeaPluginDescriptor descriptor, final PluginId on, Map<PluginId, IdeaPluginDescriptor> map) {
    for (PluginId id : descriptor.getDependentPluginIds()) {
      if (ArrayUtil.contains(id, (Object[])descriptor.getOptionalDependentPluginIds())) {
        continue;
      }
      if (id.equals(on)) {
        return true;
      }
      final IdeaPluginDescriptor depDescriptor = map.get(id);
      if (depDescriptor != null && isDependent(depDescriptor, on, map)) {
        return true;
      }
    }
    return false;
  }


  static boolean shouldLoadPlugins() {
    try {
      // no plugins during bootstrap
      Class.forName("com.intellij.openapi.extensions.Extensions");
    }
    catch (ClassNotFoundException e) {
      return false;
    }
    //noinspection HardCodedStringLiteral
    final String loadPlugins = System.getProperty("idea.load.plugins");
    return loadPlugins == null || Boolean.TRUE.toString().equals(loadPlugins);
  }

  static void configureExtensions() {
    Extensions.setLogProvider(new IdeaLogProvider());
    Extensions.registerAreaClass(ExtensionAreas.PROJECT, null);
    Extensions.registerAreaClass(ExtensionAreas.MODULE, ExtensionAreas.PROJECT);
  }

  @Nullable
  static ClassLoader createPluginClassLoader(@NotNull File[] classPath, @NotNull ClassLoader[] parentLoaders, @NotNull IdeaPluginDescriptor pluginDescriptor) {

    PluginId pluginId = pluginDescriptor.getPluginId();
    File pluginRoot = pluginDescriptor.getPath();

    //if (classPath.length == 0) return null;
    try {
      final List<URL> urls = new ArrayList<>(classPath.length);
      for (File aClassPath : classPath) {
        final File file = aClassPath.getCanonicalFile(); // it is critical not to have "." and ".." in classpath elements
        urls.add(file.toURI().toURL());
      }
      return new PluginClassLoader(urls, parentLoaders, pluginId, pluginDescriptor.getVersion(), pluginRoot);
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  public static void invalidatePlugins() {
    ourPlugins = null;
    ourDisabledPlugins = null;
  }

  public static boolean isPluginClass(String className) {
    return ourPlugins != null && getPluginByClassName(className) != null;
  }

  static void logPlugins() {
    List<String> loadedBundled = new ArrayList<>();
    List<String> disabled = new ArrayList<>();
    List<String> loadedCustom = new ArrayList<>();

    for (IdeaPluginDescriptor descriptor : ourPlugins) {
      final String version = descriptor.getVersion();
      String s = descriptor.getName() + (version != null ? " (" + version + ")" : "");
      if (descriptor.isEnabled()) {
        if (descriptor.isBundled() || CORE_PLUGIN.equals(descriptor.getPluginId())) {
          loadedBundled.add(s);
        }
        else {
          loadedCustom.add(s);
        }
      }
      else {
        disabled.add(s);
      }
    }

    Collections.sort(loadedBundled);
    Collections.sort(loadedCustom);
    Collections.sort(disabled);

    getLogger().info("Loaded bundled plugins: " + StringUtil.join(loadedBundled, ", "));
    if (!loadedCustom.isEmpty()) {
      getLogger().info("Loaded custom plugins: " + StringUtil.join(loadedCustom, ", "));
    }
    if (!disabled.isEmpty()) {
      getLogger().info("Disabled plugins: " + StringUtil.join(disabled, ", "));
    }
  }

  static ClassLoader[] getParentLoaders(Map<PluginId, ? extends IdeaPluginDescriptor> idToDescriptorMap, PluginId[] pluginIds) {
    final List<ClassLoader> classLoaders = new ArrayList<>();
    for (final PluginId id : pluginIds) {
      IdeaPluginDescriptor pluginDescriptor = idToDescriptorMap.get(id);
      if (pluginDescriptor == null) {
        continue; // Might be an optional dependency
      }

      final ClassLoader loader = pluginDescriptor.getPluginClassLoader();
      if (loader == null) {
        getLogger().error("Plugin class loader should be initialized for plugin " + id);
      }
      classLoaders.add(loader);
    }
    return classLoaders.toArray(new ClassLoader[classLoaders.size()]);
  }

  public static int countPlugins(String pluginsPath) {
    File configuredPluginsDir = new File(pluginsPath);
    if (configuredPluginsDir.exists()) {
      String[] list = configuredPluginsDir.list();
      if (list != null) {
        return list.length;
      }
    }
    return 0;
  }

  static Collection<URL> getClassLoaderUrls() {
    final ClassLoader classLoader = PluginManagerCore.class.getClassLoader();
    final Class<? extends ClassLoader> aClass = classLoader.getClass();
    try {
      @SuppressWarnings("unchecked") List<URL> urls = (List<URL>)aClass.getMethod("getUrls").invoke(classLoader);
      return urls;
    }
    catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException ignored) {
    }

    if (classLoader instanceof URLClassLoader) {
      return Arrays.asList(((URLClassLoader)classLoader).getURLs());
    }

    return Collections.emptyList();
  }

  static void prepareLoadingPluginsErrorMessage(final List<String> problems) {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment() && !ApplicationManager.getApplication().isUnitTestMode()) {
      if (ourPluginErrors == null) {
        ourPluginErrors = new ArrayList<>(problems);
      }
      else {
        ourPluginErrors.addAll(problems);
      }
    }
    else {
      for (String problem : problems) {
        getLogger().error(problem);
      }
    }
  }

  static Comparator<IdeaPluginDescriptor> getPluginDescriptorComparator(Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap) {
    final Graph<PluginId> graph = createPluginIdGraph(idToDescriptorMap);
    final DFSTBuilder<PluginId> builder = new DFSTBuilder<>(graph);
    /*
    if (!builder.isAcyclic()) {
      final Pair<String,String> circularDependency = builder.getCircularDependency();
      throw new Exception("Cyclic dependencies between plugins are not allowed: \"" + circularDependency.getFirst() + "\" and \"" + circularDependency.getSecond() + "");
    }
    */
    final Comparator<PluginId> idComparator = builder.comparator();
    return (o1, o2) -> idComparator.compare(o1.getPluginId(), o2.getPluginId());
  }

  private static Graph<PluginId> createPluginIdGraph(final Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap) {
    final List<PluginId> ids = new ArrayList<>(idToDescriptorMap.keySet());
    // this magic ensures that the dependent plugins always follow their dependencies in lexicographic order
    // needed to make sure that extensions are always in the same order
    Collections.sort(ids, (o1, o2) -> o2.getIdString().compareTo(o1.getIdString()));
    return GraphGenerator.create(CachingSemiGraph.create(new GraphGenerator.SemiGraph<PluginId>() {
      @Override
      public Collection<PluginId> getNodes() {
        return ids;
      }

      @Override
      public Iterator<PluginId> getIn(PluginId pluginId) {
        final IdeaPluginDescriptor descriptor = idToDescriptorMap.get(pluginId);
        ArrayList<PluginId> plugins = new ArrayList<>();
        for (PluginId dependentPluginId : descriptor.getDependentPluginIds()) {
          // check for missing optional dependency
          if (idToDescriptorMap.containsKey(dependentPluginId)) {
            plugins.add(dependentPluginId);
          }
        }
        return plugins.iterator();
      }
    }));
  }

  @Nullable
  static IdeaPluginDescriptorImpl loadDescriptorFromDir(final File file, @NonNls String fileName) {
    File descriptorFile = new File(file, META_INF + File.separator + fileName);
    if (descriptorFile.exists()) {
      try {
        IdeaPluginDescriptorImpl descriptor = new IdeaPluginDescriptorImpl(file);
        descriptor.readExternal(descriptorFile.toURI().toURL());
        return descriptor;
      }
      catch (XmlSerializationException e) {
        getLogger().info("Cannot load " + file, e);
        prepareLoadingPluginsErrorMessage(Collections.singletonList("File '" + file.getName() + "' contains invalid plugin descriptor."));
      }
      catch (Throwable e) {
        getLogger().info("Cannot load " + file, e);
      }
    }

    return null;
  }

  @Nullable
  static IdeaPluginDescriptorImpl loadDescriptorFromJar(@NotNull File file, @NotNull String fileName) {
    try {
      String fileURL = StringUtil.replace(file.toURI().toASCIIString(), "!", "%21");
      URL jarURL = new URL("jar:" + fileURL + "!/META-INF/" + fileName);

      ZipFile zipFile = new ZipFile(file.getPath());
      try {
        ZipEntry entry = zipFile.getEntry("META-INF/" + fileName);
        if (entry != null) {
          Document document = JDOMUtil.loadDocument(zipFile.getInputStream(entry));
          IdeaPluginDescriptorImpl descriptor = new IdeaPluginDescriptorImpl(file);
          descriptor.readExternal(document, jarURL);
          return descriptor;
        }
      }
      finally {
        try {
          zipFile.close();
        }
        catch (IOException ignored) {
        }
      }
    }
    catch (XmlSerializationException e) {
      getLogger().info("Cannot load " + file, e);
      prepareLoadingPluginsErrorMessage(Collections.singletonList("File '" + file.getName() + "' contains invalid plugin descriptor."));
    }
    catch (Throwable e) {
      getLogger().info("Cannot load " + file, e);
    }

    return null;
  }


  @Nullable
  public static IdeaPluginDescriptorImpl loadDescriptorFromJar(File file) {
    return loadDescriptorFromJar(file, PLUGIN_XML);
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  @Nullable
  public static IdeaPluginDescriptorImpl loadDescriptor(final File file, @NonNls final String fileName) {
    IdeaPluginDescriptorImpl descriptor = null;

    if (file.isDirectory()) {
      descriptor = loadDescriptorFromDir(file, fileName);

      if (descriptor == null) {
        File libDir = new File(file, "lib");
        if (!libDir.isDirectory()) {
          return null;
        }
        final File[] files = libDir.listFiles();
        if (files == null || files.length == 0) {
          return null;
        }
        Arrays.sort(files, (o1, o2) -> {
          if (o2.getName().startsWith(file.getName())) return Integer.MAX_VALUE;
          if (o1.getName().startsWith(file.getName())) return -Integer.MAX_VALUE;
          if (o2.getName().startsWith("resources")) return -Integer.MAX_VALUE;
          if (o1.getName().startsWith("resources")) return Integer.MAX_VALUE;
          return 0;
        });
        for (final File f : files) {
          if (FileUtil.isJarOrZip(f)) {
            descriptor = loadDescriptorFromJar(f, fileName);
            if (descriptor != null) {
              descriptor.setPath(file);
              break;
            }
            //           getLogger().warn("Cannot load descriptor from " + f.getName() + "");
          }
          else if (f.isDirectory()) {
            IdeaPluginDescriptorImpl descriptor1 = loadDescriptorFromDir(f, fileName);
            if (descriptor1 != null) {
              if (descriptor != null) {
                getLogger().info("Cannot load " + file + " because two or more plugin.xml's detected");
                return null;
              }
              descriptor = descriptor1;
              descriptor.setPath(file);
            }
          }
        }
      }
    }
    else if (StringUtil.endsWithIgnoreCase(file.getName(), ".jar") && file.exists()) {
      descriptor = loadDescriptorFromJar(file, fileName);
    }

    if (descriptor != null && descriptor.getOptionalConfigs() != null && !descriptor.getOptionalConfigs().isEmpty()) {
      final Map<PluginId, IdeaPluginDescriptorImpl> descriptors = new THashMap<>(descriptor.getOptionalConfigs().size());
      for (Map.Entry<PluginId, String> entry : descriptor.getOptionalConfigs().entrySet()) {
        String optionalDescriptorName = entry.getValue();
        assert !Comparing.equal(fileName, optionalDescriptorName) : "recursive dependency: " + fileName;

        IdeaPluginDescriptorImpl optionalDescriptor = loadDescriptor(file, optionalDescriptorName);
        if (optionalDescriptor == null && !FileUtil.isJarOrZip(file)) {
          for (URL url : getClassLoaderUrls()) {
            if ("file".equals(url.getProtocol())) {
              optionalDescriptor = loadDescriptor(new File(decodeUrl(url.getFile())), optionalDescriptorName);
              if (optionalDescriptor != null) {
                break;
              }
            }
          }
        }
        if (optionalDescriptor != null) {
          descriptors.put(entry.getKey(), optionalDescriptor);
        }
        else {
          getLogger().info("Cannot find optional descriptor " + optionalDescriptorName);
        }
      }
      descriptor.setOptionalDescriptors(descriptors);
    }

    return descriptor;
  }

  public static void loadDescriptors(String pluginsPath, List<IdeaPluginDescriptorImpl> result, @Nullable StartupProgress progress, int pluginsCount) {
    loadDescriptors(new File(pluginsPath), result, progress, pluginsCount);
  }

  public static void loadDescriptors(@NotNull File pluginsHome, List<IdeaPluginDescriptorImpl> result, @Nullable StartupProgress progress, int pluginsCount) {
    final File[] files = pluginsHome.listFiles();
    if (files != null) {
      int i = result.size();
      for (File file : files) {
        final IdeaPluginDescriptorImpl descriptor = loadDescriptor(file, PLUGIN_XML);
        if (descriptor == null) continue;
        if (progress != null) {
          progress.showProgress(descriptor.getName(), PLUGINS_PROGRESS_MAX_VALUE * ((float)++i / pluginsCount));
        }
        int oldIndex = result.indexOf(descriptor);
        if (oldIndex >= 0) {
          final IdeaPluginDescriptorImpl oldDescriptor = result.get(oldIndex);
          if (StringUtil.compareVersionNumbers(oldDescriptor.getVersion(), descriptor.getVersion()) < 0) {
            result.set(oldIndex, descriptor);
          }
        }
        else {
          result.add(descriptor);
        }
      }
    }
  }

  @Nullable
  static String filterBadPlugins(List<? extends IdeaPluginDescriptor> result, final Map<String, String> disabledPluginNames) {
    final Map<PluginId, IdeaPluginDescriptor> idToDescriptorMap = new HashMap<>();
    final StringBuilder message = new StringBuilder();
    boolean pluginsWithoutIdFound = false;
    for (Iterator<? extends IdeaPluginDescriptor> it = result.iterator(); it.hasNext(); ) {
      final IdeaPluginDescriptor descriptor = it.next();
      final PluginId id = descriptor.getPluginId();
      if (id == null) {
        pluginsWithoutIdFound = true;
      }
      if (idToDescriptorMap.containsKey(id)) {
        message.append("<br>");
        message.append(IdeBundle.message("message.duplicate.plugin.id"));
        message.append(id);
        it.remove();
      }
      else if (descriptor.isEnabled()) {
        idToDescriptorMap.put(id, descriptor);
      }
    }

    final List<String> disabledPluginIds = new ArrayList<>();
    final LinkedHashSet<String> faultyDescriptors = new LinkedHashSet<>();
    for (final Iterator<? extends IdeaPluginDescriptor> it = result.iterator(); it.hasNext(); ) {
      final IdeaPluginDescriptor pluginDescriptor = it.next();
      checkDependants(pluginDescriptor, idToDescriptorMap::get, pluginId -> {
        if (!idToDescriptorMap.containsKey(pluginId)) {
          pluginDescriptor.setEnabled(false);
          faultyDescriptors.add(pluginId.getIdString());
          disabledPluginIds.add(pluginDescriptor.getPluginId().getIdString());
          message.append("<br>");
          final String name = pluginDescriptor.getName();
          final IdeaPluginDescriptor descriptor = idToDescriptorMap.get(pluginId);
          String pluginName;
          if (descriptor == null) {
            pluginName = pluginId.getIdString();
            if (disabledPluginNames.containsKey(pluginName)) {
              pluginName = disabledPluginNames.get(pluginName);
            }
          }
          else {
            pluginName = descriptor.getName();
          }

          message.append(getDisabledPlugins().contains(pluginId.getIdString())
                         ? IdeBundle.message("error.required.plugin.disabled", name, pluginName)
                         : IdeBundle.message("error.required.plugin.not.installed", name, pluginName));
          it.remove();
          return false;
        }
        return true;
      });
    }
    if (!disabledPluginIds.isEmpty()) {
      myPlugins2Disable = disabledPluginIds;
      myPlugins2Enable = faultyDescriptors;
      message.append("<br>");
      message.append("<br>").append("<a href=\"" + DISABLE + "\">Disable ");
      if (disabledPluginIds.size() == 1) {
        final PluginId pluginId2Disable = PluginId.getId(disabledPluginIds.iterator().next());
        message.append(idToDescriptorMap.containsKey(pluginId2Disable) ? idToDescriptorMap.get(pluginId2Disable).getName() : pluginId2Disable.getIdString());
      }
      else {
        message.append("not loaded plugins");
      }
      message.append("</a>");
      boolean possibleToEnable = true;
      for (String descriptor : faultyDescriptors) {
        if (disabledPluginNames.get(descriptor) == null) {
          possibleToEnable = false;
          break;
        }
      }
      if (possibleToEnable) {
        message.append("<br>").append("<a href=\"" + ENABLE + "\">Enable ")
                .append(faultyDescriptors.size() == 1 ? disabledPluginNames.get(faultyDescriptors.iterator().next()) : " all necessary plugins").append("</a>");
      }
      message.append("<br>").append("<a href=\"" + EDIT + "\">Open plugin manager</a>");
    }
    if (pluginsWithoutIdFound) {
      message.append("<br>");
      message.append(IdeBundle.message("error.plugins.without.id.found"));
    }
    if (message.length() > 0) {
      return message.toString();
    }
    return null;
  }

  @SuppressWarnings("deprecation")
  private static String decodeUrl(String file) {
    String quotePluses = StringUtil.replace(file, "+", "%2B");
    return URLDecoder.decode(quotePluses);
  }

  static void loadDescriptorsFromProperty(final List<IdeaPluginDescriptorImpl> result) {
    final String pathProperty = System.getProperty(PROPERTY_PLUGIN_PATH);
    if (pathProperty == null) return;

    for (StringTokenizer t = new StringTokenizer(pathProperty, File.pathSeparator); t.hasMoreTokens(); ) {
      String s = t.nextToken();
      final IdeaPluginDescriptorImpl ideaPluginDescriptor = loadDescriptor(new File(s), PLUGIN_XML);
      if (ideaPluginDescriptor != null) {
        result.add(ideaPluginDescriptor);
      }
    }
  }

  public static IdeaPluginDescriptorImpl[] loadDescriptors(@Nullable StartupProgress progress) {
    final List<IdeaPluginDescriptorImpl> result = new ArrayList<>();

    int pluginsCount = countPlugins(PathManager.getPluginsPath()) + countPlugins(PathManager.getPreInstalledPluginsPath());
    loadDescriptors(PathManager.getPluginsPath(), result, progress, pluginsCount);
    loadDescriptors(PathManager.getPreInstalledPluginsPath(), result, progress, pluginsCount);
    loadDescriptorsFromProperty(result);

    // insert dummy platform code
    insertPlatformPlugin(result);

    // insert consulo unit dummy plugin
    if (Boolean.getBoolean(ApplicationProperties.CONSULO_IN_UNIT_TEST)) {
      IdeaPluginDescriptorImpl pluginDescriptor = new IdeaPluginDescriptorImpl(new File(PathManager.getPreInstalledPluginsPath(), "unittest"));
      pluginDescriptor.setId(UNIT_TEST_PLUGIN);
      List<PluginId> map = ContainerUtil.map(result, IdeaPluginDescriptorImpl::getPluginId);
      pluginDescriptor.setDependencies(ContainerUtil.toArray(map, PluginId.EMPTY_ARRAY));
      result.add(pluginDescriptor);
    }

    IdeaPluginDescriptorImpl[] pluginDescriptors = result.toArray(new IdeaPluginDescriptorImpl[result.size()]);
    final Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap = new THashMap<>();
    for (IdeaPluginDescriptorImpl descriptor : pluginDescriptors) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

    Arrays.sort(pluginDescriptors, getPluginDescriptorComparator(idToDescriptorMap));
    return pluginDescriptors;
  }

  private static void insertPlatformPlugin(List<IdeaPluginDescriptorImpl> result) {
    Platform platform = Platform.get();

    PluginId pluginId = platform.getPluginId();

    IdeaPluginDescriptorImpl pluginDescriptor = new IdeaPluginDescriptorImpl(new File(PathManager.getPreInstalledPluginsPath(), pluginId.getIdString()));
    pluginDescriptor.setName(StringUtil.capitalize(platform.name().toLowerCase(Locale.US)));
    pluginDescriptor.setId(pluginId);

    result.add(pluginDescriptor);
  }

  static void mergeOptionalConfigs(Map<PluginId, IdeaPluginDescriptorImpl> descriptors) {
    final Map<PluginId, IdeaPluginDescriptorImpl> descriptorsWithModules = new THashMap<>(descriptors);
    for (IdeaPluginDescriptorImpl descriptor : descriptors.values()) {
      final Map<PluginId, IdeaPluginDescriptorImpl> optionalDescriptors = descriptor.getOptionalDescriptors();
      if (optionalDescriptors != null && !optionalDescriptors.isEmpty()) {
        for (Map.Entry<PluginId, IdeaPluginDescriptorImpl> entry : optionalDescriptors.entrySet()) {
          if (descriptorsWithModules.containsKey(entry.getKey())) {
            descriptor.mergeOptionalConfig(entry.getValue());
          }
        }
      }
    }
  }

  public static void initClassLoader(@NotNull ClassLoader parentLoader, @NotNull IdeaPluginDescriptorImpl descriptor) {
    final List<File> classPath = descriptor.getClassPath();
    final ClassLoader loader = createPluginClassLoader(classPath.toArray(new File[classPath.size()]), new ClassLoader[]{parentLoader}, descriptor);
    descriptor.setLoader(loader);
  }

  static boolean shouldSkipPlugin(final IdeaPluginDescriptor descriptor, IdeaPluginDescriptor[] loaded) {
    return calcPluginSkipReason(descriptor, loaded) != PluginSkipReason.NO;
  }

  enum PluginSkipReason {
    NO,
    DISABLED,
    INCOMPATIBLE,
    DEPENDENCY_IS_NOT_RESOLVED
  }

  static PluginSkipReason calcPluginSkipReason(final IdeaPluginDescriptor descriptor, IdeaPluginDescriptor[] loaded) {
    final String idString = descriptor.getPluginId().getIdString();
    if (descriptor.getPluginId().equals(CORE_PLUGIN)) {
      return PluginSkipReason.NO;
    }

    //noinspection HardCodedStringLiteral
    final String pluginId = System.getProperty("idea.load.plugins.id");
    if (pluginId == null) {
      if (descriptor instanceof IdeaPluginDescriptorImpl && !descriptor.isEnabled()) return PluginSkipReason.DISABLED;

      if (!shouldLoadPlugins()) return PluginSkipReason.DISABLED;
    }
    final List<String> pluginIds = pluginId == null ? null : StringUtil.split(pluginId, ",");


    boolean shouldLoad;
    //noinspection HardCodedStringLiteral
    final String loadPluginCategory = System.getProperty("idea.load.plugins.category");
    if (loadPluginCategory != null) {
      shouldLoad = loadPluginCategory.equals(descriptor.getCategory());
    }
    else {
      if (pluginIds != null) {
        shouldLoad = pluginIds.contains(idString);
        if (!shouldLoad) {
          Map<PluginId, IdeaPluginDescriptor> map = new HashMap<>();
          for (final IdeaPluginDescriptor pluginDescriptor : loaded) {
            map.put(pluginDescriptor.getPluginId(), pluginDescriptor);
          }
          final IdeaPluginDescriptor descriptorFromProperty = map.get(PluginId.getId(pluginId));
          shouldLoad = descriptorFromProperty != null && isDependent(descriptorFromProperty, descriptor.getPluginId(), map);
        }
      }
      else {
        shouldLoad = !getDisabledPlugins().contains(idString);
      }
      if (shouldLoad && descriptor instanceof IdeaPluginDescriptorImpl) {
        if (isIncompatible(descriptor)) return PluginSkipReason.INCOMPATIBLE;
      }
    }

    return !shouldLoad ? PluginSkipReason.DEPENDENCY_IS_NOT_RESOLVED : PluginSkipReason.NO;
  }

  public static boolean isIncompatible(final IdeaPluginDescriptor descriptor) {
    String platformVersion = descriptor.getPlatformVersion();
    if (StringUtil.isEmpty(platformVersion)) {
      return false;
    }

    try {
      BuildNumber buildNumber = ApplicationInfoImpl.getShadowInstance().getBuild();
      BuildNumber pluginBuildNumber = BuildNumber.fromString(platformVersion);
      return !buildNumber.isSnapshot() && !pluginBuildNumber.isSnapshot() && !buildNumber.equals(pluginBuildNumber);
    }
    catch (RuntimeException ignored) {
    }

    return false;
  }

  public static boolean shouldSkipPlugin(final IdeaPluginDescriptor descriptor) {
    if (descriptor instanceof IdeaPluginDescriptorImpl) {
      IdeaPluginDescriptorImpl descriptorImpl = (IdeaPluginDescriptorImpl)descriptor;
      Boolean skipped = descriptorImpl.getSkipped();
      if (skipped != null) {
        return skipped;
      }
      boolean result = shouldSkipPlugin(descriptor, ourPlugins);
      descriptorImpl.setSkipped(result);
      return result;
    }
    return shouldSkipPlugin(descriptor, ourPlugins);
  }

  static void initializePlugins(@Nullable StartupProgress progress) {
    configureExtensions();

    final IdeaPluginDescriptorImpl[] pluginDescriptors = loadDescriptors(progress);

    final Class callerClass = ReflectionUtil.findCallerClass(1);
    assert callerClass != null;
    final ClassLoader parentLoader = callerClass.getClassLoader();

    final List<IdeaPluginDescriptorImpl> result = new ArrayList<>();
    final Map<String, String> disabledPluginNames = new THashMap<>();
    List<String> brokenPluginsList = new SmartList<>();
    for (IdeaPluginDescriptorImpl descriptor : pluginDescriptors) {
      PluginSkipReason pluginSkipReason = calcPluginSkipReason(descriptor, pluginDescriptors);
      switch (pluginSkipReason) {
        case NO:
          result.add(descriptor);
          break;
        case INCOMPATIBLE:
          brokenPluginsList.add(descriptor.getName());
        default:
          descriptor.setEnabled(false);
          disabledPluginNames.put(descriptor.getPluginId().getIdString(), descriptor.getName());
          initClassLoader(parentLoader, descriptor);
          break;
      }
    }

    List<String> problemsWithPlugins = new SmartList<>();
    if (!brokenPluginsList.isEmpty()) {
      problemsWithPlugins.add("Following plugins are incompatible with current IDE build: " + StringUtil.join(brokenPluginsList, ", "));
    }

    String badPluginMessage = filterBadPlugins(result, disabledPluginNames);
    if (badPluginMessage != null) {
      problemsWithPlugins.add(badPluginMessage);
    }

    final Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap = new THashMap<>();
    for (IdeaPluginDescriptorImpl descriptor : result) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

    mergeOptionalConfigs(idToDescriptorMap);

    final Graph<PluginId> graph = createPluginIdGraph(idToDescriptorMap);
    final DFSTBuilder<PluginId> builder = new DFSTBuilder<>(graph);
    if (!builder.isAcyclic()) {
      final String cyclePresentation;
      if (ApplicationManager.getApplication().isInternal()) {
        final List<String> cycles = new ArrayList<>();
        builder.getSCCs().forEach(new TIntProcedure() {
          int myTNumber = 0;

          @Override
          public boolean execute(int size) {
            if (size > 1) {
              String cycle = "";
              for (int j = 0; j < size; j++) {
                cycle += builder.getNodeByTNumber(myTNumber + j).getIdString() + " ";
              }
              cycles.add(cycle);
            }
            myTNumber += size;
            return true;
          }
        });
        cyclePresentation = ": " + StringUtil.join(cycles, ";");
      }
      else {
        final Couple<PluginId> circularDependency = builder.getCircularDependency();
        final PluginId id = circularDependency.getFirst();
        final PluginId parentId = circularDependency.getSecond();
        cyclePresentation = id + "->" + parentId + "->...->" + id;
      }
      problemsWithPlugins.add(IdeBundle.message("error.plugins.should.not.have.cyclic.dependencies", cyclePresentation));
    }

    prepareLoadingPluginsErrorMessage(problemsWithPlugins);

    final Comparator<PluginId> idComparator = builder.comparator();
    // sort descriptors according to plugin dependencies
    Collections.sort(result, (o1, o2) -> idComparator.compare(o1.getPluginId(), o2.getPluginId()));

    for (int i = 0; i < result.size(); i++) {
      ourId2Index.put(result.get(i).getPluginId(), i);
    }

    int i = 0;
    for (final IdeaPluginDescriptorImpl pluginDescriptor : result) {

      final List<File> classPath = pluginDescriptor.getClassPath();
      final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
      final ClassLoader[] parentLoaders = getParentLoaders(idToDescriptorMap, dependentPluginIds);

      final ClassLoader pluginClassLoader =
              createPluginClassLoader(classPath.toArray(new File[classPath.size()]), parentLoaders.length > 0 ? parentLoaders : new ClassLoader[]{parentLoader},
                                      pluginDescriptor);
      pluginDescriptor.setLoader(pluginClassLoader);

      if (progress != null) {
        progress.showProgress("", PLUGINS_PROGRESS_MAX_VALUE + (i++ / (float)result.size()) * 0.35f);
      }
    }

    registerExtensionPointsAndExtensions(Extensions.getRootArea(), result);
    Extensions.getRootArea().getExtensionPoint(Extensions.AREA_LISTENER_EXTENSION_POINT).registerExtension(new AreaListener() {
      @Override
      public void areaCreated(@NotNull String areaClass, @NotNull AreaInstance areaInstance) {
        registerExtensionPointsAndExtensions(Extensions.getArea(areaInstance), result);
      }

      @Override
      public void areaDisposing(@NotNull String areaClass, @NotNull AreaInstance areaInstance) {
      }
    });


    ourPlugins = pluginDescriptors;
  }

  private static void registerExtensionPointsAndExtensions(ExtensionsArea area, List<IdeaPluginDescriptorImpl> loadedPlugins) {
    for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
      descriptor.registerExtensionPoints(area);
    }

    ExtensionPoint[] extensionPoints = area.getExtensionPoints();
    Set<String> epNames = new THashSet<>(extensionPoints.length);
    for (ExtensionPoint point : extensionPoints) {
      epNames.add(point.getName());
    }

    for (IdeaPluginDescriptorImpl descriptor : loadedPlugins) {
      for (String epName : epNames) {
        descriptor.registerExtensions(area, epName);
      }
    }
  }

  public static void initPlugins(@Nullable StartupProgress progress) {
    long start = System.currentTimeMillis();
    try {
      initializePlugins(progress);
    }
    catch (RuntimeException e) {
      getLogger().error(e);
      throw e;
    }
    getLogger().info(ourPlugins.length + " plugins initialized in " + (System.currentTimeMillis() - start) + " ms");
    logPlugins();
    ClassUtilCore.clearJarURLCache();
  }

  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance("#com.intellij.ide.plugins.PluginManager");
  }

  private static class IdeaLogProvider implements LogProvider {
    @Override
    public void error(String message) {
      getLogger().error(message);
    }

    @Override
    public void error(String message, Throwable t) {
      getLogger().error(message, t);
    }

    @Override
    public void error(Throwable t) {
      getLogger().error(t);
    }

    @Override
    public void warn(String message) {
      getLogger().info(message);
    }

    @Override
    public void warn(String message, Throwable t) {
      getLogger().info(message, t);
    }

    @Override
    public void warn(Throwable t) {
      getLogger().info(t);
    }
  }
}
