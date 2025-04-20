/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.diff.merge;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;
import java.util.function.BooleanSupplier;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface MergeTool {
    /**
     * Creates viewer for the given request. Clients should call {@link #canShow(MergeContext, MergeRequest)} first.
     */
    @RequiredUIAccess
    @Nonnull
    MergeViewer createComponent(@Nonnull MergeContext context, @Nonnull MergeRequest request);

    boolean canShow(@Nonnull MergeContext context, @Nonnull MergeRequest request);

    record ActionRecord(@Nonnull LocalizeValue title, @Nonnull Runnable onActionPerformed) {
    }

    /**
     * Merge viewer should call {@link MergeContext#finishMerge(MergeResult)} when processing is over.
     * <p>
     * {@link MergeRequest#applyResult(MergeResult)} will be performed by the caller, so it shouldn't be called by MergeViewer directly.
     */
    interface MergeViewer extends Disposable {
        @Nonnull
        JComponent getComponent();

        @Nullable
        JComponent getPreferredFocusedComponent();

        /**
         * @return Action that should be triggered on the corresponding action.
         * <p/>
         * Typical implementation can perform some checks and either call finishMerge(result) or do nothing
         * <p/>
         * return null if action is not available
         */
        @Nullable
        ActionRecord getResolveAction(@Nonnull MergeResult result);

        /**
         * Should be called after adding {@link #getComponent()} to the components hierarchy.
         */
        @Nonnull
        @RequiredUIAccess
        ToolbarComponents init();

        @Override
        @RequiredUIAccess
        void dispose();
    }

    class ToolbarComponents {
        @Nullable
        public List<AnAction> toolbarActions;
        @Nullable
        public JComponent statusPanel;

        /**
         * return false if merge window should be prevented from closing and canceling resolve.
         */
        @Nullable
        public BooleanSupplier closeHandler;
    }
}
