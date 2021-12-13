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
package com.intellij.idea;

import com.intellij.ide.StartupProgress;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.application.ex.ApplicationEx;
import consulo.application.TransactionGuardEx;
import consulo.container.boot.ContainerPathManager;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.ide.eap.EarlyAccessProgramManager;
import consulo.localize.LocalizeManager;
import consulo.localize.impl.LocalizeManagerImpl;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.plugins.internal.ConsuloSecurityManagerEnabler;
import consulo.plugins.internal.PluginsInitializeInfo;
import consulo.plugins.internal.PluginsLoader;
import consulo.start.CommandLineArgs;
import consulo.startup.PluginPermissionEarlyAccessProgramDescriptor;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.util.io.URLUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public abstract class ApplicationStarter {
  private static final Logger LOG = Logger.getInstance(ApplicationStarter.class);

  private static ApplicationStarter ourInstance;

  public static ApplicationStarter getInstance() {
    return ourInstance;
  }

  public static boolean isLoaded() {
    return ApplicationStarterCore.isLoaded();
  }

  private final CommandLineArgs myArgs;
  private boolean myPerformProjectLoad = true;

  protected final SimpleReference<StartupProgress> mySplashRef = SimpleReference.create();

  protected final Platform myPlatform;

  protected PluginsInitializeInfo myPluginsInitializeInfo;

  public ApplicationStarter(@Nonnull CommandLineArgs args, @Nonnull StatCollector stat) {
    LOG.assertTrue(ourInstance == null);
    //noinspection AssignmentToStaticFieldFromInstanceMethod
    ourInstance = this;

    myArgs = args;

    myPlatform = Platform.current();

    initApplication(false, args, stat);
  }

  @Nonnull
  protected abstract Application createApplication(boolean isHeadlessMode, SimpleReference<StartupProgress> splashRef, CommandLineArgs args);

  protected abstract void main(StatCollector stat, Runnable appInitializeMark, ApplicationEx app, boolean newConfigFolder, @Nonnull CommandLineArgs args);

  public boolean needStartInTransaction() {
    return false;
  }

  @Nullable
  public abstract StartupProgress createSplash(CommandLineArgs args);

  protected void initApplication(boolean isHeadlessMode, CommandLineArgs args, StatCollector stat) {
    StartupProgress splash = createSplash(args);
    if (splash != null) {
      mySplashRef.set(splash);
    }

    PluginsLoader.setVersionChecker();

    myPluginsInitializeInfo = PluginsLoader.initPlugins(splash, isHeadlessMode);

    StatCollector libraryStats = new StatCollector();
    LocalizeManagerImpl localizeManager = (LocalizeManagerImpl)LocalizeManager.get();
    BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager)IconLibraryManager.get();

    Map<String, List<String>> filesWithMarkers = new HashMap<>();

    libraryStats.markWith("library.analyze", () -> analyzeLibraries(filesWithMarkers));

    libraryStats.markWith("localize.initialize", () -> localizeManager.initialize(filesWithMarkers.get(LocalizeManagerImpl.LOCALIZE_LIBRARY_MARKER)));
    libraryStats.markWith("icon.initialize", () -> iconLibraryManager.initialize(filesWithMarkers.get(BaseIconLibraryManager.ICON_LIBRARY_MARKER)));

    libraryStats.dump("Libraries", LOG::info);
    
    createApplication(isHeadlessMode, mySplashRef, args);
  }

  protected void analyzeLibraries(Map<String, List<String>> filesWithMarkers) {
    List<PluginDescriptor> pluginDescriptors = PluginManager.getPlugins();
    for (PluginDescriptor pluginDescriptor : pluginDescriptors) {
      if (PluginManager.shouldSkipPlugin(pluginDescriptor)) {
        continue;
      }

      searchMarkerInClassLoaderMarker(pluginDescriptor, filesWithMarkers, LocalizeManagerImpl.LOCALIZE_LIBRARY_MARKER);
      searchMarkerInClassLoaderMarker(pluginDescriptor, filesWithMarkers, BaseIconLibraryManager.ICON_LIBRARY_MARKER);
    }
  }

  private void searchMarkerInClassLoaderMarker(PluginDescriptor pluginDescriptor, Map<String, List<String>> filesWithMarkers, String marker) {

    try {
      ClassLoader classLoader = pluginDescriptor.getPluginClassLoader();

      Enumeration<URL> ownResources = ((PluginClassLoader)classLoader).findOwnResources(marker);

      while (ownResources.hasMoreElements()) {
        URL url = ownResources.nextElement();

        Pair<String, String> urlFileInfo = URLUtil.splitJarUrl(url.getFile());
        if (urlFileInfo == null) {
          continue;
        }

        filesWithMarkers.computeIfAbsent(marker, it -> new ArrayList<>()).add(urlFileInfo.getFirst());
      }
    }
    catch (IOException e) {
      LOG.error(e);
    }
  }

  public void run(StatCollector stat, Runnable appInitalizeMark, boolean newConfigFolder) {
    try {
      ApplicationEx app = (ApplicationEx)Application.get();
      app.load(ContainerPathManager.get().getOptionsPath());

      boolean enableSecurityManager = EarlyAccessProgramManager.is(PluginPermissionEarlyAccessProgramDescriptor.class);
      if (enableSecurityManager) {
        ConsuloSecurityManagerEnabler.enableSecurityManager();
      }

      if (needStartInTransaction()) {
        ((TransactionGuardEx)TransactionGuard.getInstance()).performUserActivity(() -> main(stat, appInitalizeMark, app, newConfigFolder, myArgs));
      }
      else {
        main(stat, appInitalizeMark, app, newConfigFolder, myArgs);
      }

      ApplicationStarterCore.ourLoaded = true;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public boolean isPerformProjectLoad() {
    return myPerformProjectLoad;
  }

  public void setPerformProjectLoad(boolean performProjectLoad) {
    myPerformProjectLoad = performProjectLoad;
  }
}
