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
package consulo.ide.impl.idea.openapi.roots.ui.configuration.artifacts.actions;

import consulo.project.localize.ProjectLocalize;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.compiler.artifact.ui.TreeNodePresentation;

import org.jspecify.annotations.Nullable;

import javax.swing.*;

/**
 * @author nik
 */
public abstract class ArtifactEditorNavigateActionBase extends DumbAwareAction {
    public ArtifactEditorNavigateActionBase(JComponent contextComponent) {
        super(ProjectLocalize.actionNameFacetNavigate());
        registerCustomShortcutSet(CommonShortcuts.getEditSource(), contextComponent);
    }

    @Override
    public void update(AnActionEvent e) {
        TreeNodePresentation presentation = getPresentation();
        e.getPresentation().setEnabled(presentation != null && presentation.canNavigateToSource());
    }

    protected abstract @Nullable TreeNodePresentation getPresentation();

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        TreeNodePresentation presentation = getPresentation();
        if (presentation != null) {
            presentation.navigateToSource();
        }
    }
}
