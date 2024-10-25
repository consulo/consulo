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

package consulo.ide.impl.idea.codeInsight.unwrap;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.ide.impl.ui.impl.PopupChooserBuilder;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.highlight.HighlightManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.refactoring.internal.unwrap.UnwrapHelper;
import consulo.language.editor.refactoring.unwrap.ScopeHighlighter;
import consulo.language.editor.refactoring.unwrap.UnwrapDescriptor;
import consulo.language.editor.refactoring.unwrap.Unwrapper;
import consulo.language.editor.util.LanguageEditorUtil;
import consulo.language.impl.ast.RecursiveTreeElementWalkingVisitor;
import consulo.language.impl.ast.TreeElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.JBList;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.event.JBPopupAdapter;
import consulo.ui.ex.popup.event.LightweightWindowEvent;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class UnwrapHandler implements CodeInsightActionHandler {
    @Override
    public boolean startInWriteAction() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        if (!LanguageEditorUtil.checkModificationAllowed(editor)) {
            return;
        }
        List<AnAction> options = collectOptions(project, editor, file);
        selectOption(options, editor, file);
    }

    @RequiredReadAction
    private List<AnAction> collectOptions(Project project, Editor editor, PsiFile file) {
        List<AnAction> result = new ArrayList<>();

        UnwrapDescriptor d = getUnwrapDescription(file);

        for (Pair<PsiElement, Unwrapper> each : d.collectUnwrappers(project, editor, file)) {
            result.add(createUnwrapAction(each.getSecond(), each.getFirst(), editor, project));
        }

        return result;
    }

    @Nullable
    @RequiredReadAction
    private static UnwrapDescriptor getUnwrapDescription(PsiFile file) {
        return ContainerUtil.getFirstItem(UnwrapDescriptor.forLanguage(file.getLanguage()));
    }

    private AnAction createUnwrapAction(Unwrapper u, PsiElement el, Editor ed, Project p) {
        return new MyUnwrapAction(p, ed, u, el);
    }

    @RequiredUIAccess
    protected void selectOption(List<AnAction> options, Editor editor, PsiFile file) {
        if (options.isEmpty()) {
            return;
        }

        if (!getUnwrapDescription(file).showOptionsDialog() || Application.get().isUnitTestMode()) {
            options.get(0).actionPerformed(null);
            return;
        }

        showPopup(options, editor);
    }

    private void showPopup(final List<AnAction> options, Editor editor) {
        final ScopeHighlighter highlighter = new ScopeHighlighter(editor);

        DefaultListModel m = new DefaultListModel();
        for (AnAction a : options) {
            m.addElement(((MyUnwrapAction)a).getName());
        }

        final JList list = new JBList(m);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.setVisibleRowCount(options.size());

        list.addListSelectionListener(e -> {
            int index = list.getSelectedIndex();
            if (index < 0) {
                return;
            }

            MyUnwrapAction a = (MyUnwrapAction)options.get(index);

            List<PsiElement> toExtract = new ArrayList<>();
            PsiElement wholeRange = a.collectAffectedElements(toExtract);
            highlighter.highlight(wholeRange, toExtract);
        });

        PopupChooserBuilder builder = new PopupChooserBuilder<>(list);
        builder.setTitle(CodeInsightLocalize.unwrapPopupTitle().get())
            .setMovable(false)
            .setResizable(false)
            .setRequestFocus(true)
            .setItemChoosenCallback(() -> {
                MyUnwrapAction a = (MyUnwrapAction)options.get(list.getSelectedIndex());
                a.actionPerformed(null);
            })
            .addListener(new JBPopupAdapter() {
                @Override
                public void onClosed(LightweightWindowEvent event) {
                    highlighter.dropHighlight();
                }
            });

        JBPopup popup = builder.createPopup();
        editor.showPopupInBestPositionFor(popup);
    }

    private static class MyUnwrapAction extends AnAction {
        private static final Key<Integer> CARET_POS_KEY = new Key<>("UNWRAP_HANDLER_CARET_POSITION");

        private final Project myProject;
        private final Editor myEditor;
        private final Unwrapper myUnwrapper;
        private final PsiElement myElement;

        public MyUnwrapAction(Project project, Editor editor, Unwrapper unwrapper, PsiElement element) {
            super(unwrapper.getDescription(element));
            myProject = project;
            myEditor = editor;
            myUnwrapper = unwrapper;
            myElement = element;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            final PsiFile file = myElement.getContainingFile();
            if (!FileModificationService.getInstance().prepareFileForWrite(file)) {
                return;
            }

            CommandProcessor.getInstance().newCommand(() -> {
                    try {
                        UnwrapDescriptor d = getUnwrapDescription(file);
                        if (d.shouldTryToRestoreCaretPosition()) {
                            saveCaretPosition(file);
                        }
                        int scrollOffset = myEditor.getScrollingModel().getVerticalScrollOffset();

                        List<PsiElement> extractedElements = myUnwrapper.unwrap(myEditor, myElement);

                        if (d.shouldTryToRestoreCaretPosition()) {
                            restoreCaretPosition(file);
                        }
                        myEditor.getScrollingModel().scrollVertically(scrollOffset);

                        highlightExtractedElements(extractedElements);
                    }
                    catch (IncorrectOperationException ex) {
                        throw new RuntimeException(ex);
                    }
                })
                .withProject(myProject)
                .withGroupId(myEditor.getDocument())
                .executeInWriteAction();
        }

        @RequiredReadAction
        private void saveCaretPosition(PsiFile file) {
            int offset = myEditor.getCaretModel().getOffset();
            PsiElement el = file.findElementAt(offset);

            int innerOffset = offset - el.getTextOffset();
            el.putCopyableUserData(CARET_POS_KEY, innerOffset);
        }

        private void restoreCaretPosition(final PsiFile file) {
            ((TreeElement)file.getNode()).acceptTree(new RecursiveTreeElementWalkingVisitor() {
                @Override
                protected void visitNode(TreeElement element) {
                    PsiElement el = element.getPsi();
                    Integer offset = el.getCopyableUserData(CARET_POS_KEY);

                    // continue;
                    if (offset != null) {
                        myEditor.getCaretModel().moveToOffset(el.getTextOffset() + offset);
                        el.putCopyableUserData(CARET_POS_KEY, null);
                        return;
                    }
                    super.visitNode(element);
                }
            });
        }

        @RequiredReadAction
        private void highlightExtractedElements(final List<PsiElement> extractedElements) {
            for (PsiElement each : extractedElements) {
                HighlightManager.getInstance(myProject).addRangeHighlight(
                    myEditor,
                    each.getTextOffset(),
                    each.getTextOffset() + each.getTextLength(),
                    UnwrapHelper.getTestAttributesForExtract(),
                    false,
                    true,
                    null
                );
            }
        }

        public String getName() {
            return myUnwrapper.getDescription(myElement);
        }

        public PsiElement collectAffectedElements(List<PsiElement> toExtract) {
            return myUnwrapper.collectAffectedElements(myElement, toExtract);
        }
    }
}
