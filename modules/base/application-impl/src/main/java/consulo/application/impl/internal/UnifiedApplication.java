/*
 * Copyright 2013-2022 consulo.io
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
package consulo.application.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentProfiles;
import consulo.application.ApplicationManager;
import consulo.application.internal.StartupProgress;
import consulo.component.ComponentManager;
import consulo.component.internal.ComponentBinding;
import consulo.component.internal.inject.InjectingContainerBuilder;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ref.SimpleReference;

import java.awt.*;
import java.util.function.BooleanSupplier;

/**
 * @author VISTALL
 * @since 12-Jul-22
 */
public abstract class UnifiedApplication extends BaseApplication {
    protected static final Logger LOG = Logger.getInstance(BaseApplication.class);

    public UnifiedApplication(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splashRef) {
        super(componentBinding, splashRef);

        myLock = new UnifiedRWLock();

        ApplicationManager.setApplication(this);
    }

    @Override
    public int getProfiles() {
        return super.getProfiles() | ComponentProfiles.UNIFIED;
    }

    @Override
    protected void bootstrapInjectingContainer(InjectingContainerBuilder builder) {
        super.bootstrapInjectingContainer(builder);
    }

    @Override
    public void invokeLaterOnWriteThread(Runnable action, ModalityState modal, BooleanSupplier expired) {
        UIAccess uiAccess = getLastUIAccess();
        uiAccess.give(() -> runIntendedWriteActionOnCurrentThread(action));
    }

    @Override
    public void exit(boolean force, boolean exitConfirmed) {

    }

    @RequiredReadAction
    @Override
    public void assertReadAccessAllowed() {
        if (!isReadAccessAllowed()) {
            LOG.error(new IllegalArgumentException("Read access required"));
        }
    }

    @RequiredUIAccess
    @Override
    public void assertIsDispatchThread() {
        UIAccess.assertIsUIThread();
    }

    @Override
    public void exit() {

    }

    @Override
    public ComponentManager getApplication() {
        return this;
    }

    @Override
    public void invokeLater(Runnable runnable) {
        UIAccess lastUIAccess = getLastUIAccess();

        lastUIAccess.give(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, BooleanSupplier expired) {
        UIAccess lastUIAccess = getLastUIAccess();

        lastUIAccess.give(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, consulo.ui.ModalityState state) {
        UIAccess lastUIAccess = getLastUIAccess();

        lastUIAccess.give(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, consulo.ui.ModalityState state, BooleanSupplier expired) {
        UIAccess lastUIAccess = getLastUIAccess();

        lastUIAccess.give(runnable);
    }

    @RequiredUIAccess
    @Override
    public void invokeAndWait(Runnable runnable, consulo.ui.ModalityState modalityState) {
        if (isDispatchThread()) {
            runnable.run();
            return;
        }
        if (UIAccess.isUIThread()) {
            runnable.run();
            return;
        }

        if (holdsReadLock()) {
            throw new IllegalStateException("Calling invokeAndWait from read-action leads to possible deadlock.");
        }

        getLastUIAccess().giveAndWait(runnable);
    }

    @Override
    public void runIntendedWriteActionOnCurrentThread(Runnable action) {
        action.run();
    }

    @Override
    public boolean isReadAccessAllowed() {
        return isWriteThread() || myLock.isReadLockedByThisThread(); // no ui thread check
    }

    
    @Override
    public ModalityState getCurrentModalityState() {
        return ModalityState.nonModal();
    }

    
    @Override
    public ModalityState getModalityStateForComponent(Component c) {
        return ModalityState.nonModal();
    }

    
    @Override
    public ModalityState getDefaultModalityState() {
        return ModalityState.nonModal();
    }

    @RequiredUIAccess
    @Override
    public long getIdleTime() {
        return 0;
    }

    @Override
    public boolean isHeadlessEnvironment() {
        return false;
    }

    @Override
    public boolean isDisposeInProgress() {
        return false;
    }

    @Override
    public void restart(boolean exitConfirmed) {

    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void assertTimeConsuming() {

    }

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public boolean isUnifiedApplication() {
        return true;
    }
}
