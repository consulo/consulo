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
package consulo.diff;

import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.List;

public interface FrameDiffTool extends DiffTool {
    /**
     * Creates viewer for the given request. Clients should call {@link #canShow(DiffContext, DiffRequest)} first.
     */
    @RequiredUIAccess
    @Nonnull
    DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request);

    interface DiffViewer extends Disposable {
        @Nonnull
        JComponent getComponent();

        @Nullable
        JComponent getPreferredFocusedComponent();

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
        public List<AnAction> popupActions;
        @Nullable
        public JComponent statusPanel;
    }
}
