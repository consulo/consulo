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
package consulo.desktop.qt.editor.impl;

import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.codeEditor.EditorColors;
import consulo.codeEditor.markup.HighlighterLayer;
import consulo.codeEditor.markup.HighlighterTargetArea;
import consulo.codeEditor.markup.RangeHighlighter;
import consulo.colorScheme.TextAttributes;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.navigation.CtrlMouseHandler;
import consulo.ide.impl.idea.codeInsight.navigation.actions.GotoDeclarationAction;
import consulo.language.editor.TargetElementUtil;
import consulo.language.editor.navigation.GotoDeclarationHandler;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.EditSourceUtil;
import consulo.navigation.Navigatable;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Underlines the reference under the pointer while ctrl is held, and follows it when clicked.
 * <p>
 * The awt frontend gets this from {@code CtrlMouseHandler}'s own mouse motion listener, which reads the modifiers
 * and the screen position off a {@code java.awt.event.MouseEvent} and so is closed to a frontend without one. The
 * open seam is the static {@link CtrlMouseHandler#getInfoAt}: the frontend notices the ctrl hover itself, asks
 * what is under the offset, and draws the underline as ordinary markup. That is what the web frontend does, and
 * this is the same thing driven by qt events.
 *
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtEditorLinkNavigation {
    private final DesktopQtEditorImpl myEditor;

    private final List<RangeHighlighter> myHighlighters = new ArrayList<>();

    public DesktopQtEditorLinkNavigation(DesktopQtEditorImpl editor) {
        myEditor = editor;
    }

    public boolean hasLink() {
        return !myHighlighters.isEmpty();
    }

    /**
     * @return whether something navigable is under the offset, so the caller can put the hand cursor up
     */
    @RequiredUIAccess
    public boolean highlightLinkAt(int offset) {
        Project project = myEditor.getProject();
        if (project == null) {
            return false;
        }

        return Application.get().runReadAction((Supplier<Boolean>) () -> {
            clear();

            if (offset < 0 || offset > myEditor.getDocument().getTextLength()) {
                return false;
            }

            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(myEditor.getDocument());
            if (file == null) {
                return false;
            }

            CtrlMouseHandler.Info info =
                CtrlMouseHandler.getInfoAt(project, myEditor, file, offset, CtrlMouseHandler.BrowseMode.Declaration);
            if (info == null || !info.isNavigatable()) {
                return false;
            }

            TextAttributes attributes = myEditor.getColorsScheme().getAttributes(EditorColors.REFERENCE_HYPERLINK_COLOR);

            for (TextRange range : info.getRanges()) {
                myHighlighters.add(myEditor.getMarkupModel().addRangeHighlighter(
                    range.getStartOffset(),
                    range.getEndOffset(),
                    HighlighterLayer.HYPERLINK,
                    attributes,
                    HighlighterTargetArea.EXACT_RANGE
                ));
            }

            return true;
        });
    }

    public void clear() {
        for (RangeHighlighter highlighter : myHighlighters) {
            myEditor.getMarkupModel().removeHighlighter(highlighter);
        }

        myHighlighters.clear();
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

        clear();

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
