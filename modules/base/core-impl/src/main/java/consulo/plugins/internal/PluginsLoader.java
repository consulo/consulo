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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.util.BuildNumber;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.util.SmartList;
import com.intellij.util.graph.*;
import consulo.application.ApplicationProperties;
import consulo.container.boot.ContainerPathManager;
import consulo.container.impl.*;
import consulo.container.impl.classloader.Java9ModuleInitializer;
import consulo.container.impl.classloader.PluginClassLoaderFactory;
import consulo.container.plugin.*;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.IntConsumer;

public class PluginsLoader {
  private static class LoggerHolder {
    private static final Logger ourLogger = Logger.getInstance(PluginsLoader.class);
  }

  public static final ContainerLogger C_LOG = new ContainerLogger() {
    @Override
    public void info(String message) {
      getLogger().info(message);
    }

    @Override
    public void warn(String message) {
      getLogger().warn(message);
    }

    @Override
    public void info(String message, Throwable t) {
      getLogger().info(message, t);
    }

    @Override
    public void error(String message, Throwable t) {
      getLogger().error(message, t);
    }
  };

  static final float PLUGINS_PROGRESS_MAX_VALUE = 0.3f;

  public static void setVersionChecker() {
    PluginValidator.VALIDATOR = new PluginDescriptorVersionValidator() {
      @Override
      public boolean validateVersion(@Nonnull PluginDescriptor descriptor) {
        return !isIncompatible(descriptor);
      }

      private boolean isIncompatible(final PluginDescriptor descriptor) {
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
    };
  }

  @Nonnull
  public static PluginsInitializeInfo initPlugins(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    long start = System.currentTimeMillis();
    PluginsInitializeInfo info;
    try {
      info = initializePlugins(progress, isHeadlessMode);
    }
    catch (RuntimeException e) {
      getLogger().error(e);
      throw e;
    }
    int pluginsCount = PluginManager.getPluginsCount();
    getLogger().info(pluginsCount + " plugins initialized in " + (System.currentTimeMillis() - start) + " ms");
    logPlugins();
    return info;
  }

  @Nonnull
  static PluginsInitializeInfo initializePlugins(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    PluginsInitializeInfo info = new PluginsInitializeInfo();

    List<PluginDescriptorImpl> pluginDescriptors = loadPluginDescriptors(progress, isHeadlessMode);

    final ClassLoader parentLoader = Application.class.getClassLoader();

    final List<PluginDescriptorImpl> result = new ArrayList<>();
    final Map<String, String> disabledPluginNames = new HashMap<>();
    List<String> brokenPluginsList = new SmartList<>();
    for (PluginDescriptorImpl descriptor : pluginDescriptors) {
      PluginManager.PluginSkipReason pluginSkipReason = PluginManager.calcPluginSkipReason(descriptor);
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

    String badPluginMessage = filterBadPlugins(info, result, disabledPluginNames);
    if (badPluginMessage != null) {
      problemsWithPlugins.add(badPluginMessage);
    }

    final Map<PluginId, PluginDescriptorImpl> idToDescriptorMap = new HashMap<>();
    for (PluginDescriptorImpl descriptor : result) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

    mergeOptionalConfigs(idToDescriptorMap);

    final Graph<PluginId> graph = createPluginIdGraph(idToDescriptorMap);
    final DFSTBuilder<PluginId> builder = new DFSTBuilder<>(graph);
    if (!builder.isAcyclic()) {
      final String cyclePresentation;
      if (ApplicationProperties.isInSandbox()) {
        final List<String> cycles = new ArrayList<>();
        builder.getSCCs().forEach(new IntConsumer() {
          int myTNumber = 0;

          @Override
          public void accept(int size) {
            if (size > 1) {
              String cycle = "";
              for (int j = 0; j < size; j++) {
                cycle += builder.getNodeByTNumber(myTNumber + j).getIdString() + " ";
              }
              cycles.add(cycle);
            }
            myTNumber += size;
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

    prepareLoadingPluginsErrorMessage(info, problemsWithPlugins, isHeadlessMode);

    final Comparator<PluginId> idComparator = builder.comparator();
    // sort descriptors according to plugin dependencies
    Collections.sort(result, (o1, o2) -> idComparator.compare(o1.getPluginId(), o2.getPluginId()));

    Map<PluginId, Integer> id2Index = new HashMap<>();
    for (int i = 0; i < result.size(); i++) {
      id2Index.put(result.get(i).getPluginId(), i);
    }

    PluginHolderModificator.setPluginLoadOrder(id2Index);

    int i = 0;
    for (final PluginDescriptorImpl pluginDescriptor : result) {
      // platform plugin already have classloader
      if (!PluginIds.isPlatformPlugin(pluginDescriptor.getPluginId())) {
        final List<File> classPath = pluginDescriptor.getClassPath();
        final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
        final ClassLoader[] parentLoaders = getParentLoaders(idToDescriptorMap, dependentPluginIds);

        final ClassLoader pluginClassLoader =
                createPluginClassLoader(classPath.toArray(new File[classPath.size()]), parentLoaders.length > 0 ? parentLoaders : new ClassLoader[]{parentLoader}, pluginDescriptor);

        if (SystemInfo.IS_AT_LEAST_JAVA9) {
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

    return info;
  }

  static void prepareLoadingPluginsErrorMessage(PluginsInitializeInfo info, final List<String> problems, boolean isHeadlessMode) {
    if (!isHeadlessMode) {
      info.addPluginErrors(problems);
    }
    else {
      for (String problem : problems) {
        getLogger().error(problem);
      }
    }
  }

  @Nonnull
  private static List<PluginDescriptorImpl> loadPluginDescriptors(@Nullable StartupProgress progress, boolean isHeadlessMode) {
    StatCollector stat = new StatCollector();

    List<PluginDescriptorImpl> pluginDescriptors = new ArrayList<>();
    for (PluginDescriptor descriptor : PluginManager.getPlugins()) {
      pluginDescriptors.add((PluginDescriptorImpl)descriptor);
    }

    pluginDescriptors.addAll(loadDescriptorsFromPluginsPath(progress, isHeadlessMode, stat));

    final Map<PluginId, PluginDescriptorImpl> idToDescriptorMap = new HashMap<>();

    for (PluginDescriptorImpl descriptor : pluginDescriptors) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

    Collections.sort(pluginDescriptors, getPluginDescriptorComparator(idToDescriptorMap));

    stat.dump("Plugins load", getLogger()::info);
    return pluginDescriptors;
  }

  @Nullable
  public static PluginDescriptor loadPluginDescriptor(File file) {
    return PluginDescriptorLoader.loadDescriptor(file, false, false, PluginsLoader.C_LOG);
  }

  @Nonnull
  public static List<PluginDescriptorImpl> loadDescriptorsFromPluginsPath(@Nullable StartupProgress progress, boolean isHeadlessMode, StatCollector stat) {
    final List<PluginDescriptorImpl> result = new ArrayList<>();

    int pluginsCount = 0;
    String[] pluginsPaths = ContainerPathManager.get().getPluginsPaths();
    for (String pluginsPath : pluginsPaths) {
      pluginsCount += countPlugins(pluginsPath);
    }

    for (String pluginsPath : pluginsPaths) {
      loadDescriptors(pluginsPath, result, progress, pluginsCount, isHeadlessMode, stat, false);
    }

    return result;
  }

  public static void loadDescriptors(String pluginsPath,
                                     List<PluginDescriptorImpl> result,
                                     @Nullable StartupProgress progress,
                                     int pluginsCount,
                                     boolean isHeadlessMode,
                                     StatCollector stat,
                                     boolean isPreInstalledPath) {
    loadDescriptors(new File(pluginsPath), result, progress, pluginsCount, stat, isHeadlessMode, isPreInstalledPath);
  }

  public static void loadDescriptors(@Nonnull File pluginsHome,
                                     List<PluginDescriptorImpl> result,
                                     @Nullable StartupProgress progress,
                                     int pluginsCount,
                                     StatCollector stat,
                                     boolean isHeadlessMode,
                                     boolean isPreInstalledPath) {
    final File[] files = pluginsHome.listFiles();
    if (files != null) {
      int i = result.size();
      for (File file : files) {
        Runnable mark = stat.mark(file.getName());
        final PluginDescriptorImpl descriptor = PluginDescriptorLoader.loadDescriptor(file, isHeadlessMode, isPreInstalledPath, C_LOG);
        if (descriptor == null) {
          mark.run();
          continue;
        }

        if (progress != null) {
          progress.showProgress(descriptor.getName(), PLUGINS_PROGRESS_MAX_VALUE * ((float)++i / pluginsCount));
        }
        int oldIndex = result.indexOf(descriptor);
        if (oldIndex >= 0) {
          final PluginDescriptorImpl oldDescriptor = result.get(oldIndex);
          if (StringUtil.compareVersionNumbers(oldDescriptor.getVersion(), descriptor.getVersion()) < 0) {
            result.set(oldIndex, descriptor);
          }
        }
        else {
          result.add(descriptor);
        }
        mark.run();
      }
    }
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

  @Nullable
  static String filterBadPlugins(PluginsInitializeInfo info, List<? extends PluginDescriptor> result, final Map<String, String> disabledPluginNames) {
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
      PluginManager.checkDependants(pluginDescriptor, idToDescriptorMap::get, pluginId -> {
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

          message.append(PluginManager.getDisabledPlugins().contains(pluginId.getIdString())
                         ? IdeBundle.message("error.required.plugin.disabled", name, pluginName)
                         : IdeBundle.message("error.required.plugin.not.installed", name, pluginName));
          it.remove();
          return false;
        }
        return true;
      });
    }
    if (!disabledPluginIds.isEmpty()) {
      info.setPluginsForDisable(disabledPluginIds);
      info.setPluginsForEnable(faultyDescriptors);

      message.append("<br>");
      message.append("<br>").append("<a href=\"" + PluginsInitializeInfo.DISABLE + "\">Disable ");
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
        message.append("<br>").append("<a href=\"" + PluginsInitializeInfo.ENABLE + "\">Enable ")
                .append(faultyDescriptors.size() == 1 ? disabledPluginNames.get(faultyDescriptors.iterator().next()) : " all necessary plugins").append("</a>");
      }
      message.append("<br>").append("<a href=\"" + PluginsInitializeInfo.EDIT + "\">Open plugin manager</a>");
    }
    if (message.length() > 0) {
      return message.toString();
    }
    return null;
  }

  static void mergeOptionalConfigs(Map<PluginId, PluginDescriptorImpl> descriptors) {
    final Map<PluginId, PluginDescriptorImpl> descriptorsWithModules = new HashMap<>(descriptors);
    for (PluginDescriptorImpl descriptor : descriptors.values()) {
      final Map<PluginId, PluginDescriptorImpl> optionalDescriptors = descriptor.getOptionalDescriptors();
      if (optionalDescriptors != null && !optionalDescriptors.isEmpty()) {
        for (Map.Entry<PluginId, PluginDescriptorImpl> entry : optionalDescriptors.entrySet()) {
          if (descriptorsWithModules.containsKey(entry.getKey())) {
            descriptor.mergeOptionalConfig(entry.getValue());
          }
        }
      }
    }
  }

  static Comparator<PluginDescriptor> getPluginDescriptorComparator(Map<PluginId, PluginDescriptorImpl> idToDescriptorMap) {
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

  private static Graph<PluginId> createPluginIdGraph(final Map<PluginId, PluginDescriptorImpl> idToDescriptorMap) {
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

  @Nonnull
  static List<Object> getParentModuleLayer(Map<PluginId, ? extends PluginDescriptor> idToDescriptorMap, PluginId[] pluginIds) {
    final List<Object> result = new ArrayList<>();
    for (final PluginId id : pluginIds) {
      PluginDescriptor pluginDescriptor = idToDescriptorMap.get(id);
      if (pluginDescriptor == null) {
        continue; // Might be an optional dependency
      }

      if (pluginDescriptor instanceof PluginDescriptorImpl) {
        result.add(((PluginDescriptorImpl)pluginDescriptor).getModuleLayer());
      }
    }
    return result;
  }

  static void initClassLoader(@Nonnull ClassLoader parentLoader, @Nonnull PluginDescriptorImpl descriptor) {
    final List<File> classPath = descriptor.getClassPath();
    final ClassLoader loader = createPluginClassLoader(classPath.toArray(new File[classPath.size()]), new ClassLoader[]{parentLoader}, descriptor);
    descriptor.setLoader(loader);
  }

  @Nullable
  static ClassLoader createPluginClassLoader(@Nonnull File[] classPath, @Nonnull ClassLoader[] parentLoaders, @Nonnull PluginDescriptor pluginDescriptor) {
    try {
      final List<URL> urls = new ArrayList<>(classPath.length);
      for (File aClassPath : classPath) {
        final File file = aClassPath.getCanonicalFile(); // it is critical not to have "." and ".." in classpath elements
        urls.add(file.toURI().toURL());
      }
      return PluginClassLoaderFactory.create(urls, parentLoaders, pluginDescriptor);
    }
    catch (IOException e) {
      getLogger().error(e);
    }
    return null;
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

  public static Logger getLogger() {
    return LoggerHolder.ourLogger;
  }
}
