// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.data.impl;

import consulo.application.Application;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.ide.impl.dataContext.PreCachedDataContext;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.awt.*;

/**
 * Desktop AWT async data context. Captures the AWT component hierarchy on EDT into one snapshot and
 * delegates to {@link PreCachedDataContext}, which can be read from background threads.
 */
class DesktopAsyncDataContext implements AsyncDataContext {
    private final PreCachedDataContext myDelegate;

    @RequiredUIAccess
    DesktopAsyncDataContext(DesktopDataManagerImpl dataManager, DataContext syncContext, Application application) {
        UIAccess.assertIsUIThread();
        Component component = syncContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);

        myDelegate = dataManager.captureAwtHierarchy(component, true).build(dataManager);
    }

    @Override
    public <T> @Nullable T getData(Key<T> dataId) {
        return myDelegate.getData(dataId);
    }
}
