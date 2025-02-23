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
package consulo.application.impl.internal.start;

import consulo.application.Application;
import consulo.application.ApplicationProperties;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.application.impl.internal.plugin.ConsuloSecurityManagerEnabler;
import consulo.application.internal.StartupProgress;
import consulo.application.internal.plugin.PluginsInitializeInfo;
import consulo.application.internal.plugin.PluginsLoader;
import consulo.application.internal.ApplicationEx;
import consulo.application.internal.ApplicationInfo;
import consulo.application.internal.ApplicationStarterCore;
import consulo.component.impl.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingBindingLoader;
import consulo.component.internal.inject.TopicBindingLoader;
import consulo.container.boot.ContainerPathManager;
import consulo.container.classloader.PluginClassLoader;
import consulo.container.internal.ShowErrorCaller;
import consulo.container.plugin.PluginDescriptor;
import consulo.container.plugin.PluginManager;
import consulo.container.util.StatCollector;
import consulo.localize.LocalizeManager;
import consulo.localize.internal.LocalizeManagerEx;
import consulo.logging.Logger;
import consulo.logging.internal.LoggerFactoryInitializer;
import consulo.platform.Platform;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.impl.image.BaseIconLibraryManager;
import consulo.util.io.URLUtil;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.function.Supplier;

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

        initializeEnviroment(false, args, stat);
    }

    @Nonnull
    protected abstract Application createApplication(ComponentBinding componentBinding,
                                                     boolean isHeadlessMode,
                                                     SimpleReference<StartupProgress> splashRef,
                                                     CommandLineArgs args);

    protected abstract void main(StatCollector stat,
                                 Runnable appInitializeMark,
                                 ApplicationEx app,
                                 boolean newConfigFolder,
                                 @Nonnull CommandLineArgs args);

    protected boolean needSetVersionChecker() {
        return true;
    }

    @Nullable
    public abstract StartupProgress createSplash(CommandLineArgs args);

    protected void initializeEnviroment(boolean isHeadlessMode, CommandLineArgs args, StatCollector stat) {
        StartupProgress splash = createSplash(args);
        if (splash != null) {
            mySplashRef.set(splash);
        }

        if (needSetVersionChecker()) {
            PluginsLoader.setVersionChecker();
        }

        myPluginsInitializeInfo = PluginsLoader.initPlugins(splash, isHeadlessMode);

        StatCollector libraryStats = new StatCollector();
        LocalizeManagerEx localizeManager = (LocalizeManagerEx) LocalizeManager.get();
        BaseIconLibraryManager iconLibraryManager = (BaseIconLibraryManager) IconLibraryManager.get();

        Map<String, Set<String>> filesWithMarkers = new HashMap<>();

        InjectingBindingLoader injectingBindingLoader = new InjectingBindingLoader();
        TopicBindingLoader topicBindingLoader = new TopicBindingLoader();

        libraryStats.markWith("injecting.binding.analyze", injectingBindingLoader::analyzeBindings);
        libraryStats.markWith("topic.binding.analyze", topicBindingLoader::analyzeBindings);

        libraryStats.markWith("library.analyze", () -> analyzeLibraries(filesWithMarkers));

        libraryStats.markWith("localize.initialize", localizeManager::initialize);
        libraryStats.markWith("icon.initialize",
            () -> iconLibraryManager.initialize(filesWithMarkers.get(BaseIconLibraryManager.ICON_DIRECTORY)));

        libraryStats.dump("Libraries", LOG::info);

        createApplication(new ComponentBinding(injectingBindingLoader, topicBindingLoader), isHeadlessMode, mySplashRef, args);
    }

    protected void analyzeLibraries(Map<String, Set<String>> filesWithMarkers) {
        PluginManager.forEachEnabledPlugin(pluginDescriptor -> {
            searchMarkerInClassLoaderMarker(pluginDescriptor, filesWithMarkers, BaseIconLibraryManager.ICON_DIRECTORY);
        });
    }

    private void searchMarkerInClassLoaderMarker(PluginDescriptor pluginDescriptor,
                                                 Map<String, Set<String>> filesWithMarkers,
                                                 String libraryDir) {
        PluginClassLoader pluginClassLoader = (PluginClassLoader) pluginDescriptor.getPluginClassLoader();

        try {
            Map<URL, Set<String>> urlsIndex = pluginClassLoader.getUrlsIndex();
            if (urlsIndex != null) {
                for (Map.Entry<URL, Set<String>> entry : urlsIndex.entrySet()) {
                    URL fileUrl = entry.getKey();
                    Set<String> index = entry.getValue();

                    for (String classPath : index) {
                        if (classPath.startsWith(libraryDir)) {
                            File file = URLUtil.urlToFile(fileUrl);

                            filesWithMarkers.computeIfAbsent(libraryDir, it -> new HashSet<>()).add(file.getPath());
                            // we add file - stop checking
                            break;
                        }
                    }
                }
            }
            else {
                Enumeration<URL> ownResources = pluginClassLoader.findOwnResources(libraryDir);

                while (ownResources.hasMoreElements()) {
                    URL url = ownResources.nextElement();

                    Pair<String, String> urlFileInfo = URLUtil.splitJarUrl(url.getFile());
                    if (urlFileInfo == null) {
                        continue;
                    }

                    filesWithMarkers.computeIfAbsent(libraryDir, it -> new HashSet<>()).add(urlFileInfo.getFirst());

                    break;
                }
            }
        }
        catch (IOException e) {
            LOG.error(e);
        }
    }

    public void run(StatCollector stat, Runnable appInitalizeMark, boolean newConfigFolder) {
        try {
            ApplicationEx app = (ApplicationEx) Application.get();
            stat.markWith("app.config.load", () -> {
                try {
                    app.load(ContainerPathManager.get().getOptionsPath());
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            boolean enableSecurityManager = EarlyAccessProgramManager.is(PluginPermissionEarlyAccessProgramDescriptor.class);
            if (enableSecurityManager) {
                ConsuloSecurityManagerEnabler.enableSecurityManager();
            }

            main(stat, appInitalizeMark, app, newConfigFolder, myArgs);

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

    public static void installExceptionHandler(Supplier<Logger> logger) {
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> processException(logger, e));
    }

    public static void processException(Supplier<Logger> logger, Throwable t) {
        StartupAbortedException se = null;

        if (t instanceof StartupAbortedException) {
            se = (StartupAbortedException) t;
        }
        else if (t.getCause() instanceof StartupAbortedException) {
            se = (StartupAbortedException) t.getCause();
        }
        else if (!ApplicationStarter.isLoaded()) {
            se = new StartupAbortedException(t);
        }

        if (se != null) {
            if (se.logError()) {
                try {
                    if (LoggerFactoryInitializer.isInitialized() && !(t instanceof ControlFlowException)) {
                        logger.get().error(t);
                    }
                }
                catch (Throwable ignore) {
                }

                ShowErrorCaller.showErrorDialog("Start Failed", t.getMessage(), t);
            }

            System.exit(se.exitCode());
        }

        if (!(t instanceof ControlFlowException)) {
            logger.get().error(t);
        }
    }

    @Nonnull
    public static String getFrameClass() {
        String name = ApplicationInfo.getInstance().getName().toLowerCase(Locale.ROOT);
        String wmClass = StringUtil.replaceChar(name, ' ', '-');
        if (ApplicationProperties.isInSandbox()) {
            wmClass += "-sandbox";
        }
        return wmClass;
    }
}
