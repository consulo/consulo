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
package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ServiceImpl;
import consulo.application.impl.internal.LaterInvocator;
import consulo.ide.impl.idea.codeInsight.actions.OptimizeImportsProcessor;
import consulo.ide.impl.idea.ide.HelpTooltipImpl;
import consulo.language.editor.AutoImportHelper;
import consulo.language.editor.localize.DaemonLocalize;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.internal.ModalityPerProjectEAPDescriptor;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.keymap.util.KeymapUtil;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 26-Jul-22
 */
@Singleton
@ServiceImpl
public class AutoImportHelperImpl implements AutoImportHelper {
    private final DaemonListeners myDaemonListeners;

    @Inject
    public AutoImportHelperImpl(DaemonListeners daemonListeners) {
        myDaemonListeners = daemonListeners;
    }

    @Override
    public boolean canChangeFileSilently(@Nonnull PsiFile file) {
        return myDaemonListeners.canChangeFileSilently(file);
    }

    @Override
    public boolean mayAutoImportNow(@Nonnull PsiFile psiFile, boolean isInContent) {
        Project project = psiFile.getProject();
        boolean isInModlessContext = ModalityPerProjectEAPDescriptor.is() ? !LaterInvocator.isInModalContextForProject(project) : !LaterInvocator.isInModalContext();
        return isInModlessContext && canChangeFileSilently(psiFile);
    }

    @Override
    public void runOptimizeImports(@Nonnull Project project, @Nonnull PsiFile file, boolean withProgress) {
        OptimizeImportsProcessor processor = new OptimizeImportsProcessor(project, file);
        if (withProgress) {
            processor.run();
        }
        else {
            processor.runWithoutProgress();
        }
    }

    @Nonnull
    @Override
    public LocalizeValue getImportMessage(@Nonnull LocalizeValue actioName, @Nonnull LocalizeValue kind, boolean multiple, @Nonnull String name) {
        String firstKeyboardShortcutText =
            KeymapUtil.getFirstKeyboardShortcutText(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_INTENTION_ACTIONS));

        String htmlColor = ColorUtil.toHtmlColor(HelpTooltipImpl.SHORTCUT_COLOR);

        return DaemonLocalize.importPopupHintText(kind, name, firstKeyboardShortcutText, actioName, htmlColor);
    }
}
