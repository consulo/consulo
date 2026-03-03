// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.data.impl;

import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.ide.impl.dataContext.PreCachedDataContext;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.collection.JBIterable;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Desktop AWT async data context. Walks the AWT component hierarchy on EDT,
 * pre-initializes providers for background use, and delegates to {@link PreCachedDataContext}.
 */
class DesktopAsyncDataContext implements AsyncDataContext {
    private final PreCachedDataContext myDelegate;

    @RequiredUIAccess
    DesktopAsyncDataContext(DesktopDataManagerImpl dataManager, DataContext syncContext) {
        UIAccess.assertIsUIThread();
        Component component = syncContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        List<Component> hierarchy = JBIterable.generate(component, Component::getParent).toList();

        List<DataProvider> providers = new ArrayList<>(hierarchy.size());
        for (Component each : hierarchy) {
            DataProvider provider = dataManager.getDataProviderEx(each);
            if (provider != null) {
                providers.add(PreCachedDataContext.initProviderForAsync(provider));
            }
        }

        myDelegate = new PreCachedDataContext(dataManager, providers);
    }

    @Nullable
    @Override
    public <T> T getData(@Nonnull Key<T> dataId) {
        return myDelegate.getData(dataId);
    }
}
