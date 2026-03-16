// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.project.ui.impl.internal.wm.statusBar;

import consulo.annotation.component.ServiceImpl;
import consulo.component.util.SimpleModificationTracker;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.*;
import consulo.ui.UIAccess;
import org.jspecify.annotations.Nullable;
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
    private final WindowManager myWindowManager;

    @Inject
    public StatusBarWidgetsManagerImpl(Project project, WindowManager windowManager) {
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
    }

    private StatusBarWidgetsCache getCache() {
        return myProject.getExtensionPoint(StatusBarWidgetFactory.class).getOrBuildCache(StatusBarWidgetsCache.CACHE_KEY);
    }

    @Override
    public void updateAllWidgets(@Nullable IdeFrame frame, UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            for (StatusBarWidgetFactory factory : myWidgetFactories.keySet()) {
                updateWidget(frame, factory, uiAccess);
            }
        }
    }

    public void disableAllWidgets(UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            for (StatusBarWidgetFactory factory : myWidgetFactories.keySet()) {
                disableWidget(null, factory, uiAccess);
            }
        }
    }

    @Override
    public void updateWidget(Class<? extends StatusBarWidgetFactory> factoryExtension, UIAccess uiAccess) {
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
    public void updateWidget(StatusBarWidgetFactory factory, UIAccess uiAccess) {
        updateWidget(null, factory, uiAccess);
    }

    public void updateWidget(@Nullable IdeFrame frame,
                             StatusBarWidgetFactory factory,
                             UIAccess uiAccess) {
        if (factory.isAvailable(myProject) && (!factory.isConfigurable() || StatusBarWidgetSettings.getInstance().isEnabled(factory))) {
            enableWidget(frame, factory, uiAccess);
        }
        else {
            disableWidget(frame, factory, uiAccess);
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
            myWidgetFactories.forEach((factory, createdWidget) -> disableWidget(null, factory, myProject.getUIAccess()));
            myWidgetFactories.clear();
        }
    }

    @Nullable
    public StatusBarWidgetFactory findWidgetFactory(String widgetId) {
        return getCache().keyMap().get(widgetId);
    }

    public Set<StatusBarWidgetFactory> getWidgetFactories() {
        synchronized (myWidgetFactories) {
            return myWidgetFactories.keySet();
        }
    }

    private void enableWidget(@Nullable IdeFrame frame, StatusBarWidgetFactory factory, UIAccess uiAccess) {
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
                    StatusBarEx statusBar = frame != null
                        ? (StatusBarEx) frame.getStatusBar()
                        : (StatusBarEx) myWindowManager.getStatusBar(myProject);

                    if (statusBar == null) {
                        LOG.error("Cannot add a widget for project without root status bar: " + factory.getId());
                        return;
                    }

                    statusBar.addWidget(widget, order, this);
                }
            });
        }
    }

    private void disableWidget(@Nullable IdeFrame frame, StatusBarWidgetFactory factory, UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            StatusBarWidget createdWidget = myWidgetFactories.put(factory, null);
            if (createdWidget != null) {
                factory.disposeWidget(createdWidget);
                uiAccess.giveIfNeed(() -> {
                    if (!myProject.isDisposed()) {
                        StatusBarEx statusBar = frame != null
                            ? (StatusBarEx) frame.getStatusBar()
                            : (StatusBarEx) myWindowManager.getStatusBar(myProject);

                        if (statusBar != null) {
                            statusBar.removeWidget(createdWidget.getId());
                        }
                    }
                });
            }
        }
    }

    public boolean canBeEnabledOnStatusBar(StatusBarWidgetFactory factory, StatusBar statusBar) {
        return factory.isAvailable(myProject) && factory.isConfigurable() && factory.canBeEnabledOn(statusBar);
    }

    private void addWidgetFactory(StatusBarWidgetFactory factory) {
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

    private void removeWidgetFactory(StatusBarWidgetFactory factory, UIAccess uiAccess) {
        synchronized (myWidgetFactories) {
            disableWidget(null, factory, uiAccess);
            myWidgetFactories.remove(factory);
            incModificationCount();
        }
    }
}
