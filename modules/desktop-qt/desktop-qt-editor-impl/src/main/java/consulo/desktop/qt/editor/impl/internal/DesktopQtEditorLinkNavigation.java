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
package consulo.desktop.qt.editor.impl.internal;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.util.EditSourceUtil;
import consulo.navigation.Navigatable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;

import java.util.function.Supplier;

/**
 * The go to declaration click for the qt editor - ctrl/cmd click and middle click. The hover half
 * (underline and hand cursor) is the platform ctrl hover handling, driven by the editor mouse events
 * the widget fires.
 */
public class DesktopQtEditorLinkNavigation {
    private final DesktopQtEditorImpl myEditor;

    public DesktopQtEditorLinkNavigation(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    /**
     * Go to declaration, which ctrl click is bound to. The target is resolved the same way the awt action does,
     * since a raw element is not the declaration and is not navigable by itself either.
     */
    @RequiredUIAccess
    public void navigateTo(int offset) {
        Project project = myEditor.getProject();
        if (project == null) {
            return;
        }

        myEditor.getCaretModel().moveToOffset(offset);

        PsiDocumentManager.getInstance(project).commitAllDocuments();

        DumbService dumbService = DumbService.getInstance(project);

        Navigatable navigatable = Application.get().runReadAction((Supplier<Navigatable>) () -> {
            dumbService.setAlternativeResolveEnabled(true);
            try {
                Pair<PsiElement[], GotoDeclarationHandler> found =
                    GotoDeclarationAction.findAllTargetElementsInfo(project, myEditor, offset);

                PsiElement[] elements = found.getFirst();
                // no popup to choose between several targets yet, so the first one is taken
                if (elements == null || elements.length == 0) {
                    return null;
                }

                PsiElement element = elements[0];

                PsiElement declaration = TargetElementUtil.getGotoDeclarationTarget(element, element.getNavigationElement());
                if (declaration == null) {
                    declaration = element;
                }

                return declaration instanceof Navigatable target ? target : EditSourceUtil.getDescriptor(declaration);
            }
            catch (IndexNotReadyException e) {
                return null;
            }
            finally {
                dumbService.setAlternativeResolveEnabled(false);
            }
        });

        if (navigatable == null || !navigatable.canNavigate()) {
            return;
        }

        // inside a command so that navigating back finds it
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .run(() -> navigatable.navigate(true));
    }
}
