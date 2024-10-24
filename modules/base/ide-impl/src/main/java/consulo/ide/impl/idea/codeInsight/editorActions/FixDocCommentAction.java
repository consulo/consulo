// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.application.Application;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.action.EditorAction;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.CodeStyleSettings;
import consulo.language.codeStyle.DocCommentSettings;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.documentation.*;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

/**
 * Creates documentation comment for the current context if it's not created yet (e.g. the caret is inside a method which
 * doesn't have a doc comment).
 * <p>
 * Updates existing documentation comment if necessary if the one exists. E.g. we've changed method signature and want to remove all
 * outdated parameters and create stubs for the new ones.
 *
 * @author Denis Zhdanov
 * @since 9/20/12 10:15 AM
 */
public class FixDocCommentAction extends EditorAction {
    @Nonnull
    @NonNls
    public static final String ACTION_ID = "FixDocComment";

    public FixDocCommentAction() {
        super(new MyHandler());
    }

    private static final class MyHandler extends EditorActionHandler {

        @Override
        public void doExecute(@Nonnull Editor editor, @Nullable Caret caret, DataContext dataContext) {
            Project project = dataContext.getData(Project.KEY);
            if (project == null) {
                return;
            }

            PsiFile psiFile = dataContext.getData(PsiFile.KEY);
            if (psiFile == null) {
                return;
            }

            process(psiFile, editor, project, editor.getCaretModel().getOffset());
        }
    }

    private static void process(@Nonnull final PsiFile file, @Nonnull final Editor editor, @Nonnull final Project project, int offset) {
        PsiElement elementAtOffset = file.findElementAt(offset);
        if (elementAtOffset == null || !FileModificationService.getInstance().preparePsiElementForWrite(elementAtOffset)) {
            return;
        }
        generateOrFixComment(elementAtOffset, project, editor);
    }

    /**
     * Generates comment if it's not exist or try to fix if exists
     *
     * @param element target element for which a comment should be generated
     * @param project current project
     * @param editor  target editor
     */
    public static void generateOrFixComment(
        @Nonnull final PsiElement element,
        @Nonnull final Project project,
        @Nonnull final Editor editor
    ) {
        Language language = element.getLanguage();
        final CodeDocumentationProvider docProvider;
        final DocumentationProvider langDocumentationProvider = LanguageDocumentationProvider.forLanguageComposite(language);
        if (langDocumentationProvider instanceof CompositeDocumentationProvider) {
            docProvider = ((CompositeDocumentationProvider)langDocumentationProvider).getFirstCodeDocumentationProvider();
        }
        else if (langDocumentationProvider instanceof CodeDocumentationProvider) {
            docProvider = (CodeDocumentationProvider)langDocumentationProvider;
        }
        else {
            docProvider = null;
        }
        if (docProvider == null) {
            return;
        }

        final Pair<PsiElement, PsiComment> pair = docProvider.parseContext(element);
        if (pair == null) {
            return;
        }

        Commenter c = Commenter.forLanguage(language);
        if (!(c instanceof CodeDocumentationAwareCommenter)) {
            return;
        }
        final CodeDocumentationAwareCommenter commenter = (CodeDocumentationAwareCommenter)c;
        final Runnable task;
        if (pair.second == null || pair.second.getTextRange().isEmpty()) {
            task = () -> generateComment(pair.first, editor, docProvider, commenter, project);
        }
        else {
            final DocCommentFixer fixer = DocCommentFixer.forLanguage(language);
            if (fixer == null) {
                return;
            }
            else {
                task = () -> fixer.fixComment(project, editor, pair.second);
            }
        }
        final Runnable command = () -> Application.get().runWriteAction(task);
        CommandProcessor.getInstance().executeCommand(project, command, "Fix documentation", null);
    }

    /**
     * Generates a comment if possible.
     * <p>
     * It's assumed that this method {@link PsiDocumentManager#commitDocument(Document) syncs} all PSI-document
     * changes during the processing.
     *
     * @param anchor    target element for which a comment should be generated
     * @param editor    target editor
     * @param commenter commenter to use
     * @param project   current project
     */
    private static void generateComment(
        @Nonnull PsiElement anchor,
        @Nonnull Editor editor,
        @Nonnull CodeDocumentationProvider documentationProvider,
        @Nonnull CodeDocumentationAwareCommenter commenter,
        @Nonnull Project project
    ) {
        Document document = editor.getDocument();
        int commentStartOffset = anchor.getTextRange().getStartOffset();
        int lineStartOffset = document.getLineStartOffset(document.getLineNumber(commentStartOffset));
        if (lineStartOffset > 0 && lineStartOffset < commentStartOffset) {
            // Example:
            //    void test1() {
            //    }
            //    void test2() {
            //       <offset>
            //    }
            // We want to insert the comment at the start of the line where 'test2()' is declared.
            int nonWhiteSpaceOffset = CharArrayUtil.shiftBackward(document.getCharsSequence(), commentStartOffset - 1, " \t");
            commentStartOffset = Math.max(nonWhiteSpaceOffset, lineStartOffset);
        }

        int commentBodyRelativeOffset = 0;
        int caretOffsetToSet = -1;
        StringBuilder buffer = new StringBuilder();
        String commentPrefix = commenter.getDocumentationCommentPrefix();
        if (commentPrefix != null) {
            buffer.append(commentPrefix).append("\n");
            commentBodyRelativeOffset += commentPrefix.length() + 1;
        }

        String linePrefix = commenter.getDocumentationCommentLinePrefix();
        if (linePrefix != null) {
            buffer.append(linePrefix);
            commentBodyRelativeOffset += linePrefix.length();
            caretOffsetToSet = commentStartOffset + commentBodyRelativeOffset;
        }
        buffer.append("\n");
        commentBodyRelativeOffset++;

        String commentSuffix = commenter.getDocumentationCommentSuffix();
        if (commentSuffix != null) {
            buffer.append(commentSuffix).append("\n");
        }

        if (buffer.length() <= 0) {
            return;
        }

        document.insertString(commentStartOffset, buffer);
        PsiDocumentManager docManager = PsiDocumentManager.getInstance(project);
        docManager.commitDocument(document);

        Pair<PsiElement, PsiComment> pair = documentationProvider.parseContext(anchor);
        if (pair == null || pair.second == null) {
            return;
        }

        String stub = documentationProvider.generateDocumentationContentStub(pair.second);
        CaretModel caretModel = editor.getCaretModel();
        if (stub != null) {
            int insertionOffset = commentStartOffset + commentBodyRelativeOffset;
            document.insertString(insertionOffset, stub);
            docManager.commitDocument(document);
            pair = documentationProvider.parseContext(anchor);
        }

        if (caretOffsetToSet >= 0) {
            caretModel.moveToOffset(caretOffsetToSet);
            editor.getSelectionModel().removeSelection();
        }

        if (pair == null || pair.second == null) {
            return;
        }

        int start = Math.min(calcStartReformatOffset(pair.first), calcStartReformatOffset(pair.second));
        int end = pair.second.getTextRange().getEndOffset();

        reformatCommentKeepingEmptyTags(anchor.getContainingFile(), project, start, end);
        editor.getCaretModel().moveToOffset(document.getLineEndOffset(document.getLineNumber(editor.getCaretModel().getOffset())));

        int caretOffset = caretModel.getOffset();
        if (caretOffset > 0 && caretOffset <= document.getTextLength()) {
            char c = document.getCharsSequence().charAt(caretOffset - 1);
            if (!StringUtil.isWhiteSpace(c)) {
                document.insertString(caretOffset, " ");
                caretModel.moveToOffset(caretOffset + 1);
            }
        }
    }

    private static void reformatCommentKeepingEmptyTags(@Nonnull PsiFile file, @Nonnull Project project, int start, int end) {
        CodeStyleSettings tempSettings = CodeStyle.getSettings(file).clone();
        LanguageCodeStyleSettingsProvider langProvider = LanguageCodeStyleSettingsProvider.forLanguage(file.getLanguage());
        if (langProvider != null) {
            DocCommentSettings docCommentSettings = langProvider.getDocCommentSettings(tempSettings);
            docCommentSettings.setRemoveEmptyTags(false);
        }
        CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(project);
        CodeStyle.doWithTemporarySettings(project, tempSettings, () -> codeStyleManager.reformatText(file, start, end));
    }

    private static int calcStartReformatOffset(@Nonnull PsiElement element) {
        int result = element.getTextRange().getStartOffset();
        for (PsiElement e = element.getPrevSibling(); e != null; e = e.getPrevSibling()) {
            if (e instanceof PsiWhiteSpace) {
                result = e.getTextRange().getStartOffset();
            }
            else {
                break;
            }
        }
        return result;
    }
}
