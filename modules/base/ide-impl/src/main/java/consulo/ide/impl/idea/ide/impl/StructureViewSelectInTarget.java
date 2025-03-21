/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.structureView.StructureViewWrapper;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.structureView.StructureViewFactoryEx;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.project.ui.view.SelectInTarget;
import consulo.project.ui.view.StandardTargetWeights;
import consulo.project.ui.wm.ToolWindowId;
import consulo.project.ui.wm.ToolWindowManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ExtensionImpl
public class StructureViewSelectInTarget implements SelectInTarget {
    private final Project myProject;

    @Inject
    public StructureViewSelectInTarget(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public LocalizeValue getActionText() {
        return IdeLocalize.selectInFileStructure();
    }

    @Override
    public boolean canSelect(SelectInContext context) {
        return context.getFileEditorProvider() != null;
    }

    @Override
    public void selectIn(final SelectInContext context, final boolean requestFocus) {
        final FileEditor fileEditor = context.getFileEditorProvider().get();

        ToolWindowManager windowManager = ToolWindowManager.getInstance(context.getProject());
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                StructureViewFactoryEx.getInstanceEx(myProject).runWhenInitialized(new Runnable() {
                    @Override
                    public void run() {
                        final StructureViewWrapper structureView = getStructureViewWrapper();
                        structureView.selectCurrentElement(fileEditor, context.getVirtualFile(), requestFocus);
                    }
                });
            }
        };
        if (requestFocus) {
            windowManager.getToolWindow(ToolWindowId.STRUCTURE_VIEW).activate(runnable);
        }
        else {
            runnable.run();
        }

    }

    private StructureViewWrapper getStructureViewWrapper() {
        return StructureViewFactoryEx.getInstanceEx(myProject).getStructureViewWrapper();
    }

    @Override
    public String getToolWindowId() {
        return ToolWindowId.STRUCTURE_VIEW;
    }

    @Override
    public float getWeight() {
        return StandardTargetWeights.STRUCTURE_WEIGHT;
    }
}
