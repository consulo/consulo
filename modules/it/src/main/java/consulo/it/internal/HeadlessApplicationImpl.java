/*
 * Copyright 2013-2026 consulo.io
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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.application.internal.StartupProgress;
import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.progress.ProgressManager;
import consulo.component.internal.ComponentBinding;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.UIAccess;
import consulo.util.lang.ControlFlowException;
import consulo.util.lang.ref.SimpleReference;

import java.util.function.BooleanSupplier;

/**
 * Real (non-stub) headless {@link consulo.application.Application} for integration tests, built on
 * {@link UnifiedApplication}. UI work is dispatched onto the single {@link HeadlessUIAccess} thread.
 *
 * @author VISTALL
 */
public class HeadlessApplicationImpl extends UnifiedApplication {
    private static final Logger LOG = Logger.getInstance(HeadlessApplicationImpl.class);

    public HeadlessApplicationImpl(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splashRef) {
        super(componentBinding, splashRef);
    }

    @Override
    public int getProfiles() {
        return super.getProfiles() | ComponentProfiles.INTEGRATION_TEST;
    }

    /**
     * Cached as the IDE's application caches it: every lexer step of every parse checks for cancellation through it, and
     * a lookup in the container each time is most of the cost of indexing a large library.
     */
    private volatile ProgressManager myProgressManager;

    @Override
    public ProgressManager getProgressManager() {
        ProgressManager progressManager = myProgressManager;
        if (progressManager == null) {
            myProgressManager = progressManager = getInstance(ProgressManager.class);
        }
        return progressManager;
    }

    @Override
    public void invokeLater(Runnable runnable) {
        invokeLater(runnable, () -> false);
    }

    @Override
    public void invokeLater(Runnable runnable, BooleanSupplier expired) {
        HeadlessUIAccess.INSTANCE.giveAsync(() -> {
            if (!expired.getAsBoolean()) {
                runnable.run();
            }
            return null;
        }).whenComplete((result, error) -> {
            if (error != null && !(error instanceof ControlFlowException)) {
                LOG.error("Headless UI task failed", error);
            }
        });
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state) {
        invokeLater(runnable);
    }

    @Override
    public void invokeLater(Runnable runnable, ModalityState state, BooleanSupplier expired) {
        invokeLater(runnable, expired);
    }

    @Override
    public UIAccess getLastUIAccess() {
        return HeadlessUIAccess.INSTANCE;
    }
}
