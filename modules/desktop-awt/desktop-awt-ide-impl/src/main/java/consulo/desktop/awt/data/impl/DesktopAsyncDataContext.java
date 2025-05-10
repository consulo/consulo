// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.data.impl;

import consulo.desktop.awt.ui.ProhibitAWTEvents;
import consulo.ide.impl.idea.openapi.actionSystem.BackgroundableDataProvider;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionUpdateEdtExecutor;
import consulo.ide.impl.idea.reference.SoftReference;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.application.AccessToken;
import consulo.application.ApplicationManager;
import consulo.application.util.ConcurrentFactoryMap;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import java.awt.*;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;

class DesktopAsyncDataContext extends DesktopDataManagerImpl.MyDataContext implements AsyncDataContext {
  private static final Logger LOG = Logger.getInstance(AsyncDataContext.class);
  private final List<WeakReference<Component>> myHierarchy;

  private final Map<Component, DataProvider> myProviders = ConcurrentFactoryMap.create(key -> ActionUpdateEdtExecutor.computeOnEdt(() -> {
                                                                                         DataProvider provider = getDataManager().getDataProviderEx(key);
                                                                                         if (provider == null) return null;

                                                                                         if (provider instanceof BackgroundableDataProvider) {
                                                                                           return ((BackgroundableDataProvider)provider).createBackgroundDataProvider();
                                                                                         }
                                                                                         return dataKey -> {
                                                                                           boolean bg = !ApplicationManager.getApplication().isDispatchThread();
                                                                                           return ActionUpdateEdtExecutor.computeOnEdt(() -> {
                                                                                             long start = System.currentTimeMillis();
                                                                                             try {
                                                                                               return provider.getData(dataKey);
                                                                                             }
                                                                                             finally {
                                                                                               long elapsed = System.currentTimeMillis() - start;
                                                                                               if (elapsed > 100 && bg) {
                                                                                                 LOG.warn("Slow data provider " + provider + " took " + elapsed + "ms on " + dataKey + ". Consider speeding it up and/or implementing BackgroundableDataProvider.");
                                                                                               }
                                                                                             }
                                                                                           });
                                                                                         };
                                                                                       }),

                                                                                       ContainerUtil::createConcurrentWeakKeySoftValueMap);

  @RequiredUIAccess
  DesktopAsyncDataContext(DesktopDataManagerImpl dataManager, DataContext syncContext) {
    super(dataManager, syncContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT));
    UIAccess.assertIsUIThread();
    Component component = getData(UIExAWTDataKey.CONTEXT_COMPONENT);
    List<Component> hierarchy = JBIterable.generate(component, Component::getParent).toList();
    for (Component each : hierarchy) {
      myProviders.get(each);
    }
    myHierarchy = ContainerUtil.map(hierarchy, WeakReference::new);
  }

  @Override
  protected Object calcData(@Nonnull Key dataId, Component focused) {
    try (AccessToken ignored = ProhibitAWTEvents.start("getData")) {
      for (WeakReference<Component> reference : myHierarchy) {
        Component component = SoftReference.dereference(reference);
        if (component == null) continue;
        DataProvider dataProvider = myProviders.get(component);
        if (dataProvider == null) continue;
        Object data = getDataManager().getDataFromProvider(dataProvider, dataId, null);
        if (data != null) return data;
      }
    }
    return null;
  }

}
