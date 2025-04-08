// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.generation.surroundWith;

import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.SelectionModel;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeInsight.template.impl.SurroundWithTemplateHandler;
import consulo.ide.impl.idea.lang.folding.CustomFoldingSurroundDescriptor;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.refactoring.rename.inplace.InplaceRefactoring;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.surroundWith.SurroundWithRangeAdjuster;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.editor.template.TemplateManager;
import consulo.language.localize.LanguageLocalize;
import consulo.language.psi.PsiCompiledElement;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.AnSeparator;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.undoRedo.CommandProcessor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public class SurroundWithHandler implements CodeInsightActionHandler {
    public static final TextRange CARET_IS_OK = new TextRange(0, 0);

    @Override
    @RequiredUIAccess
    public void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        invoke(project, editor, file, null);
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @RequiredUIAccess
    public static void invoke(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file, Surrounder surrounder) {
        if (!EditorModificationUtil.checkModificationAllowed(editor)) {
            return;
        }
        if (file instanceof PsiCompiledElement) {
            HintManager.getInstance().showErrorHint(editor, LanguageLocalize.hintTextCanTModifyDecompiledCode().get());
            return;
        }

        List<AnAction> applicable = buildSurroundActions(project, editor, file, surrounder);
        if (applicable != null) {
            showPopup(editor, applicable);
        }
        else if (!project.getApplication().isUnitTestMode()) {
            HintManager.getInstance().showErrorHint(editor, LanguageLocalize.hintTextCouldnTFindSurround().get());
        }
    }

    @Nullable
    @RequiredUIAccess
    public static List<AnAction> buildSurroundActions(Project project, Editor editor, PsiFile file, @Nullable Surrounder surrounder) {
        SelectionModel selectionModel = editor.getSelectionModel();
        boolean hasSelection = selectionModel.hasSelection();
        if (!hasSelection) {
            selectLogicalLineContentsAtCaret(editor);
        }
        int startOffset = selectionModel.getSelectionStart();
        int endOffset = selectionModel.getSelectionEnd();

        PsiElement element1 = file.findElementAt(startOffset);
        PsiElement element2 = file.findElementAt(endOffset - 1);

        if (element1 == null || element2 == null) {
            return null;
        }

        TextRange textRange = new TextRange(startOffset, endOffset);
        for (SurroundWithRangeAdjuster adjuster : SurroundWithRangeAdjuster.EP_NAME.getExtensionList()) {
            textRange = adjuster.adjustSurroundWithRange(file, textRange, hasSelection);
            if (textRange == null) {
                return null;
            }
        }
        startOffset = textRange.getStartOffset();
        endOffset = textRange.getEndOffset();
        element1 = file.findElementAt(startOffset);

        Language baseLanguage = file.getViewProvider().getBaseLanguage();
        assert element1 != null;
        Language l = element1.getParent().getLanguage();

        List<SurroundDescriptor> surroundDescriptors = new ArrayList<>(SurroundDescriptor.forLanguage(l));
        if (l != baseLanguage) {
            surroundDescriptors.addAll(SurroundDescriptor.forLanguage(baseLanguage));
        }
        surroundDescriptors.add(CustomFoldingSurroundDescriptor.INSTANCE);

        int exclusiveCount = 0;
        List<SurroundDescriptor> exclusiveSurroundDescriptors = new ArrayList<>();
        for (SurroundDescriptor sd : surroundDescriptors) {
            if (sd.isExclusive()) {
                exclusiveCount++;
                exclusiveSurroundDescriptors.add(sd);
            }
        }

        if (exclusiveCount > 0) {
            surroundDescriptors = exclusiveSurroundDescriptors;
        }

        if (surrounder != null) {
            invokeSurrounderInTests(project, editor, file, surrounder, startOffset, endOffset, surroundDescriptors);
            return null;
        }

        Map<Surrounder, PsiElement[]> surrounders = new LinkedHashMap<>();
        for (SurroundDescriptor descriptor : surroundDescriptors) {
            PsiElement[] elements = descriptor.getElementsToSurround(file, startOffset, endOffset);
            if (elements.length > 0) {
                for (PsiElement element : elements) {
                    assert element != null : "descriptor " + descriptor + " returned null element";
                    assert element.isValid() : descriptor;
                }
                for (Surrounder s : descriptor.getSurrounders()) {
                    surrounders.put(s, elements);
                }
            }
        }
        return doBuildSurroundActions(project, editor, file, surrounders);
    }

    public static void selectLogicalLineContentsAtCaret(Editor editor) {
        int caretOffset = editor.getCaretModel().getOffset();
        Document document = editor.getDocument();
        CharSequence text = document.getImmutableCharSequence();
        editor.getSelectionModel().setSelection(
            CharArrayUtil.shiftForward(text, DocumentUtil.getLineStartOffset(caretOffset, document), " \t"),
            CharArrayUtil.shiftBackward(text, DocumentUtil.getLineEndOffset(caretOffset, document) - 1, " \t") + 1
        );
    }

    @RequiredUIAccess
    private static void invokeSurrounderInTests(
        Project project,
        Editor editor,
        PsiFile file,
        Surrounder surrounder,
        int startOffset,
        int endOffset, List<? extends SurroundDescriptor> surroundDescriptors
    ) {
        assert project.getApplication().isUnitTestMode();
        for (SurroundDescriptor descriptor : surroundDescriptors) {
            PsiElement[] elements = descriptor.getElementsToSurround(file, startOffset, endOffset);
            if (elements.length > 0) {
                for (Surrounder descriptorSurrounder : descriptor.getSurrounders()) {
                    if (surrounder.getClass().equals(descriptorSurrounder.getClass())) {
                        CommandProcessor.getInstance().newCommand()
                            .project(project)
                            .inWriteAction()
                            .run(() -> doSurround(project, editor, surrounder, elements));
                        return;
                    }
                }
            }
        }
    }

    private static void showPopup(Editor editor, List<AnAction> applicable) {
        DataContext context = DataManager.getInstance().getDataContext(editor.getContentComponent());
        JBPopupFactory.ActionSelectionAid mnemonics = JBPopupFactory.ActionSelectionAid.MNEMONICS;
        DefaultActionGroup group = new DefaultActionGroup(applicable.toArray(AnAction.EMPTY_ARRAY));
        ListPopup popup = JBPopupFactory.getInstance()
            .createActionGroupPopup(CodeInsightLocalize.surroundWithChooserTitle().get(), group, context, mnemonics, true);

        editor.showPopupInBestPositionFor(popup);
    }

    @RequiredUIAccess
    static void doSurround(Project project, Editor editor, Surrounder surrounder, PsiElement[] elements) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        int col = editor.getCaretModel().getLogicalPosition().column;
        int line = editor.getCaretModel().getLogicalPosition().line;
        if (!editor.getCaretModel().supportsMultipleCarets()) {
            LogicalPosition pos = new LogicalPosition(0, 0);
            editor.getCaretModel().moveToLogicalPosition(pos);
        }
        TextRange range = surrounder.surroundElements(project, editor, elements);
        if (range != CARET_IS_OK) {
            if (TemplateManager.getInstance(project).getActiveTemplate(editor) == null
                && InplaceRefactoring.getActiveInplaceRenamer(editor) == null) {
                LogicalPosition pos1 = new LogicalPosition(line, col);
                editor.getCaretModel().moveToLogicalPosition(pos1);
            }
            if (range != null) {
                int offset = range.getStartOffset();
                editor.getCaretModel().removeSecondaryCarets();
                editor.getCaretModel().moveToOffset(offset);
                editor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE);
                editor.getSelectionModel().setSelection(range.getStartOffset(), range.getEndOffset());
            }
        }
    }

    @Nullable
    private static List<AnAction> doBuildSurroundActions(
        Project project,
        Editor editor,
        PsiFile file,
        Map<Surrounder, PsiElement[]> surrounders
    ) {
        List<AnAction> applicable = new ArrayList<>();

        Set<Character> usedMnemonicsSet = new HashSet<>();

        int index = 0;
        for (Map.Entry<Surrounder, PsiElement[]> entry : surrounders.entrySet()) {
            Surrounder surrounder = entry.getKey();
            PsiElement[] elements = entry.getValue();
            if (surrounder.isApplicable(elements)) {
                char mnemonic;
                if (index < 9) {
                    mnemonic = (char)('0' + index + 1);
                }
                else if (index == 9) {
                    mnemonic = '0';
                }
                else {
                    mnemonic = (char)('A' + index - 10);
                }
                index++;
                usedMnemonicsSet.add(Character.toUpperCase(mnemonic));
                applicable.add(new InvokeSurrounderAction(surrounder, project, editor, elements, mnemonic));
            }
        }

        List<AnAction> templateGroup = SurroundWithTemplateHandler.createActionGroup(editor, file, usedMnemonicsSet);
        if (!templateGroup.isEmpty()) {
            applicable.add(new AnSeparator(IdeLocalize.actionAnonymousTextLiveTemplates()));
            applicable.addAll(templateGroup);
            applicable.add(AnSeparator.getInstance());
            applicable.add(new ConfigureTemplatesAction());
        }
        return applicable.isEmpty() ? null : applicable;
    }

    private static class InvokeSurrounderAction extends AnAction {
        private final Surrounder mySurrounder;
        private final Project myProject;
        private final Editor myEditor;
        private final PsiElement[] myElements;

        InvokeSurrounderAction(Surrounder surrounder, Project project, Editor editor, PsiElement[] elements, char mnemonic) {
            super(LocalizeValue.localizeTODO(UIUtil.MNEMONIC + String.valueOf(mnemonic) + ". " + surrounder.getTemplateDescription()));
            mySurrounder = surrounder;
            myProject = project;
            myEditor = editor;
            myElements = elements;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(@Nonnull AnActionEvent e) {
            if (!FileDocumentManager.getInstance().requestWriting(myEditor.getDocument(), myProject)) {
                return;
            }

            Language language = Language.ANY;
            if (myElements != null && myElements.length != 0) {
                language = myElements[0].getLanguage();
            }
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .inWriteAction()
                .run(() -> doSurround(myProject, myEditor, mySurrounder, myElements));
            //SurroundWithLogger.logSurrounder(mySurrounder, language, myProject);
        }
    }

    private static final class ConfigureTemplatesAction extends AnAction {
        private ConfigureTemplatesAction() {
            super(ActionLocalize.actionConfiguretemplatesactionText());
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(@Nonnull AnActionEvent e) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(e.getData(Project.KEY), CodeInsightLocalize.templatesSettingsPageTitle().get());
        }
    }
}
