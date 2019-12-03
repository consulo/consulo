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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.ExtensionPoint;
import consulo.container.plugin.PluginId;
import com.intellij.openapi.extensions.impl.ExtensionAreaId;
import com.intellij.openapi.extensions.impl.ExtensionsAreaImpl;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfoRt;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.Function;
import com.intellij.util.ReflectionUtil;
import com.intellij.util.SmartList;
import com.intellij.util.graph.*;
import consulo.annotation.DeprecationInfo;
import consulo.application.ApplicationProperties;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.impl.ContainerLogger;
import consulo.container.impl.IdeaPluginDescriptorImpl;
import consulo.container.impl.PluginHolderModificator;
import consulo.container.impl.PluginLoader;
import consulo.container.impl.classloader.Java9ModuleInitializer;
import consulo.container.impl.classloader.PluginClassLoaderFactory;
import consulo.container.impl.classloader.PluginLoadStatistics;
import consulo.container.impl.parser.ExtensionInfo;
import consulo.container.plugin.IdeaPluginDescriptor;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginIds;
import consulo.container.plugin.PluginManager;
import consulo.logging.Logger;
import consulo.util.nodep.map.SimpleMultiMap;
import consulo.util.nodep.xml.node.SimpleXmlElement;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import gnu.trove.TIntProcedure;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.*;
import java.net.URL;
import java.util.*;

public class PluginManagerCore {
  public static final ContainerLogger C_LOG = new ContainerLogger() {
    @Override
    public void info(String message) {
      Logger.getInstance(PluginManagerCore.class).info(message);
    }

    @Override
    public void warn(String message) {
      Logger.getInstance(PluginManagerCore.class).warn(message);
    }

    @Override
    public void info(String message, Throwable t) {
      Logger.getInstance(PluginManagerCore.class).info(message, t);
    }

    @Override
    public void error(String message, Throwable t) {
      Logger.getInstance(PluginManagerCore.class).error(message, t);
    }
  };

  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance(PluginManagerCore.class);
  }

  @NonNls
  public static final String DISABLED_PLUGINS_FILENAME = "disabled_plugins.txt";
  @NonNls
  public static final String CORE_PLUGIN_ID = "com.intellij";
  @NonNls
  public static final PluginId CORE_PLUGIN = PluginIds.CONSULO_PLATFORM_BASE;

  public static final float PLUGINS_PROGRESS_MAX_VALUE = 0.3f;

  static final String DISABLE = "disable";
  static final String ENABLE = "enable";
  static final String EDIT = "edit";

  static List<String> ourDisabledPlugins = null;
  static List<String> ourPluginErrors = null;
  static List<String> myPlugins2Disable = null;
  static LinkedHashSet<String> myPlugins2Enable = null;

  @Nonnull
  @Deprecated
  @DeprecationInfo("Use consulo.container.plugin.PluginManager#getPlugins()")
  @SuppressWarnings("deprecation")
  public static IdeaPluginDescriptor[] getPlugins() {
    List<PluginDescriptor> plugins = PluginManager.getPlugins();
    IdeaPluginDescriptor[] array = new IdeaPluginDescriptor[plugins.size()];
    for (int i = 0; i < plugins.size(); i++) {
      PluginDescriptor pluginDescriptor = plugins.get(i);
      array[i] = (IdeaPluginDescriptor)pluginDescriptor;
    }
    return array;
  }

  static boolean isUnitTestMode() {
    final Application app = ApplicationManager.getApplication();
    return app != null && app.isUnitTestMode();
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

  @Nonnull
  public static List<String> getDisabledPlugins() {
    if (ourDisabledPlugins == null) {
      ourDisabledPlugins = new ArrayList<>();
      if (System.getProperty("consulo.ignore.disabled.plugins") == null && !isUnitTestMode()) {
        loadDisabledPlugins(PathManager.getConfigPath(), ourDisabledPlugins);
      }
    }
    return ourDisabledPlugins;
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

  public static void checkDependants(final PluginDescriptor pluginDescriptor, final Function<PluginId, PluginDescriptor> pluginId2Descriptor, final Condition<PluginId> check) {
    checkDependants(pluginDescriptor, pluginId2Descriptor, check, new HashSet<>());
  }

  private static boolean checkDependants(final PluginDescriptor pluginDescriptor,
                                         final Function<PluginId, PluginDescriptor> pluginId2Descriptor,
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
        final PluginDescriptor dependantPluginDescriptor = pluginId2Descriptor.fun(dependentPluginId);
        if (dependantPluginDescriptor != null && !checkDependants(dependantPluginDescriptor, pluginId2Descriptor, check, processed)) {
          return false;
        }
      }
    }
    return true;
  }


  @Nullable
  public static PluginId getPluginByClassName(@Nonnull String className) {
    if (className.startsWith("java.") || className.startsWith("javax.") || className.startsWith("kotlin.") || className.startsWith("groovy.")) {
      return null;
    }

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      if (hasLoadedClass(className, descriptor.getPluginClassLoader())) {
        PluginId id = descriptor.getPluginId();
        return CORE_PLUGIN_ID.equals(id.getIdString()) ? null : id;
      }
    }
    return null;
  }

  private static boolean hasLoadedClass(@Nonnull String className, ClassLoader loader) {
    if (loader instanceof PluginClassLoader) return ((PluginClassLoader)loader).hasLoadedClass(className);

    // it can be an UrlClassLoader loaded by another class loader, so instanceof doesn't work
    try {
      return (Boolean)loader.getClass().getMethod("hasLoadedClass", String.class).invoke(loader, className);
    }
    catch (Exception e) {
      return false;
    }
  }

  @Nullable
  public static PluginId getPluginId(@Nonnull Class<?> clazz) {
    ClassLoader loader = clazz.getClassLoader();
    if (!(loader instanceof PluginClassLoader)) {
      return null;
    }
    return ((PluginClassLoader)loader).getPluginId();
  }

  @Nullable
  static ClassLoader createPluginClassLoader(@Nonnull File[] classPath, @Nonnull ClassLoader[] parentLoaders, @Nonnull PluginDescriptor pluginDescriptor) {
    PluginId pluginId = pluginDescriptor.getPluginId();
    File pluginRoot = pluginDescriptor.getPath();

    try {
      final List<URL> urls = new ArrayList<>(classPath.length);
      for (File aClassPath : classPath) {
        final File file = aClassPath.getCanonicalFile(); // it is critical not to have "." and ".." in classpath elements
        urls.add(file.toURI().toURL());
      }
      return PluginClassLoaderFactory.create(urls, parentLoaders, pluginId, pluginDescriptor.getVersion(), pluginRoot);
    }
    catch (IOException e) {
      getLogger().error(e);
    }
    return null;
  }

  public static boolean isPluginClass(String className) {
    return PluginManager.isInitialized() && getPluginByClassName(className) != null;
  }

  static void logPlugins() {
    List<String> platform = new ArrayList<>();
    List<String> disabled = new ArrayList<>();
    List<String> loadedCustom = new ArrayList<>();

    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      final String version = descriptor.getVersion();
      String s = descriptor.getName() + (version != null ? " (" + version + ")" : "");
      if (descriptor.isEnabled()) {
        if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
          platform.add(s);
        }
        else {
          loadedCustom.add(s);
        }
      }
      else {
        disabled.add(s);
      }
    }

    Collections.sort(platform);
    Collections.sort(loadedCustom);
    Collections.sort(disabled);

    getLogger().info("Loaded platform plugins: " + StringUtil.join(platform, ", "));
    if (!loadedCustom.isEmpty()) {
      getLogger().info("Loaded custom plugins: " + StringUtil.join(loadedCustom, ", "));
    }
    if (!disabled.isEmpty()) {
      getLogger().info("Disabled plugins: " + StringUtil.join(disabled, ", "));
    }
  }

  @Nonnull
  static ClassLoader[] getParentLoaders(Map<PluginId, ? extends PluginDescriptor> idToDescriptorMap, PluginId[] pluginIds) {
    final List<ClassLoader> classLoaders = new ArrayList<>();
    for (final PluginId id : pluginIds) {
      PluginDescriptor pluginDescriptor = idToDescriptorMap.get(id);
      if (pluginDescriptor == null) {
        continue; // Might be an optional dependency
      }

      final ClassLoader loader = pluginDescriptor.getPluginClassLoader();
      classLoaders.add(loader);
    }
    return classLoaders.toArray(new ClassLoader[classLoaders.size()]);
  }

  @Nonnull
  static List<Object> getParentModuleLayer(Map<PluginId, ? extends PluginDescriptor> idToDescriptorMap, PluginId[] pluginIds) {
    final List<Object> result = new ArrayList<>();
    for (final PluginId id : pluginIds) {
      PluginDescriptor pluginDescriptor = idToDescriptorMap.get(id);
      if (pluginDescriptor == null) {
        continue; // Might be an optional dependency
      }

      if (pluginDescriptor instanceof IdeaPluginDescriptorImpl) {
        result.add(((IdeaPluginDescriptorImpl)pluginDescriptor).getModuleLayer());
      }
    }
    return result;
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

  static void prepareLoadingPluginsErrorMessage(final List<String> problems, boolean isHeadlessMode) {
    if (!isHeadlessMode) {
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

  static Comparator<PluginDescriptor> getPluginDescriptorComparator(Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap) {
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
    return GraphGenerator.generate(CachingSemiGraph.cache(new InboundSemiGraph<PluginId>() {
      @Override
      public Collection<PluginId> getNodes() {
        return ids;
      }

      @Override
      public Iterator<PluginId> getIn(PluginId pluginId) {
        final PluginDescriptor descriptor = idToDescriptorMap.get(pluginId);
        List<PluginId> plugins = new ArrayList<>();
        for (PluginId dependentPluginId : descriptor.getDependentPluginIds()) {
          // check for missing optional dependency
          PluginDescriptor dep = idToDescriptorMap.get(dependentPluginId);
          if (dep != null) {
            //if 'dep' refers to a module we need to add the real plugin containing this module only if it's still enabled, otherwise the graph will be inconsistent
            PluginId realPluginId = dep.getPluginId();
            if (idToDescriptorMap.containsKey(realPluginId)) {
              plugins.add(realPluginId);
            }
          }
        }
        return plugins.iterator();
      }
    }));
  }


  public static void loadDescriptors(String pluginsPath,
                                     List<IdeaPluginDescriptorImpl> result,
                                     @Nullable StartupProgress progress,
                                     int pluginsCount,
                                     boolean isHeadlessMode,
                                     boolean isPreInstalledPath) {
    loadDescriptors(new File(pluginsPath), result, progress, pluginsCount, isHeadlessMode, isPreInstalledPath);
  }

  public static void loadDescriptors(@Nonnull File pluginsHome,
                                     List<IdeaPluginDescriptorImpl> result,
                                     @Nullable StartupProgress progress,
                                     int pluginsCount,
                                     boolean isHeadlessMode,
                                     boolean isPreInstalledPath) {
    final File[] files = pluginsHome.listFiles();
    if (files != null) {
      int i = result.size();
      for (File file : files) {
        final IdeaPluginDescriptorImpl descriptor = PluginLoader.loadDescriptor(file, isHeadlessMode, isPreInstalledPath, C_LOG);
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
  static String filterBadPlugins(List<? extends PluginDescriptor> result, final Map<String, String> disabledPluginNames) {
    final Map<PluginId, PluginDescriptor> idToDescriptorMap = new HashMap<>();
    final StringBuilder message = new StringBuilder();
    for (Iterator<? extends PluginDescriptor> it = result.iterator(); it.hasNext(); ) {
      final PluginDescriptor descriptor = it.next();
      final PluginId id = descriptor.getPluginId();

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
    for (final Iterator<? extends PluginDescriptor> it = result.iterator(); it.hasNext(); ) {
      final PluginDescriptor pluginDescriptor = it.next();
      checkDependants(pluginDescriptor, idToDescriptorMap::get, pluginId -> {
        if (!idToDescriptorMap.containsKey(pluginId)) {
          pluginDescriptor.setEnabled(false);
          faultyDescriptors.add(pluginId.getIdString());
          disabledPluginIds.add(pluginDescriptor.getPluginId().getIdString());
          message.append("<br>");
          final String name = pluginDescriptor.getName();
          final PluginDescriptor descriptor = idToDescriptorMap.get(pluginId);
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
    if (message.length() > 0) {
      return message.toString();
    }
    return null;
  }

  @Nonnull
  public static List<IdeaPluginDescriptorImpl> loadDescriptorsFromPluginPath(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    final List<IdeaPluginDescriptorImpl> result = new ArrayList<>();

    int pluginsCount = 0;
    String[] pluginsPaths = PathManager.getPluginsPaths();
    for (String pluginsPath : pluginsPaths) {
      pluginsCount += countPlugins(pluginsPath);
    }

    for (String pluginsPath : pluginsPaths) {
      loadDescriptors(pluginsPath, result, progress, pluginsCount, isHeadlessMode, false);
    }

    return result;
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

  public static void initClassLoader(@Nonnull ClassLoader parentLoader, @Nonnull IdeaPluginDescriptorImpl descriptor) {
    final List<File> classPath = descriptor.getClassPath();
    final ClassLoader loader = createPluginClassLoader(classPath.toArray(new File[classPath.size()]), new ClassLoader[]{parentLoader}, descriptor);
    descriptor.setLoader(loader);
  }

  static boolean shouldSkipPlugin0(final PluginDescriptor descriptor) {
    return calcPluginSkipReason(descriptor) != PluginSkipReason.NO;
  }

  enum PluginSkipReason {
    NO,
    DISABLED,
    INCOMPATIBLE,
    DEPENDENCY_IS_NOT_RESOLVED
  }

  @Nonnull
  static PluginSkipReason calcPluginSkipReason(final PluginDescriptor descriptor) {
    final String idString = descriptor.getPluginId().getIdString();
    if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
      return PluginSkipReason.NO;
    }

    if (!descriptor.isEnabled()) return PluginSkipReason.DISABLED;

    boolean shouldLoad = !getDisabledPlugins().contains(idString);
    if (shouldLoad && descriptor instanceof IdeaPluginDescriptorImpl) {
      if (isIncompatible(descriptor)) return PluginSkipReason.INCOMPATIBLE;
    }

    return !shouldLoad ? PluginSkipReason.DEPENDENCY_IS_NOT_RESOLVED : PluginSkipReason.NO;
  }

  public static boolean isIncompatible(final PluginDescriptor descriptor) {
    String platformVersion = descriptor.getPlatformVersion();
    if (StringUtil.isEmpty(platformVersion)) {
      return false;
    }

    try {
      BuildNumber buildNumber = ApplicationInfo.getInstance().getBuild();
      BuildNumber pluginBuildNumber = BuildNumber.fromString(platformVersion);
      return !buildNumber.isSnapshot() && !pluginBuildNumber.isSnapshot() && !buildNumber.equals(pluginBuildNumber);
    }
    catch (RuntimeException ignored) {
    }

    return false;
  }

  public static void markAsDeletedPlugin(PluginDescriptor descriptor) {
    if(descriptor instanceof IdeaPluginDescriptorImpl) {
      ((IdeaPluginDescriptorImpl)descriptor).setDeleted(true);
    }
  }

  @Nullable
  public static PluginDescriptor loadPluginDescriptor(File file) {
    return PluginLoader.loadDescriptor(file, false, false, PluginManagerCore.C_LOG);
  }

  public static void dumpPluginClassStatistics(Logger logger) {
    PluginLoadStatistics.get().dumpPluginClassStatistics(logger::info);
  }

  public static boolean shouldSkipPlugin(final PluginDescriptor descriptor) {
    if (descriptor instanceof IdeaPluginDescriptorImpl) {
      IdeaPluginDescriptorImpl descriptorImpl = (IdeaPluginDescriptorImpl)descriptor;
      Boolean skipped = descriptorImpl.getSkipped();
      if (skipped != null) {
        return skipped;
      }
      boolean result = shouldSkipPlugin0(descriptor);
      descriptorImpl.setSkipped(result);
      return result;
    }
    return shouldSkipPlugin0(descriptor);
  }

  @Nonnull
  private static List<IdeaPluginDescriptorImpl> loadPluginDescriptors(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    List<IdeaPluginDescriptorImpl> pluginDescriptors = new ArrayList<>();
    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      pluginDescriptors.add((IdeaPluginDescriptorImpl)descriptor);
    }

    pluginDescriptors.addAll(loadDescriptorsFromPluginPath(progress, isHeadlessMode));

    final Map<PluginId, IdeaPluginDescriptorImpl> idToDescriptorMap = new THashMap<>();

    for (IdeaPluginDescriptorImpl descriptor : pluginDescriptors) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

    Collections.sort(pluginDescriptors, getPluginDescriptorComparator(idToDescriptorMap));

    return pluginDescriptors;
  }

  static void initializePlugins(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    List<IdeaPluginDescriptorImpl> pluginDescriptors = loadPluginDescriptors(progress, isHeadlessMode);

    final Class callerClass = ReflectionUtil.findCallerClass(1);
    assert callerClass != null;
    final ClassLoader parentLoader = callerClass.getClassLoader();

    final List<IdeaPluginDescriptorImpl> result = new ArrayList<>();
    final Map<String, String> disabledPluginNames = new THashMap<>();
    List<String> brokenPluginsList = new SmartList<>();
    for (IdeaPluginDescriptorImpl descriptor : pluginDescriptors) {
      PluginSkipReason pluginSkipReason = calcPluginSkipReason(descriptor);
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
      if (ApplicationProperties.isInSandbox()) {
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

    prepareLoadingPluginsErrorMessage(problemsWithPlugins, isHeadlessMode);

    final Comparator<PluginId> idComparator = builder.comparator();
    // sort descriptors according to plugin dependencies
    Collections.sort(result, (o1, o2) -> idComparator.compare(o1.getPluginId(), o2.getPluginId()));

    Map<PluginId, Integer> id2Index = new HashMap<>();
    for (int i = 0; i < result.size(); i++) {
      id2Index.put(result.get(i).getPluginId(), i);
    }

    PluginHolderModificator.setPluginLoadOrder(id2Index);

    int i = 0;
    for (final IdeaPluginDescriptorImpl pluginDescriptor : result) {
      // platform plugin already have classloader
      if (!PluginIds.isPlatformPlugin(pluginDescriptor.getPluginId())) {
        final List<File> classPath = pluginDescriptor.getClassPath();
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final ClassLoader[] parentLoaders = getParentLoaders(idToDescriptorMap, dependentPluginIds);

        final ClassLoader pluginClassLoader =
                createPluginClassLoader(classPath.toArray(new File[classPath.size()]), parentLoaders.length > 0 ? parentLoaders : new ClassLoader[]{parentLoader}, pluginDescriptor);

        if (SystemInfoRt.IS_AT_LEAST_JAVA9) {
          List<Object> parentModuleLayer = getParentModuleLayer(idToDescriptorMap, dependentPluginIds);

          pluginDescriptor.setModuleLayer(Java9ModuleInitializer.initializeEtcModules(parentModuleLayer, pluginDescriptor.getClassPath(), pluginClassLoader));
        }

        pluginDescriptor.setLoader(pluginClassLoader);
      }

      if (progress != null) {
        progress.showProgress("", PLUGINS_PROGRESS_MAX_VALUE + (i++ / (float)result.size()) * 0.35f);
      }
    }

    PluginHolderModificator.initialize(pluginDescriptors);
  }

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
      registerExtensionPoints(((IdeaPluginDescriptorImpl)descriptor), areaId, area);
    }

    ExtensionPoint[] extensionPoints = area.getExtensionPoints();
    Set<String> epNames = new THashSet<>(extensionPoints.length);
    for (ExtensionPoint point : extensionPoints) {
      epNames.add(point.getName());
    }

    for (PluginDescriptor descriptor : pluginDescriptors) {
      IdeaPluginDescriptorImpl pluginDescriptor = (IdeaPluginDescriptorImpl)descriptor;

      SimpleMultiMap<String, ExtensionInfo> extensions = pluginDescriptor.getExtensions();

      for (String epName : epNames) {
        Collection<ExtensionInfo> extensionInfos = extensions.get(epName);
        for (ExtensionInfo extensionInfo : extensionInfos) {
          area.registerExtension(pluginDescriptor, extensionInfo);
        }
      }
    }
  }

  private static void registerExtensionPoints(IdeaPluginDescriptorImpl pluginDescriptor, ExtensionAreaId areaId, @Nonnull ExtensionsAreaImpl area) {
    SimpleMultiMap<String, SimpleXmlElement> extensionsPoints = pluginDescriptor.getExtensionsPoints();

    Collection<SimpleXmlElement> extensionPoints = extensionsPoints.get(areaId.name());

    for (SimpleXmlElement element : extensionPoints) {
      area.registerExtensionPoint(pluginDescriptor, element);
    }

    String oldAreaId = "CONSULO_" + areaId;
    Collection<SimpleXmlElement> oldExtensionPoints = extensionsPoints.get(oldAreaId);
    for (SimpleXmlElement oldElement : oldExtensionPoints) {
      getLogger().warn("Using old area id " + oldAreaId + ": " + oldElement);

      area.registerExtensionPoint(pluginDescriptor, oldElement);
    }
  }

  public static void initPlugins(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    long start = System.currentTimeMillis();
    try {
      initializePlugins(progress, isHeadlessMode);
    }
    catch (RuntimeException e) {
      getLogger().error(e);
      throw e;
    }
    int pluginsCount = PluginManager.getPluginsCount();
    getLogger().info(pluginsCount + " plugins initialized in " + (System.currentTimeMillis() - start) + " ms");
    logPlugins();
  }

  public static boolean isSystemPlugin(@Nonnull PluginId pluginId) {
    return PluginIds.isPlatformPlugin(pluginId);
  }
}
