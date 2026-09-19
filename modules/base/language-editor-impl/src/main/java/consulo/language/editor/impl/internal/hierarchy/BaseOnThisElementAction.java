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
package consulo.language.editor.impl.internal.hierarchy;

import consulo.annotation.component.ActionImpl;
import consulo.disposer.Disposer;
import consulo.language.editor.hierarchy.HierarchyKind;
import consulo.language.editor.hierarchy.HierarchyViewType;
import consulo.language.editor.internal.hierarchy.HierarchyBrowseService;
import consulo.language.editor.internal.hierarchy.HierarchyBrowser;
import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnActionWithAsyncUpdate;
import consulo.ui.ex.action.ShortcutSet;
import consulo.ui.ex.action.coroutine.ActionSafeReadLock;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;

/**
 * Re-opens the hierarchy on the node the user has selected, in the same kind and - where the new model still
 * offers it - the same view. Which elements may serve as a base, and how the action names them, is the
 * model's answer.
 *
 * @author cdr
 */
@ActionImpl(id = "TypeHierarchyBase.BaseOnThisType")
public final class BaseOnThisElementAction extends AnAction implements AnActionWithAsyncUpdate {
    public BaseOnThisElementAction() {
        super(LanguageEditorLocalize.actionBaseOnThisType());
    }

    /**
     * Shortcut of the action which opens a hierarchy of the given kind, so that pressing it inside an open
     * hierarchy re-bases it instead of opening another one. Answers null where the kind names no action.
     */
    public static @Nullable ShortcutSet findShortcutSet(@Nullable String actionId) {
        if (actionId == null) {
            return null;
        }
        AnAction action = ActionManager.getInstance().getAction(actionId);
        return action != null ? action.getShortcutSet() : null;
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent event) {
        HierarchyBrowser browser = event.getData(HierarchyBrowser.KEY);
        if (browser == null) {
            return;
        }

        PsiElement selectedElement = browser.getSelectedElement();
        if (selectedElement == null || !browser.isApplicableElement(selectedElement) || !browser.canBeBase(selectedElement)) {
            return;
        }

        HierarchyKind kind = browser.getKind();
        HierarchyViewType currentViewType = browser.getCurrentViewType();
        Project project = browser.getProject();
        Disposer.dispose(browser);

        UIAccess uiAccess = UIAccess.current();

        HierarchyBrowseService.getInstance(project).browse(
            kind,
            selectedElement,
            event.getDataContext(),
            newBrowser -> {
                if (currentViewType != null) {
                    uiAccess.give(() -> newBrowser.changeViewType(newBrowser.correctViewType(currentViewType)));
                }
            }
        );
    }

    @Override
    public Coroutine<?, ?> updateAsync(AnActionEvent e) {
        return ActionSafeReadLock.run(e, presentation -> {
            HierarchyBrowser browser = e.getData(HierarchyBrowser.KEY);
            if (browser == null) {
                presentation.setEnabledAndVisible(false);
                return;
            }

            ShortcutSet shortcutSet = findShortcutSet(browser.getKind().getShortcutActionId());
            if (shortcutSet != null) {
                registerCustomShortcutSet(shortcutSet, null);
            }

            PsiElement selectedElement = browser.getSelectedElement();
            if (selectedElement == null || !browser.isApplicableElement(selectedElement) || !browser.canBeBase(selectedElement)) {
                presentation.setEnabledAndVisible(false);
                return;
            }

            presentation.setVisible(true);
            presentation.setEnabled(!selectedElement.equals(browser.getHierarchyBase()) && selectedElement.isValid());

            LocalizeValue text = browser.getBaseOnThisText(selectedElement);
            if (text.isNotEmpty()) {
                presentation.setText(text);
            }
        }).toCoroutine();
    }
}
