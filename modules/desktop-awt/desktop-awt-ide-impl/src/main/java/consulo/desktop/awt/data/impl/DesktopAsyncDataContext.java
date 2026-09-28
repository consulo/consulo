// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.desktop.awt.data.impl;

import consulo.application.Application;
import consulo.dataContext.AsyncDataContext;
import consulo.dataContext.DataContext;
import consulo.ide.impl.dataContext.PreCachedDataContext;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.awtUnsafe.TargetAWT;
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

        PreCachedDataContext.Capture capture = PreCachedDataContext.captureForAsync(application);
        DataContext context = capture.collectCustomized(syncContext);
        Component component = context.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
        if (component == null) {
            consulo.ui.Component uiComponent = context.getData(consulo.ui.Component.KEY);
            component = uiComponent == null ? null : TargetAWT.to(uiComponent);
        }

        myDelegate = dataManager.captureAwtHierarchy(capture, component).build(dataManager);
    }

    @Override
    public <T> @Nullable T getData(Key<T> dataId) {
        return myDelegate.getData(dataId);
    }
}
