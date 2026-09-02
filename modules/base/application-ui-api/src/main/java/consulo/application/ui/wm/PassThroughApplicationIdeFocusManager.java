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
package consulo.application.ui.wm;

import consulo.component.ComponentManager;
import consulo.dataContext.DataContext;
import consulo.ui.ModalityState;
import consulo.util.concurrent.AsyncResult;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.awt.Component;

/**
 * Focus manager for frontends without a real focus subsystem: focus requests resolve immediately,
 * "when focus settles down" runs inline, and every lookup routes back to this single instance.
 *
 * @author VISTALL
 * @since 2026-09-01
 */
public abstract class PassThroughApplicationIdeFocusManager implements ApplicationIdeFocusManager {
    @Override
    public AsyncResult<Void> requestFocus(Component c, boolean forced) {
        return AsyncResult.resolved();
    }

    @Override
    public AsyncResult<Void> requestFocus(consulo.ui.Component c, boolean forced) {
        return AsyncResult.resolved();
    }

    @Override
    public JComponent getFocusTargetFor(JComponent comp) {
        return null;
    }

    @Override
    public void doWhenFocusSettlesDown(Runnable runnable) {
        runnable.run();
    }

    @Override
    public void doWhenFocusSettlesDown(Runnable runnable, ModalityState modality) {
        runnable.run();
    }

    @Override
    public void doWhenFocusSettlesDown(ExpirableRunnable runnable) {
        if (!runnable.isExpired()) {
            runnable.run();
        }
    }

    @Override
    public Component getFocusedDescendantFor(Component c) {
        return null;
    }

    @Override
    public AsyncResult<Void> requestDefaultFocus(boolean forced) {
        return AsyncResult.resolved();
    }

    @Override
    public boolean isFocusTransferEnabled() {
        return true;
    }

    @Override
    public Component getFocusOwner() {
        return null;
    }

    @Override
    public void runOnOwnContext(DataContext context, Runnable runnable) {
        runnable.run();
    }

    @Override
    public @Nullable Component getLastFocusedFor(@Nullable FocusableFrame frame) {
        return null;
    }

    @Override
    public @Nullable FocusableFrame getLastFocusedFrame() {
        return null;
    }

    @Override
    public void toFront(JComponent c) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public IdeFocusManager findInstanceByComponent(Component c) {
        return this;
    }

    @Override
    public IdeFocusManager findInstanceByContext(@Nullable DataContext context) {
        return this;
    }

    @Override
    public IdeFocusManager getInstanceForProject(@Nullable ComponentManager componentManager) {
        return this;
    }
}
