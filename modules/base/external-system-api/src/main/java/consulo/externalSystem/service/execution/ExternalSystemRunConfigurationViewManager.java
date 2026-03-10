// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
package consulo.externalSystem.service.execution;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.BuildProgressObservable;
import consulo.build.ui.ViewManager;
import consulo.build.ui.event.BuildEvent;
import consulo.build.ui.internal.BuildProgressObservableListener;
import consulo.build.ui.progress.BuildProgressListener;
import consulo.disposer.Disposable;
import consulo.disposer.util.DisposableList;
import consulo.logging.Logger;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
public final class ExternalSystemRunConfigurationViewManager
    implements ViewManager, BuildProgressListener, BuildProgressObservable {

    private static final Logger LOG = Logger.getInstance(ExternalSystemRunConfigurationViewManager.class);

    private final DisposableList<BuildProgressListener> myListeners = DisposableList.create();

    @Inject
    public ExternalSystemRunConfigurationViewManager(@Nonnull Project project) {
        BuildProgressObservableListener.listen(project, this);
    }

    @Override
    public boolean isConsoleEnabledByDefault() {
        return true;
    }

    @Override
    public boolean isBuildContentView() {
        return false;
    }

    @Override
    public void addListener(@Nonnull BuildProgressListener listener, @Nonnull Disposable disposable) {
        myListeners.add(listener, disposable);
    }

    @Override
    public void onEvent(@Nonnull Object buildId, @Nonnull BuildEvent event) {
        for (BuildProgressListener listener : myListeners) {
            try {
                listener.onEvent(buildId, event);
            }
            catch (Exception e) {
                LOG.warn(e);
            }
        }
    }
}
