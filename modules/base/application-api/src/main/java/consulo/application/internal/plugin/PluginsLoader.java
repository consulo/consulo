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
package consulo.application.internal.plugin;

import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.internal.ApplicationInfo;
import consulo.application.internal.StartupProgress;
import consulo.application.localize.ApplicationLocalize;
import consulo.component.util.BuildNumber;
import consulo.component.util.graph.DFSTBuilder;
import consulo.component.util.graph.Graph;
import consulo.component.util.graph.GraphGenerator;
import consulo.component.util.graph.InboundSemiGraph;
import consulo.container.boot.ContainerPathManager;
import consulo.container.internal.ContainerLogger;
import consulo.container.internal.PluginValidator;
import consulo.container.internal.plugin.PluginDescriptorImpl;
import consulo.container.internal.plugin.PluginDescriptorLoader;
import consulo.container.internal.plugin.PluginHolderModificator;
import consulo.container.internal.plugin.classloader.Java9ModuleInitializer;
import consulo.container.internal.plugin.classloader.PluginClassLoaderFactory;
import consulo.container.plugin.*;
import consulo.container.util.StatCollector;
import consulo.logging.Logger;
import consulo.util.collection.SmartList;
import consulo.util.lang.Couple;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
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
    final Map<PluginId, String> disabledPluginNames = new HashMap<>();
    List<String> brokenPluginsList = new SmartList<>();

    for (PluginDescriptorImpl descriptor : pluginDescriptors) {
      // platform plugins not controlled by user, always enabled
      if (PluginIds.isPlatformPlugin(descriptor.getPluginId())) {
        result.add(descriptor);
        continue;
      }

      if (PluginValidator.isIncompatible(descriptor)) {
        descriptor.setStatus(PluginDescriptorStatus.WRONG_PLATFORM_VERSION);

        brokenPluginsList.add(descriptor.getName());
      }
      else {
        result.add(descriptor);
      }
    }

    List<CompositeMessage> problemsWithPlugins = new SmartList<>();
    if (!brokenPluginsList.isEmpty()) {
      problemsWithPlugins.add(new CompositeMessage().append("Following plugins are incompatible with current IDE build: " + StringUtil.join(
        brokenPluginsList,
        ", ")));
    }

    CompositeMessage badPluginMessage = filterBadPlugins(info, result, disabledPluginNames);
    if (badPluginMessage != null) {
      problemsWithPlugins.add(badPluginMessage);
    }

    final Map<PluginId, PluginDescriptorImpl> idToDescriptorMap = new HashMap<>();
    for (PluginDescriptorImpl descriptor : result) {
      idToDescriptorMap.put(descriptor.getPluginId(), descriptor);
    }

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
      problemsWithPlugins.add(new CompositeMessage().append(ApplicationLocalize.errorPluginsShouldNotHaveCyclicDependencies(
        cyclePresentation)));
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
      if (PluginIds.isPlatformPlugin(pluginDescriptor.getPluginId())) {
        continue;
      }
      else {
        try {
          final PluginId[] dependentPluginIds = pluginDescriptor.getDependentPluginIds();
          final ClassLoader[] parentLoaders = getParentLoaders(idToDescriptorMap, dependentPluginIds);

          final ClassLoader pluginClassLoader = createPluginClassLoader(idToDescriptorMap.keySet(),
                                                                        parentLoaders.length > 0 ? parentLoaders : new ClassLoader[]{parentLoader},
                                                                        pluginDescriptor);

          if (System.getProperty("jdk.module.path") != null) {
            List<ModuleLayer> parentModuleLayer = getParentModuleLayer(idToDescriptorMap, dependentPluginIds);

            pluginDescriptor.setModuleLayer(Java9ModuleInitializer.initializeEtcModules(parentModuleLayer,
                                                                                        pluginDescriptor.getClassPathFiles(idToDescriptorMap.keySet()),
                                                                                        pluginClassLoader));
          }

          pluginDescriptor.setLoader(pluginClassLoader);
        }
        catch (Throwable e) {
          pluginDescriptor.setStatus(PluginDescriptorStatus.ERROR_WHILE_LOADING);

          getLogger().error(e);
        }
      }

      if (progress != null) {
        progress.showProgress("", PLUGINS_PROGRESS_MAX_VALUE + (i++ / (float)result.size()) * 0.35f);
      }
    }

    PluginHolderModificator.initialize(pluginDescriptors);

    return info;
  }

  static void prepareLoadingPluginsErrorMessage(PluginsInitializeInfo info, final List<CompositeMessage> problems, boolean isHeadlessMode) {
    if (!isHeadlessMode) {
      info.addPluginErrors(problems);
    }
    else {
      for (CompositeMessage problem : problems) {
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
    return PluginDescriptorLoader.loadDescriptor(file, false, PluginsLoader.C_LOG);
  }

  @Nonnull
  public static List<PluginDescriptorImpl> loadDescriptorsFromPluginsPath(@Nullable StartupProgress progress,
                                                                          boolean isHeadlessMode,
                                                                          StatCollector stat) {
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
        final PluginDescriptorImpl descriptor = PluginDescriptorLoader.loadDescriptor(file, isPreInstalledPath, C_LOG);
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
  static CompositeMessage filterBadPlugins(PluginsInitializeInfo info,
                                           List<PluginDescriptorImpl> result,
                                           final Map<PluginId, String> disabledPluginNames) {
    final Map<PluginId, PluginDescriptor> idToDescriptorMap = new HashMap<>();
    final CompositeMessage message = new CompositeMessage();
    for (Iterator<? extends PluginDescriptor> it = result.iterator(); it.hasNext(); ) {
      final PluginDescriptor descriptor = it.next();
      final PluginId id = descriptor.getPluginId();

      if (idToDescriptorMap.containsKey(id)) {
        message.append("<br>");
        message.append(ApplicationLocalize.messageDuplicatePluginId());
        message.append(id.getIdString());
        it.remove();
      }
      else if (descriptor.getStatus() == PluginDescriptorStatus.OK) {
        idToDescriptorMap.put(id, descriptor);
      }
    }

    for (final Iterator<PluginDescriptorImpl> it = result.iterator(); it.hasNext(); ) {
      final PluginDescriptorImpl pluginDescriptor = it.next();
      PluginValidator.checkDependants(pluginDescriptor, idToDescriptorMap::get, pluginId -> {
        if (!idToDescriptorMap.containsKey(pluginId)) {
          pluginDescriptor.setStatus(PluginIds.isPlatformImplementationPlugin(pluginId) ? PluginDescriptorStatus.WRONG_PLATFORM : PluginDescriptorStatus.DEPENDENCY_NOT_LOADED);

          // if dependent plugin is platform - do not show error, just disable it
          if (!PluginIds.isPlatformImplementationPlugin(pluginId)) {
            message.append("<br>");
            final String name = pluginDescriptor.getName();
            final PluginDescriptor descriptor = idToDescriptorMap.get(pluginId);

            String pluginName;
            if (descriptor == null) {
              pluginName = pluginId.getIdString();
              if (disabledPluginNames.containsKey(pluginId)) {
                pluginName = disabledPluginNames.get(pluginId);
              }
            }
            else {
              pluginName = descriptor.getName();
            }

            message.append(ApplicationLocalize.errorRequiredPluginNotInstalled(
                name,
                pluginName));
          }

          it.remove();

          return false;
        }
        return true;
      });
    }

    if (!message.isEmpty()) {
      return message;
    }
    return null;
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
    return GraphGenerator.generate(new InboundSemiGraph<PluginId>() {
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
    });
  }

  @Nonnull
  static List<ModuleLayer> getParentModuleLayer(Map<PluginId, ? extends PluginDescriptor> idToDescriptorMap, PluginId[] pluginIds) {
    final List<ModuleLayer> result = new ArrayList<>();
    for (final PluginId id : pluginIds) {
      PluginDescriptor pluginDescriptor = idToDescriptorMap.get(id);
      if (pluginDescriptor == null) {
        continue; // Might be an optional dependency
      }

      if (pluginDescriptor instanceof PluginDescriptorImpl) {
          ModuleLayer moduleLayer = pluginDescriptor.getModuleLayer();
          if (moduleLayer != null) {
              result.add(moduleLayer);
          }
      }
    }
    return result;
  }

  @Nullable
  static ClassLoader createPluginClassLoader(@Nonnull Set<PluginId> enabledPluginIds,
                                             @Nonnull ClassLoader[] parentLoaders,
                                             @Nonnull PluginDescriptor pluginDescriptor) {
    try {
      return PluginClassLoaderFactory.create(enabledPluginIds, parentLoaders, pluginDescriptor);
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
      if (pluginDescriptor == null || pluginDescriptor.getStatus() != PluginDescriptorStatus.OK) {
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
