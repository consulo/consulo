/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.desktop.awt.internal.versionControlSystem.patch;

import consulo.annotation.component.ExtensionImpl;
import consulo.diff.DiffContext;
import consulo.diff.FrameDiffTool;
import consulo.diff.request.DiffRequest;
import consulo.versionControlSystem.impl.internal.patch.tool.ApplyPatchDiffRequest;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.localize.VcsLocalize;
import jakarta.annotation.Nonnull;

@ExtensionImpl
public class ApplyPatchDiffTool implements FrameDiffTool {
    @Nonnull
    @Override
    @RequiredUIAccess
    public DiffViewer createComponent(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return new MyApplyPatchViewer(context, (ApplyPatchDiffRequest)request);
    }

    @Override
    public boolean canShow(@Nonnull DiffContext context, @Nonnull DiffRequest request) {
        return request instanceof ApplyPatchDiffRequest;
    }

    @Nonnull
    @Override
    public String getName() {
        return VcsLocalize.patchApplySomehowDiffName().get();
    }

    private static class MyApplyPatchViewer extends ApplyPatchViewer implements DiffViewer {
        public MyApplyPatchViewer(@Nonnull DiffContext context, @Nonnull ApplyPatchDiffRequest request) {
            super(context, request);
        }

        @Nonnull
        @Override
        @RequiredUIAccess
        public ToolbarComponents init() {
            initPatchViewer();

            ToolbarComponents components = new ToolbarComponents();
            components.statusPanel = getStatusPanel();
            components.toolbarActions = createToolbarActions();

            return components;
        }
    }
}
