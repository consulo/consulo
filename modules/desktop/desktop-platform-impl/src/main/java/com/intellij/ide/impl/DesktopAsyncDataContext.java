// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.impl;

import com.intellij.ide.ProhibitAWTEvents;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.actionSystem.impl.ActionUpdateEdtExecutor;
import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import consulo.util.dataholder.Key;
import com.intellij.reference.SoftReference;
import com.intellij.util.containers.ConcurrentFactoryMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.JBIterable;
import javax.annotation.Nonnull;

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

  DesktopAsyncDataContext(DesktopDataManagerImpl dataManager, DataContext syncContext) {
    super(dataManager, syncContext.getData(PlatformDataKeys.CONTEXT_COMPONENT));
    ApplicationManager.getApplication().assertIsDispatchThread();
    Component component = getData(PlatformDataKeys.CONTEXT_COMPONENT);
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
