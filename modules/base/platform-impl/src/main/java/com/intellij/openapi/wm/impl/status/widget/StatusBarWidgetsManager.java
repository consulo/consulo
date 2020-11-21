// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.wm.impl.status.widget;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.StatusBarWidgetFactory;
import com.intellij.openapi.wm.WindowManager;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

@Singleton
public final class StatusBarWidgetsManager extends SimpleModificationTracker implements Disposable {
  @Nonnull
  public static StatusBarWidgetsManager getInstance(@Nonnull Project project) {
    return ServiceManager.getService(project, StatusBarWidgetsManager.class);
  }

  private static final Logger LOG = Logger.getInstance(StatusBar.class);

  private final Map<StatusBarWidgetFactory, StatusBarWidget> myWidgetFactories = new LinkedHashMap<>();
  private final Map<String, StatusBarWidgetFactory> myWidgetIdsMap = new HashMap<>();

  private final Project myProject;

  @Inject
  public StatusBarWidgetsManager(@Nonnull Project project) {
    myProject = project;

    StatusBarWidgetFactory.EP_NAME.forEachExtensionSafe(project.getApplication(), this::addWidgetFactory);
    
    //StatusBarWidgetFactory.EP_NAME.getPoint().addExtensionPointListener(new ExtensionPointListener<StatusBarWidgetFactory>() {
    //  @Override
    //  public void extensionAdded(@NotNull StatusBarWidgetFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
    //    addWidgetFactory(factory);
    //  }
    //
    //  @Override
    //  public void extensionRemoved(@NotNull StatusBarWidgetFactory factory, @NotNull PluginDescriptor pluginDescriptor) {
    //    removeWidgetFactory(factory);
    //  }
    //}, true, this);
    //
    ////noinspection deprecation
    //StatusBarWidgetProvider.EP_NAME.getPoint().addExtensionPointListener(new ExtensionPointListener<StatusBarWidgetProvider>() {
    //  @Override
    //  public void extensionAdded(@NotNull StatusBarWidgetProvider provider, @NotNull PluginDescriptor pluginDescriptor) {
    //    addWidgetFactory(new StatusBarWidgetProviderToFactoryAdapter(myProject, provider));
    //  }
    //
    //  @Override
    //  public void extensionRemoved(@NotNull StatusBarWidgetProvider provider, @NotNull PluginDescriptor pluginDescriptor) {
    //    removeWidgetFactory(new StatusBarWidgetProviderToFactoryAdapter(myProject, provider));
    //  }
    //}, true, this);
  }

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

  public void updateWidget(@Nonnull Class<? extends StatusBarWidgetFactory> factoryExtension, @Nonnull UIAccess uiAccess) {
    StatusBarWidgetFactory factory = StatusBarWidgetFactory.EP_NAME.findExtension(factoryExtension);
    synchronized (myWidgetFactories) {
      if (factory == null || !myWidgetFactories.containsKey(factory)) {
        LOG.info("Factory is not registered as `com.intellij.statusBarWidgetFactory` extension: " + factoryExtension.getName());
        return;
      }
      updateWidget(factory, uiAccess);
    }
  }

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
    return myWidgetIdsMap.get(widgetId);
  }

  @Nonnull
  public Set<StatusBarWidgetFactory> getWidgetFactories() {
    synchronized (myWidgetFactories) {
      return myWidgetFactories.keySet();
    }
  }

  private void enableWidget(@Nonnull StatusBarWidgetFactory factory, UIAccess uiAccess) {
    List<StatusBarWidgetFactory> availableFactories = StatusBarWidgetFactory.EP_NAME.getExtensionList();
    synchronized (myWidgetFactories) {
      if (!myWidgetFactories.containsKey(factory)) {
        LOG.error("Factory is not registered as `com.intellij.statusBarWidgetFactory` extension: " + factory.getId());
        return;
      }

      StatusBarWidget createdWidget = myWidgetFactories.get(factory);
      if (createdWidget != null) {
        // widget is already enabled
        return;
      }

      StatusBarWidget widget = factory.createWidget(myProject);
      myWidgetFactories.put(factory, widget);
      myWidgetIdsMap.put(widget.ID(), factory);
      String anchor = getAnchor(factory, availableFactories);

      uiAccess.giveIfNeed(() -> {
        if (!myProject.isDisposed()) {
          StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
          if (statusBar == null) {
            LOG.error("Cannot add a widget for project without root status bar: " + factory.getId());
            return;
          }
          statusBar.addWidget(widget, anchor, this);
        }
      });
    }
  }

  @Nonnull
  private String getAnchor(@Nonnull StatusBarWidgetFactory factory, @Nonnull List<StatusBarWidgetFactory> availableFactories) {
    if (factory instanceof StatusBarWidgetProviderToFactoryAdapter) {
      return ((StatusBarWidgetProviderToFactoryAdapter)factory).getAnchor();
    }
    int indexOf = availableFactories.indexOf(factory);
    for (int i = indexOf + 1; i < availableFactories.size(); i++) {
      StatusBarWidgetFactory nextFactory = availableFactories.get(i);
      StatusBarWidget widget = myWidgetFactories.get(nextFactory);
      if (widget != null) {
        return StatusBar.Anchors.before(widget.ID());
      }
    }
    for (int i = indexOf - 1; i >= 0; i--) {
      StatusBarWidgetFactory prevFactory = availableFactories.get(i);
      StatusBarWidget widget = myWidgetFactories.get(prevFactory);
      if (widget != null) {
        return StatusBar.Anchors.after(widget.ID());
      }
    }
    return StatusBar.Anchors.DEFAULT_ANCHOR;
  }

  private void disableWidget(@Nonnull StatusBarWidgetFactory factory, UIAccess uiAccess) {
    synchronized (myWidgetFactories) {
      StatusBarWidget createdWidget = myWidgetFactories.put(factory, null);
      if (createdWidget != null) {
        myWidgetIdsMap.remove(createdWidget.ID());
        factory.disposeWidget(createdWidget);
        uiAccess.giveIfNeed(() -> {
          if (!myProject.isDisposed()) {
            StatusBar statusBar = WindowManager.getInstance().getStatusBar(myProject);
            if (statusBar != null) {
              statusBar.removeWidget(createdWidget.ID());
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
      ApplicationManager.getApplication().invokeLater(() -> {
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
