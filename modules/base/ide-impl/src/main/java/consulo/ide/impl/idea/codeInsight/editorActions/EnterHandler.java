// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Caret;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.action.EditorActionHandler;
import consulo.codeEditor.action.ExtensionEditorActionHandler;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataContextWrapper;
import consulo.dataContext.DataManager;
import consulo.document.Document;
import consulo.document.RangeMarker;
import consulo.document.util.DocumentUtil;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.codeStyle.CodeStyleFacade;
import consulo.ide.impl.idea.openapi.editor.EditorModificationUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.FormatterUtil;
import consulo.language.codeStyle.PostprocessReformattingAspect;
import consulo.language.codeStyle.lineIndent.LineIndentProvider;
import consulo.language.editor.CodeInsightSettings;
import consulo.language.editor.action.*;
import consulo.language.editor.documentation.CodeDocumentationProvider;
import consulo.language.editor.documentation.CompositeDocumentationProvider;
import consulo.language.editor.documentation.DocumentationProvider;
import consulo.language.editor.documentation.LanguageDocumentationProvider;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.lexer.Lexer;
import consulo.language.parser.ParserDefinition;
import consulo.language.psi.*;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.DumbService;
import consulo.project.Project;
import consulo.ui.ex.action.IdeActions;
import consulo.undoRedo.CommandProcessor;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.lang.StringUtil;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

@ExtensionImpl(id = "editorEnter")
public class EnterHandler extends BaseEnterHandler implements ExtensionEditorActionHandler {
    private static final Logger LOG = Logger.getInstance(EnterHandler.class);

    private final static Key<Language> CONTEXT_LANGUAGE = Key.create("EnterHandler.Language");
    private EditorActionHandler myOriginalHandler;

    public EnterHandler() {
        super(true);
    }

    @Override
    public void init(@Nullable EditorActionHandler originalHandler) {
        myOriginalHandler = originalHandler;
    }

    @Nonnull
    @Override
    public String getActionId() {
        return IdeActions.ACTION_EDITOR_ENTER;
    }

    @Override
    public boolean isEnabledForCaret(@Nonnull Editor editor, @Nonnull Caret caret, DataContext dataContext) {
        return myOriginalHandler.isEnabled(editor, caret, dataContext);
    }

    @Override
    @RequiredWriteAction
    public void executeWriteAction(Editor editor, Caret caret, DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        if (project != null && !project.isDefault()) {
            PostprocessReformattingAspect.getInstance(project)
                .disablePostprocessFormattingInside(() -> executeWriteActionInner(
                    editor,
                    caret,
                    getExtendedContext(dataContext, project, caret),
                    project
                ));
        }
        else {
            executeWriteActionInner(editor, caret, dataContext, project);
        }
    }

    @RequiredReadAction
    private void executeWriteActionInner(Editor editor, Caret caret, DataContext dataContext, Project project) {
        CodeInsightSettings settings = CodeInsightSettings.getInstance();
        if (project == null) {
            myOriginalHandler.execute(editor, caret, dataContext);
            return;
        }
        Document document = editor.getDocument();
        PsiFile file = PsiUtilBase.getPsiFileInEditor(editor, project);

        if (file == null) {
            myOriginalHandler.execute(editor, caret, dataContext);
            return;
        }

        CommandProcessor.getInstance().setCurrentCommandName(CodeInsightLocalize.commandNameTyping().get());

        EditorModificationUtil.deleteSelectedText(editor);

        int caretOffset = editor.getCaretModel().getOffset();
        CharSequence text = document.getCharsSequence();
        int length = document.getTextLength();
        if (caretOffset < length && text.charAt(caretOffset) != '\n') {
            int offset1 = CharArrayUtil.shiftBackward(text, caretOffset, " \t");
            if (offset1 < 0 || text.charAt(offset1) == '\n') {
                int offset2 = CharArrayUtil.shiftForward(text, offset1 + 1, " \t");
                boolean isEmptyLine = offset2 >= length || text.charAt(offset2) == '\n';
                if (!isEmptyLine) { // we are in leading spaces of a non-empty line
                    myOriginalHandler.execute(editor, caret, dataContext);
                    return;
                }
            }
        }

        boolean forceIndent = false;
        boolean forceSkipIndent = false;
        SimpleReference<Integer> caretOffsetRef = new SimpleReference<>(caretOffset);
        SimpleReference<Integer> caretAdvanceRef = new SimpleReference<>(0);

        for (EnterHandlerDelegate delegate : Application.get().getExtensionPoint(EnterHandlerDelegate.class)) {
            EnterHandlerDelegate.Result result =
                delegate.preprocessEnter(file, editor, caretOffsetRef, caretAdvanceRef, dataContext, myOriginalHandler);
            if (caretOffsetRef.get() > document.getTextLength()) {
                throw new AssertionError("Wrong caret offset change by " + delegate);
            }

            if (result == EnterHandlerDelegate.Result.Stop) {
                return;
            }
            if (result != EnterHandlerDelegate.Result.Continue) {
                if (result == EnterHandlerDelegate.Result.DefaultForceIndent) {
                    forceIndent = true;
                }
                else if (result == EnterHandlerDelegate.Result.DefaultSkipIndent) {
                    forceSkipIndent = true;
                }
                break;
            }
        }

        text = document.getCharsSequence();   // update after changes done in preprocessEnter()
        caretOffset = caretOffsetRef.get();
        boolean isFirstColumn = caretOffset == 0 || text.charAt(caretOffset - 1) == '\n';
        boolean insertSpace =
            !isFirstColumn && !(caretOffset >= text.length() || text.charAt(caretOffset) == ' ' || text.charAt(caretOffset) == '\t');
        editor.getCaretModel().moveToOffset(caretOffset);
        myOriginalHandler.execute(editor, caret, dataContext);
        if (!editor.isInsertMode() || forceSkipIndent) {
            return;
        }

        if (settings.SMART_INDENT_ON_ENTER || forceIndent) {
            caretOffset += 1;
            caretOffset = CharArrayUtil.shiftForward(editor.getDocument().getCharsSequence(), caretOffset, " \t");
            if (DocumentUtil.isAtLineEnd(caretOffset, document)) {
                caretOffset = editor.getCaretModel().getOffset();
            }
        }
        else {
            caretOffset = editor.getCaretModel().getOffset();
        }

        DoEnterAction action =
            new DoEnterAction(file, editor, document, dataContext, caretOffset, !insertSpace, caretAdvanceRef.get(), project);
        action.setForceIndent(forceIndent);
        action.run();
        for (EnterHandlerDelegate delegate : Application.get().getExtensionPoint(EnterHandlerDelegate.class)) {
            if (delegate.postProcessEnter(file, editor, dataContext) == EnterHandlerDelegate.Result.Stop) {
                break;
            }
        }

        if (settings.SMART_INDENT_ON_ENTER && action.isIndentAdjustmentNeeded()) {
            CodeStyleManager.getInstance(project).scheduleIndentAdjustment(document, editor.getCaretModel().getOffset());
        }
    }

    @Nonnull
    private static DataContext getExtendedContext(@Nonnull DataContext originalContext, @Nonnull Project project, @Nonnull Caret caret) {
        DataContext context = originalContext instanceof UserDataHolder ? originalContext : new DataContextWrapper(originalContext);
        ((UserDataHolder)context).putUserData(CONTEXT_LANGUAGE, PsiUtilBase.getLanguageInEditor(caret, project));
        return context;
    }

    @RequiredReadAction
    public static boolean isCommentComplete(PsiComment comment, CodeDocumentationAwareCommenter commenter, Editor editor) {
        for (CommentCompleteHandler handler : CommentCompleteHandler.EP_NAME.getExtensionList()) {
            if (handler.isApplicable(comment, commenter)) {
                return handler.isCommentComplete(comment, commenter, editor);
            }
        }

        String commentText = comment.getText();
        boolean docComment = isDocComment(comment, commenter);
        String expectedCommentEnd = docComment ? commenter.getDocumentationCommentSuffix() : commenter.getBlockCommentSuffix();
        if (!commentText.endsWith(expectedCommentEnd)) {
            return false;
        }

        PsiFile containingFile = comment.getContainingFile();
        Language language = containingFile.getLanguage();
        ParserDefinition parserDefinition = ParserDefinition.forLanguage(language);
        if (parserDefinition == null) {
            return true;
        }
        Lexer lexer = parserDefinition.createLexer(containingFile.getLanguageVersion());
        String commentPrefix = docComment ? commenter.getDocumentationCommentPrefix() : commenter.getBlockCommentPrefix();
        lexer.start(commentText, commentPrefix == null ? 0 : commentPrefix.length(), commentText.length());
        QuoteHandler fileTypeHandler = TypedHandler.getQuoteHandler(containingFile, editor);
        JavaLikeQuoteHandler javaLikeQuoteHandler = fileTypeHandler instanceof JavaLikeQuoteHandler handler ? handler : null;

        while (true) {
            IElementType tokenType = lexer.getTokenType();
            if (tokenType == null) {
                return false;
            }

            if (javaLikeQuoteHandler != null && javaLikeQuoteHandler.getStringTokenTypes() != null
                && javaLikeQuoteHandler.getStringTokenTypes().contains(tokenType)) {
                String text = commentText.substring(lexer.getTokenStart(), lexer.getTokenEnd());
                int endOffset = comment.getTextRange().getEndOffset();

                if (text.endsWith(expectedCommentEnd)
                    && endOffset < containingFile.getTextLength()
                    && containingFile.getText().charAt(endOffset) == '\n') {
                    return true;
                }
            }
            if (tokenType == commenter.getDocumentationCommentTokenType() || tokenType == commenter.getBlockCommentTokenType()) {
                return false;
            }
            if (tokenType == commenter.getLineCommentTokenType() && lexer.getTokenText().contains(commentPrefix)) {
                return false;
            }
            if (lexer.getTokenEnd() == commentText.length()) {
                if (tokenType == commenter.getLineCommentTokenType()) {
                    String prefix = commenter.getLineCommentPrefix();
                    lexer.start(commentText, lexer.getTokenStart() + (prefix == null ? 0 : prefix.length()), commentText.length());
                    lexer.advance();
                    continue;
                }
                else if (isInvalidPsi(comment)) {
                    return false;
                }
                return true;
            }
            lexer.advance();
        }
    }

    /**
     * There is a following possible use-case:
     * <pre>
     * <ul>
     *   <li>
     *     <b>Particular document has valid text:</b>
     *     <pre>
     *       [caret]
     *       class A {
     *           int foo() {
     *             return 1 *&#47;*comment*&#47; 1;
     *           }
     *       }
     *     </pre>
     *   </li>
     *   <li>
     *     <b>The user starts comment (inserts comment start symbols):</b>
     *     <pre>
     *       &#47;**[caret]
     *       class A {
     *           int foo() {
     *             return 1 *&#47;*comment*&#47; 1;
     *           }
     *       }
     *     </pre>
     *   </li>
     *   <li>The user presses {@code 'enter'};</li>
     * </ul>
     * </pre>
     * We want to understand that doc comment is incomplete now, i.e. don't want to consider '*&#47;' before
     * '*comment*&#47; 1;' as comment end. Current approach is to check if next PSI sibling to the current PSI comment is invalid.
     * This method allows to perform such an examination.
     */
    @RequiredReadAction
    private static boolean isInvalidPsi(@Nonnull PsiElement base) {
        for (PsiElement current = base.getNextSibling(); current != null; current = current.getNextSibling()) {
            if (current instanceof PsiErrorElement) {
                return true;
            }
        }
        return base.getPrevSibling() instanceof PsiErrorElement;
    }

    private static boolean isDocComment(PsiElement element, CodeDocumentationAwareCommenter commenter) {
        return element instanceof PsiComment comment && commenter.isDocumentationComment(comment);
    }

    /**
     * Adjusts indentation of the line with {@code offset} in {@code document}.
     *
     * @param language used for code style extraction
     * @param document for indent adjustment
     * @param editor   used for code style extraction
     * @param offset   in {@code document} for indent adjustment
     * @return new offset in the {@code document} after commit-free indent adjustment or
     * {@code -1} if commit-free indent adjustment is unavailable in position.
     */
    public static int adjustLineIndentNoCommit(Language language, @Nonnull Document document, @Nonnull Editor editor, int offset) {
        CharSequence docChars = document.getCharsSequence();
        int indentStart = CharArrayUtil.shiftBackwardUntil(docChars, offset - 1, "\n") + 1;
        int indentEnd = CharArrayUtil.shiftForward(docChars, indentStart, " \t");
        String newIndent = CodeStyleFacade.getInstance(editor.getProject()).getLineIndent(editor, language, offset, false);
        if (newIndent == null) {
            return -1;
        }
        if (newIndent == LineIndentProvider.DO_NOT_ADJUST) {
            return offset;
        }
        int delta = newIndent.length() - (indentEnd - indentStart);
        document.replaceString(indentStart, indentEnd, newIndent);
        return offset <= indentEnd ? (indentStart + newIndent.length()) : (offset + delta);
    }

    private static class DoEnterAction implements Runnable {

        private final DataContext myDataContext;
        private final PsiFile myFile;
        private int myOffset;
        private final Document myDocument;
        private final boolean myInsertSpace;
        private final Editor myEditor;
        private final Project myProject;
        private int myCaretAdvance;

        private boolean myForceIndent = false;
        private static final String LINE_SEPARATOR = "\n";

        private boolean myIsIndentAdjustmentNeeded = true;

        DoEnterAction(
            PsiFile file,
            Editor view,
            Document document,
            DataContext dataContext,
            int offset,
            boolean insertSpace,
            int caretAdvance,
            Project project
        ) {
            myEditor = view;
            myFile = file;
            myDataContext = dataContext;
            myOffset = offset;
            myDocument = document;
            myInsertSpace = insertSpace;
            myCaretAdvance = caretAdvance;
            myProject = project;
        }

        public void setForceIndent(boolean forceIndent) {
            myForceIndent = forceIndent;
        }

        @Override
        @RequiredWriteAction
        public void run() {
            CaretModel caretModel = myEditor.getCaretModel();
            try {
                CharSequence chars = myDocument.getCharsSequence();
                int i = CharArrayUtil.shiftBackwardUntil(chars, myOffset - 1, LINE_SEPARATOR) - 1;
                i = CharArrayUtil.shiftBackwardUntil(chars, i, LINE_SEPARATOR) + 1;
                if (i < 0) {
                    i = 0;
                }
                int lineStart = CharArrayUtil.shiftForward(chars, i, " \t");
                Language language = myDataContext instanceof UserDataHolder ? CONTEXT_LANGUAGE.get((UserDataHolder)myDataContext) : null;
                Commenter langCommenter = language != null ? Commenter.forLanguage(language) : null;
                CodeDocumentationUtil.CommentContext commentContext =
                    CodeDocumentationUtil.tryParseCommentContext(langCommenter, chars, lineStart);

                PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(getProject());
                if (commentContext.docStart) {
                    psiDocumentManager.commitDocument(myDocument);
                    PsiElement element = myFile.findElementAt(commentContext.lineStart);
                    String text = element.getText();
                    PsiElement parent = element.getParent();

                    if (text.equals(commentContext.commenter.getDocumentationCommentPrefix())
                        && isDocComment(parent, commentContext.commenter)
                        || text.startsWith(commentContext.commenter.getDocumentationCommentPrefix()) && element instanceof PsiComment) {
                        PsiComment comment = isDocComment(parent, commentContext.commenter) ? (PsiComment)parent : (PsiComment)element;
                        int commentEnd = comment.getTextRange().getEndOffset();

                        if (myOffset >= commentEnd) {
                            commentContext.docStart = false;
                        }
                        else if (isCommentComplete(comment, commentContext.commenter, myEditor)) {
                            commentContext.docAsterisk = myOffset < commentEnd
                                && CodeStyleManager.getInstance(getProject()).getDocCommentSettings(myFile).isLeadingAsteriskEnabled();
                            commentContext.docStart = false;
                        }
                        else {
                            generateJavadoc(commentContext.commenter);
                        }
                    }
                    else {
                        commentContext.docStart = false;
                    }
                }
                else if (commentContext.docAsterisk) {
                    psiDocumentManager.commitDocument(myDocument);
                    PsiElement element = myFile.findElementAt(commentContext.lineStart);
                    PsiComment comment = PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
                    if (comment == null || !isDocComment(comment, commentContext.commenter)) {
                        commentContext.docAsterisk = false; // don't process block comments
                    }
                }

                String indentInsideJavadoc = null;
                if (myOffset < myDocument.getTextLength()) {
                    int line = myDocument.getLineNumber(myOffset);
                    if (line > 0 && (commentContext.docAsterisk || commentContext.docStart)) {
                        indentInsideJavadoc =
                            CodeDocumentationUtil.getIndentInsideJavadoc(myDocument, myDocument.getLineStartOffset(line - 1));
                    }
                }

                if (commentContext.docAsterisk) {
                    commentContext.docAsterisk = insertDocAsterisk(
                        commentContext.lineStart,
                        commentContext.docAsterisk,
                        !StringUtil.isEmpty(indentInsideJavadoc),
                        commentContext.commenter
                    );
                }

                boolean docIndentApplied = false;
                CodeInsightSettings codeInsightSettings = CodeInsightSettings.getInstance();
                if (codeInsightSettings.SMART_INDENT_ON_ENTER || myForceIndent || commentContext.docStart || commentContext.docAsterisk) {
                    int offset = adjustLineIndentNoCommit(getLanguage(myDataContext), myDocument, myEditor, myOffset);
                    if (offset >= 0) {
                        myOffset = offset;
                        myIsIndentAdjustmentNeeded = false;
                    }

                    if (commentContext.docAsterisk && !StringUtil.isEmpty(indentInsideJavadoc) && myOffset < myDocument.getTextLength()) {
                        myDocument.insertString(myOffset + 1, indentInsideJavadoc);
                        myOffset += indentInsideJavadoc.length();
                        docIndentApplied = true;
                    }

                    if (myForceIndent && indentInsideJavadoc != null) {
                        int indentSize = CodeStyle.getSettings(myFile).getIndentSize(myFile.getFileType());
                        myDocument.insertString(myOffset + 1, StringUtil.repeatSymbol(' ', indentSize));
                        myCaretAdvance += indentSize;
                    }
                }

                if ((commentContext.docAsterisk || commentContext.docStart) && !docIndentApplied) {
                    if (myInsertSpace) {
                        if (myOffset == myDocument.getTextLength()) {
                            myDocument.insertString(myOffset, " ");
                        }
                        myDocument.insertString(myOffset + 1, " ");
                    }

                    char c = myDocument.getCharsSequence().charAt(myOffset);
                    if (c != '\n') {
                        myOffset += 1;
                    }
                }

                if (commentContext.docAsterisk && !commentContext.docStart) {
                    myCaretAdvance += 1;
                }
            }
            catch (IncorrectOperationException e) {
                LOG.error(e);
            }

            myOffset = Math.min(myOffset, myDocument.getTextLength());
            caretModel.moveToOffset(myOffset);
            EditorModificationUtil.scrollToCaret(myEditor);
            myEditor.getSelectionModel().removeSelection();
            if (myCaretAdvance != 0) {
                LogicalPosition caretPosition = caretModel.getLogicalPosition();
                LogicalPosition pos = new LogicalPosition(caretPosition.line, caretPosition.column + myCaretAdvance);
                caretModel.moveToLogicalPosition(pos);
            }
        }

        @RequiredWriteAction
        private void generateJavadoc(CodeDocumentationAwareCommenter commenter) throws IncorrectOperationException {
            CodeInsightSettings settings = CodeInsightSettings.getInstance();
            StringBuilder buffer = new StringBuilder();
            String docCommentLinePrefix = commenter.getDocumentationCommentLinePrefix();
            if (docCommentLinePrefix == null) {
                return;
            }

            // There are at least two approaches for completing javadoc in case there is a text between current caret position and line end:
            //     1. Move that tail text below the javadoc. Use-case:
            //         Before:
            //             /**<caret>public void foo() {}
            //         After:
            //             /**
            //              */
            //             public void foo() {}
            //     2. Move the tail text inside the javadoc. Use-case:
            //          Before:
            //             /**This is <caret>javadoc description
            //          After:
            //             /** This is
            //              * javadoc description
            //              */
            // The later is most relevant when we have 'auto wrap when typing reaches right margin' option set, i.e. user starts javadoc
            // and types until right margin is reached. We want the wrapped text tail to be located inside javadoc and continue typing
            // inside it. So, we have a control flow branch below that does the trick.
            buffer.append(docCommentLinePrefix);
            Boolean autoWrapInProgress = DataManager.getInstance()
                .loadFromDataContext(myDataContext, AutoHardWrapHandler.AUTO_WRAP_LINE_IN_PROGRESS_KEY);
            if (Objects.equals(autoWrapInProgress, Boolean.TRUE)) {
                myDocument.insertString(myOffset, buffer);

                // We create new buffer here because the one referenced by current 'buffer' variable value may be already referenced at another
                // place (e.g. 'undo' processing stuff).
                buffer = new StringBuilder(LINE_SEPARATOR).append(commenter.getDocumentationCommentSuffix());
                int line = myDocument.getLineNumber(myOffset);
                myOffset = myDocument.getLineEndOffset(line);
            }
            else {
                buffer.append(LINE_SEPARATOR);
                buffer.append(commenter.getDocumentationCommentSuffix());
            }

            PsiComment comment = createComment(buffer, settings);
            if (comment == null) {
                return;
            }

            myOffset = comment.getTextRange().getStartOffset();
            CharSequence text = myDocument.getCharsSequence();
            myOffset = CharArrayUtil.shiftForwardUntil(text, myOffset, LINE_SEPARATOR);
            myOffset = CharArrayUtil.shiftForward(text, myOffset, LINE_SEPARATOR);
            myOffset = CharArrayUtil.shiftForwardUntil(text, myOffset, docCommentLinePrefix) + 1;
            removeTrailingSpaces(myDocument, myOffset);

            if (!CodeStyleManager.getInstance(getProject()).getDocCommentSettings(myFile).isLeadingAsteriskEnabled()) {
                LOG.assertTrue(CharArrayUtil.regionMatches(
                    myDocument.getCharsSequence(),
                    myOffset - docCommentLinePrefix.length(),
                    docCommentLinePrefix
                ));
                myDocument.deleteString(myOffset - docCommentLinePrefix.length(), myOffset);
                myOffset--;
            }
            else {
                myDocument.insertString(myOffset, " ");
                myOffset++;
            }

            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        }

        @Nullable
        @RequiredWriteAction
        private PsiComment createComment(CharSequence buffer, CodeInsightSettings settings) throws IncorrectOperationException {
            myDocument.insertString(myOffset, buffer);

            PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
            CodeStyleManager.getInstance(getProject()).adjustLineIndent(myFile, myOffset + buffer.length() - 2);

            PsiComment comment = PsiTreeUtil.getNonStrictParentOfType(myFile.findElementAt(myOffset), PsiComment.class);

            comment = createJavaDocStub(settings, comment, getProject());
            if (comment == null) {
                return null;
            }

            CodeStyleManager codeStyleManager = CodeStyleManager.getInstance(getProject());
            SimpleReference<PsiComment> commentRef = SimpleReference.create(comment);
            codeStyleManager.runWithDocCommentFormattingDisabled(myFile, () -> formatComment(commentRef, codeStyleManager));
            comment = commentRef.get();

            PsiElement next = comment.getNextSibling();
            if (next == null && comment.getParent().getClass() == comment.getClass()) {
                next = comment.getParent().getNextSibling(); // expanding chameleon comment produces comment under comment
            }
            if (next != null) {
                next = myFile.findElementAt(next.getTextRange().getStartOffset()); // maybe switch to another tree
            }
            if (next != null && (!FormatterUtil.containsWhiteSpacesOnly(next.getNode()) || !next.getText().contains(LINE_SEPARATOR))) {
                int lineBreakOffset = comment.getTextRange().getEndOffset();
                myDocument.insertString(lineBreakOffset, LINE_SEPARATOR);
                PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
                codeStyleManager.adjustLineIndent(myFile, lineBreakOffset + 1);
                comment = PsiTreeUtil.getNonStrictParentOfType(myFile.findElementAt(myOffset), PsiComment.class);
            }
            return comment;
        }

        @RequiredWriteAction
        private void formatComment(SimpleReference<PsiComment> commentRef, CodeStyleManager codeStyleManager) {
            PsiComment comment = commentRef.get();
            RangeMarker commentMarker =
                myDocument.createRangeMarker(comment.getTextRange().getStartOffset(), comment.getTextRange().getEndOffset());
            codeStyleManager.reformatNewlyAddedElement(comment.getNode().getTreeParent(), comment.getNode());
            commentRef.set(PsiTreeUtil.getNonStrictParentOfType(myFile.findElementAt(commentMarker.getStartOffset()), PsiComment.class));
            commentMarker.dispose();
        }

        @Nullable
        @RequiredReadAction
        private PsiComment createJavaDocStub(CodeInsightSettings settings, PsiComment comment, Project project) {
            if (settings.JAVADOC_STUB_ON_ENTER) {
                DocumentationProvider langDocumentationProvider =
                    LanguageDocumentationProvider.forLanguageComposite(comment.getParent().getLanguage());

                CodeDocumentationProvider docProvider;
                if (langDocumentationProvider instanceof CompositeDocumentationProvider compositeDocumentationProvider) {
                    docProvider = compositeDocumentationProvider.getFirstCodeDocumentationProvider();
                }
                else {
                    docProvider = langDocumentationProvider instanceof CodeDocumentationProvider codeDocumentationProvider
                        ? codeDocumentationProvider
                        : null;
                }

                if (docProvider != null) {
                    if (docProvider.findExistingDocComment(comment) != comment) {
                        return comment;
                    }
                    String docStub;

                    DumbService.getInstance(project).setAlternativeResolveEnabled(true);
                    try {
                        docStub = docProvider.generateDocumentationContentStub(comment);
                    }
                    finally {
                        DumbService.getInstance(project).setAlternativeResolveEnabled(false);
                    }

                    if (docStub != null && docStub.length() != 0) {
                        myOffset = CharArrayUtil.shiftForwardUntil(myDocument.getCharsSequence(), myOffset, LINE_SEPARATOR);
                        myOffset = CharArrayUtil.shiftForward(myDocument.getCharsSequence(), myOffset, LINE_SEPARATOR);
                        myDocument.insertString(myOffset, docStub);
                    }
                }

                PsiDocumentManager.getInstance(project).commitAllDocuments();
                return PsiTreeUtil.getNonStrictParentOfType(myFile.findElementAt(myOffset), PsiComment.class);
            }
            return comment;
        }

        private Project getProject() {
            return myFile.getProject();
        }

        private static void removeTrailingSpaces(Document document, int startOffset) {
            int endOffset = startOffset;

            CharSequence charsSequence = document.getCharsSequence();

            for (int i = startOffset; i < charsSequence.length(); i++) {
                char c = charsSequence.charAt(i);
                endOffset = i;
                if (c == '\n') {
                    break;
                }
                if (c != ' ' && c != '\t') {
                    return;
                }
            }

            document.deleteString(startOffset, endOffset);
        }

        @RequiredReadAction
        private boolean insertDocAsterisk(
            int lineStart,
            boolean docAsterisk,
            boolean previousLineIndentUsed,
            CodeDocumentationAwareCommenter commenter
        ) {
            PsiDocumentManager.getInstance(myProject).commitDocument(myDocument);
            PsiElement atLineStart = myFile.findElementAt(lineStart);
            if (atLineStart == null) {
                return false;
            }

            String linePrefix = commenter.getDocumentationCommentLinePrefix();
            String docPrefix = commenter.getDocumentationCommentPrefix();

            String text = atLineStart.getText();
            TextRange textRange = atLineStart.getTextRange();

            if (text.equals(linePrefix) || text.equals(docPrefix)
                || docPrefix != null && text.regionMatches(lineStart - textRange.getStartOffset(), docPrefix, 0, docPrefix.length())
                || linePrefix != null && text.regionMatches(lineStart - textRange.getStartOffset(), linePrefix, 0, linePrefix.length())) {
                PsiElement element = myFile.findElementAt(myOffset);
                if (element == null) {
                    return false;
                }

                PsiComment comment = element instanceof PsiComment psiComment
                    ? psiComment
                    : PsiTreeUtil.getParentOfType(element, PsiComment.class, false);
                if (comment != null) {
                    int commentEnd = comment.getTextRange().getEndOffset();
                    if (myOffset >= commentEnd || lineStart < comment.getTextOffset()) {
                        docAsterisk = false;
                    }
                    else {
                        removeTrailingSpaces(myDocument, myOffset);
                        String toInsert = previousLineIndentUsed
                            ? "*"
                            : CodeDocumentationUtil.createDocCommentLine("", myFile, commenter);
                        myDocument.insertString(myOffset, toInsert);
                        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
                    }
                }
                else {
                    docAsterisk = false;
                }
            }
            else {
                docAsterisk = false;
            }
            return docAsterisk;
        }

        public boolean isIndentAdjustmentNeeded() {
            return myIsIndentAdjustmentNeeded;
        }
    }

    @Nullable
    public static Language getLanguage(@Nonnull DataContext dataContext) {
        if (dataContext instanceof UserDataHolder userDataHolder) {
            return CONTEXT_LANGUAGE.get(userDataHolder);
        }
        return null;
    }
}
