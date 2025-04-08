// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@ServiceImpl
public final class StatusBarWidgetsManagerImpl extends SimpleModificationTracker implements Disposable, StatusBarWidgetsManager {
    private static final Logger LOG = Logger.getInstance(StatusBar.class);

    private final Map<StatusBarWidgetFactory, StatusBarWidget> myWidgetFactories = new LinkedHashMap<>();

    private final Project myProject;
    @Nonnull
    private final WindowManager myWindowManager;

    @Inject
    public StatusBarWidgetsManagerImpl(@Nonnull Project project, @Nonnull WindowManager windowManager) {
        myProject = project;
        myWindowManager = windowManager;

        project.getExtensionPoint(StatusBarWidgetFactory.class).forEachExtensionSafe(this::addWidgetFactory);

        //StatusBarWidgetFactory.EP_NAME.getPoint().addExtensionPointListener(new ExtensionPointListener<StatusBarWidgetFactory>() {
        //    @Override
        //    public void extensionAdded(@NotNull StatusBarWidgetFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
        //        addWidgetFactory(factory);
        //    }
        //
        //    @Override
        //    public void extensionRemoved(@NotNull StatusBarWidgetFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
        //        removeWidgetFactory(factory);
        //    }
        //}, true, this);
        //
    }

    @Nonnull
    private StatusBarWidgetsCache getCache() {
        return myProject.getExtensionPoint(StatusBarWidgetFactory.class).getOrBuildCache(StatusBarWidgetsCache.CACHE_KEY);
    }

    @Override
    public void updateAllWidgets(@Nonnull UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            for (StatusBarWidgetFactory factory : myWidgetFactories.keySet()) {
                updateWidget(factory, uiAccess);
            }
        }
    }

    public void disableAllWidgets(@Nonnull UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            for (StatusBarWidgetFactory factory : myWidgetFactories.keySet()) {
                disableWidget(factory, uiAccess);
            }
        }
    }

    @Override
    public void updateWidget(@Nonnull Class<? extends StatusBarWidgetFactory> factoryExtension, @Nonnull UIAccess uiAccess) {
        StatusBarWidgetFactory factory = myProject.getExtensionPoint(StatusBarWidgetFactory.class).findExtension(factoryExtension);
        synchronized (myWidgetFactories) {
            if (factory == null || !myWidgetFactories.containsKey(factory)) {
                LOG.info("Factory is not registered as `StatusBarWidgetFactory` extension: " + factoryExtension.getName());
                return;
            }
            updateWidget(factory, uiAccess);
        }
    }

    @Override
    public void updateWidget(@Nonnull StatusBarWidgetFactory factory, @Nonnull UIAccess uiAccess) {
        if (factory.isAvailable(myProject) && (!factory.isConfigurable() || StatusBarWidgetSettings.getInstance().isEnabled(factory))) {
            enableWidget(factory, uiAccess);
        }
        else {
            disableWidget(factory, uiAccess);
        }
    }

    public boolean wasWidgetCreated(@Nullable StatusBarWidgetFactory factory) {
        synchronized (myWidgetFactories) {
            return myWidgetFactories.get(factory) != null;
        }
    }

    @Override
    public void dispose() {
        synchronized (myWidgetFactories) {
            myWidgetFactories.forEach((factory, createdWidget) -> disableWidget(factory, Application.get().getLastUIAccess()));
            myWidgetFactories.clear();
        }
    }

    @Nullable
    public StatusBarWidgetFactory findWidgetFactory(@Nonnull String widgetId) {
        return getCache().keyMap().get(widgetId);
    }

    @Nonnull
    public Set<StatusBarWidgetFactory> getWidgetFactories() {
        synchronized (myWidgetFactories) {
            return myWidgetFactories.keySet();
        }
    }

    private void enableWidget(@Nonnull StatusBarWidgetFactory factory, UIAccess uiAccess) {
        List<String> order = getCache().order();

        synchronized (myWidgetFactories) {
            if (!myWidgetFactories.containsKey(factory)) {
                LOG.error("Factory is not registered as `StatusBarWidgetFactory` extension: " + factory.getId());
                return;
            }

            StatusBarWidget createdWidget = myWidgetFactories.get(factory);
            if (createdWidget != null) {
                // widget is already enabled
                return;
            }

            StatusBarWidget widget = factory.createWidget(myProject);
            myWidgetFactories.put(factory, widget);

            uiAccess.giveIfNeed(() -> {
                if (!myProject.isDisposed()) {
                    StatusBarEx statusBar = (StatusBarEx)myWindowManager.getStatusBar(myProject);
                    if (statusBar == null) {
                        LOG.error("Cannot add a widget for project without root status bar: " + factory.getId());
                        return;
                    }

                    statusBar.addWidget(widget, order, this);
                }
            });
        }
    }

    private void disableWidget(@Nonnull StatusBarWidgetFactory factory, UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            StatusBarWidget createdWidget = myWidgetFactories.put(factory, null);
            if (createdWidget != null) {
                factory.disposeWidget(createdWidget);
                uiAccess.giveIfNeed(() -> {
                    if (!myProject.isDisposed()) {
                        StatusBarEx statusBar = (StatusBarEx)myWindowManager.getStatusBar(myProject);
                        if (statusBar != null) {
                            statusBar.removeWidget(createdWidget.getId());
                        }
                    }
                });
            }
        }
    }

    public boolean canBeEnabledOnStatusBar(@Nonnull StatusBarWidgetFactory factory, @Nonnull StatusBar statusBar) {
        return factory.isAvailable(myProject) && factory.isConfigurable() && factory.canBeEnabledOn(statusBar);
    }

    private void addWidgetFactory(@Nonnull StatusBarWidgetFactory factory) {
        synchronized (myWidgetFactories) {
            if (myWidgetFactories.containsKey(factory)) {
                LOG.error("Factory has been added already: " + factory.getId());
                return;
            }
            myWidgetFactories.put(factory, null);
            myProject.getApplication().invokeLater(() -> {
                if (!myProject.isDisposed()) {
                    updateWidget(factory, UIAccess.current());
                }
            });
            incModificationCount();
        }
    }

    private void removeWidgetFactory(@Nonnull StatusBarWidgetFactory factory, @Nonnull UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            disableWidget(factory, uiAccess);
            myWidgetFactories.remove(factory);
            incModificationCount();
        }
    }
}
