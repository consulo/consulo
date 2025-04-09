// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.ide.impl.psi.impl.source.codeStyle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.annotation.component.ServiceImpl;
import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.ScrollType;
import consulo.codeEditor.VisualPosition;
import consulo.component.ProcessCanceledException;
import consulo.document.Document;
import consulo.document.DocumentWindow;
import consulo.document.RangeMarker;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.formatting.FormatterEx;
import consulo.ide.impl.idea.formatting.FormatterImpl;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.ide.impl.idea.util.text.CharArrayUtil;
import consulo.ide.impl.psi.codeStyle.IndentOld;
import consulo.ide.impl.psi.impl.source.codeStyle.lineIndent.FormatterBasedIndentAdjuster;
import consulo.language.CodeDocumentationAwareCommenter;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.ast.TokenType;
import consulo.language.codeStyle.*;
import consulo.language.codeStyle.internal.CoreCodeStyleUtil;
import consulo.language.codeStyle.setting.LanguageCodeStyleSettingsProvider;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.file.FileViewProvider;
import consulo.language.impl.ast.RecursiveTreeElementWalkingVisitor;
import consulo.language.impl.ast.TreeElement;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.impl.psi.IndentHelper;
import consulo.language.impl.psi.SourceTreeToPsiMap;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.plain.ast.PlainTextTokenTypes;
import consulo.language.psi.*;
import consulo.language.util.IncorrectOperationException;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.function.ThrowableRunnable;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@Singleton
@ServiceImpl
public class CodeStyleManagerImpl extends CodeStyleManager implements FormattingModeAwareIndentAdjuster {
    private static final Logger LOG = Logger.getInstance(CodeStyleManagerImpl.class);

    private final ThreadLocal<FormattingMode> myCurrentFormattingMode = ThreadLocal.withInitial(() -> FormattingMode.REFORMAT);

    private final Project myProject;

    @Inject
    public CodeStyleManagerImpl(Project project) {
        myProject = project;
    }

    @Nonnull
    @Override
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public PsiElement reformat(@Nonnull PsiElement element) throws IncorrectOperationException {
        return reformat(element, false);
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public PsiElement reformat(@Nonnull PsiElement element, boolean canChangeWhiteSpacesOnly) throws IncorrectOperationException {
        CheckUtil.checkWritable(element);
        if (!SourceTreeToPsiMap.hasTreeElement(element)) {
            return element;
        }

        ASTNode treeElement = element.getNode();
        PsiFile file = element.getContainingFile();
        if (ExternalFormatProcessor.useExternalFormatter(file)) {
            return ExternalFormatProcessor.formatElement(element, element.getTextRange(), canChangeWhiteSpacesOnly);
        }

        PsiElement formatted = new CodeFormatterFacade(getSettings(file), element.getLanguage(), canChangeWhiteSpacesOnly)
            .processElement(treeElement)
            .getPsi();
        if (!canChangeWhiteSpacesOnly) {
            return postProcessElement(file, formatted);
        }
        return formatted;
    }

    @RequiredReadAction
    private static PsiElement postProcessElement(@Nonnull PsiFile file, @Nonnull PsiElement formatted) {
        PsiElement result = formatted;
        CodeStyleSettings settingsForFile = CodeStyle.getSettings(file);
        if (settingsForFile.FORMATTER_TAGS_ENABLED && formatted instanceof PsiFile) {
            postProcessEnabledRanges((PsiFile)formatted, formatted.getTextRange(), settingsForFile);
        }
        else {
            for (PostFormatProcessor postFormatProcessor : PostFormatProcessor.EP_NAME.getExtensionList()) {
                try {
                    result = postFormatProcessor.processElement(result, settingsForFile);
                }
                catch (ProcessCanceledException e) {
                    throw e;
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }
        }
        return result;
    }

    private static void postProcessText(@Nonnull PsiFile file, @Nonnull TextRange textRange) {
        if (!getSettings(file).FORMATTER_TAGS_ENABLED) {
            TextRange currentRange = textRange;
            for (PostFormatProcessor myPostFormatProcessor : PostFormatProcessor.EP_NAME.getExtensionList()) {
                currentRange = myPostFormatProcessor.processText(file, currentRange, getSettings(file));
            }
        }
        else {
            postProcessEnabledRanges(file, textRange, getSettings(file));
        }
    }

    @Override
    @RequiredUIAccess
    public PsiElement reformatRange(
        @Nonnull PsiElement element,
        int startOffset,
        int endOffset,
        boolean canChangeWhiteSpacesOnly
    ) throws IncorrectOperationException {
        return reformatRangeImpl(element, startOffset, endOffset, canChangeWhiteSpacesOnly);
    }

    @Override
    @RequiredUIAccess
    public PsiElement reformatRange(@Nonnull PsiElement element, int startOffset, int endOffset) throws IncorrectOperationException {
        return reformatRangeImpl(element, startOffset, endOffset, false);
    }

    private static void transformAllChildren(ASTNode file) {
        ((TreeElement)file).acceptTree(new RecursiveTreeElementWalkingVisitor() {
        });
    }


    @Override
    @RequiredWriteAction
    public void reformatText(@Nonnull PsiFile file, int startOffset, int endOffset) throws IncorrectOperationException {
        reformatText(file, Collections.singleton(new TextRange(startOffset, endOffset)));
    }

    @Override
    @RequiredWriteAction
    public void reformatText(@Nonnull PsiFile file, @Nonnull Collection<TextRange> ranges) throws IncorrectOperationException {
        reformatText(file, ranges, null);
    }

    @Override
    @RequiredWriteAction
    public void reformatTextWithContext(@Nonnull PsiFile file, @Nonnull ChangedRangesInfo info) throws IncorrectOperationException {
        FormatTextRanges formatRanges = new FormatTextRanges(info, ChangedRangesUtil.processChangedRanges(file, info));
        formatRanges.setExtendToContext(true);
        reformatText(file, formatRanges, null);
    }

    @RequiredWriteAction
    public void reformatText(
        @Nonnull PsiFile file,
        @Nonnull Collection<? extends TextRange> ranges,
        @Nullable Editor editor
    ) throws IncorrectOperationException {
        FormatTextRanges formatRanges = new FormatTextRanges();
        ranges.forEach((range) -> formatRanges.add(range, true));
        reformatText(file, formatRanges, editor);
    }

    @RequiredWriteAction
    private void reformatText(
        @Nonnull PsiFile file,
        @Nonnull FormatTextRanges ranges,
        @Nullable Editor editor
    ) throws IncorrectOperationException {
        if (ranges.isEmpty()) {
            return;
        }
        file.getApplication().assertWriteAccessAllowed();
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        CheckUtil.checkWritable(file);
        if (!SourceTreeToPsiMap.hasTreeElement(file)) {
            return;
        }

        ASTNode treeElement = SourceTreeToPsiMap.psiElementToTree(file);
        transformAllChildren(treeElement);

        LOG.assertTrue(file.isValid(), "File name: " + file.getName() + " , class: " + file.getClass().getSimpleName());

        if (editor == null) {
            editor = PsiUtilBase.findEditor(file);
        }

        CaretPositionKeeper caretKeeper = null;
        if (editor != null) {
            caretKeeper = new CaretPositionKeeper(editor, getSettings(file), file.getLanguage());
        }

        if (FormatterUtil.isFormatterCalledExplicitly()) {
            removeEndingWhiteSpaceFromEachRange(file, ranges);
        }

        formatRanges(
            file,
            ranges,
            ExternalFormatProcessor.useExternalFormatter(file)
                ? null  // do nothing, delegate the external formatting activity to post-processor
                : () -> {
                CodeFormatterFacade codeFormatter = new CodeFormatterFacade(getSettings(file), file.getLanguage());
                codeFormatter.processText(file, ranges, true);
            }
        );

        if (caretKeeper != null) {
            caretKeeper.restoreCaretPosition();
        }
    }

    @RequiredUIAccess
    public static void formatRanges(
        @Nonnull PsiFile file,
        @Nonnull FormatTextRanges ranges,
        @Nullable @RequiredUIAccess Runnable formatAction
    ) {
        SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(file.getProject());

        List<RangeFormatInfo> infos = new ArrayList<>();
        for (TextRange range : ranges.getTextRanges()) {
            PsiElement start = CoreCodeStyleUtil.findElementInTreeWithFormatterEnabled(file, range.getStartOffset());
            PsiElement end = CoreCodeStyleUtil.findElementInTreeWithFormatterEnabled(file, range.getEndOffset());
            if (start != null && !start.isValid()) {
                LOG.error("start=" + start + "; file=" + file);
            }
            if (end != null && !end.isValid()) {
                LOG.error("end=" + start + "; end=" + file);
            }
            boolean formatFromStart = range.getStartOffset() == 0;
            boolean formatToEnd = range.getEndOffset() == file.getTextLength();
            infos.add(new RangeFormatInfo(
                start == null ? null : smartPointerManager.createSmartPsiElementPointer(start),
                end == null ? null : smartPointerManager.createSmartPsiElementPointer(end),
                formatFromStart,
                formatToEnd
            ));
        }

        if (formatAction != null) {
            formatAction.run();
        }

        for (RangeFormatInfo info : infos) {
            PsiElement startElement = info.startPointer == null ? null : info.startPointer.getElement();
            PsiElement endElement = info.endPointer == null ? null : info.endPointer.getElement();
            if ((startElement != null || info.fromStart) && (endElement != null || info.toEnd)) {
                postProcessText(
                    file,
                    new TextRange(
                        info.fromStart ? 0 : startElement.getTextRange().getStartOffset(),
                        info.toEnd ? file.getTextLength() : endElement.getTextRange().getEndOffset()
                    )
                );
            }
            if (info.startPointer != null) {
                smartPointerManager.removePointer(info.startPointer);
            }
            if (info.endPointer != null) {
                smartPointerManager.removePointer(info.endPointer);
            }
        }
    }

    @RequiredReadAction
    private static void removeEndingWhiteSpaceFromEachRange(@Nonnull PsiFile file, @Nonnull FormatTextRanges ranges) {
        for (FormatTextRange formatRange : ranges.getRanges()) {
            TextRange range = formatRange.getTextRange();

            int rangeStart = range.getStartOffset();
            int rangeEnd = range.getEndOffset();

            PsiElement lastElementInRange = CoreCodeStyleUtil.findElementInTreeWithFormatterEnabled(file, rangeEnd);
            if (lastElementInRange instanceof PsiWhiteSpace && rangeStart < lastElementInRange.getTextRange().getStartOffset()) {
                PsiElement prev = lastElementInRange.getPrevSibling();
                if (prev != null) {
                    int newEnd = prev.getTextRange().getEndOffset();
                    formatRange.setTextRange(new TextRange(rangeStart, newEnd));
                }
            }
        }
    }

    @RequiredUIAccess
    private static PsiElement reformatRangeImpl(
        @Nonnull PsiElement element,
        int startOffset,
        int endOffset,
        boolean canChangeWhiteSpacesOnly
    ) throws IncorrectOperationException {
        LOG.assertTrue(element.isValid());
        CheckUtil.checkWritable(element);
        if (!SourceTreeToPsiMap.hasTreeElement(element)) {
            return element;
        }

        ASTNode treeElement = element.getNode();
        PsiFile file = element.getContainingFile();
        if (ExternalFormatProcessor.useExternalFormatter(file)) {
            return ExternalFormatProcessor.formatElement(element, TextRange.create(startOffset, endOffset), canChangeWhiteSpacesOnly);
        }

        CodeFormatterFacade codeFormatter = new CodeFormatterFacade(getSettings(file), element.getLanguage());
        PsiElement formatted = codeFormatter.processRange(treeElement, startOffset, endOffset).getPsi();
        return canChangeWhiteSpacesOnly ? formatted : postProcessElement(file, formatted);
    }


    @Override
    @RequiredWriteAction
    public void reformatNewlyAddedElement(@Nonnull ASTNode parent, @Nonnull ASTNode addedElement) throws IncorrectOperationException {
        LOG.assertTrue(addedElement.getTreeParent() == parent, "addedElement must be added to parent");

        PsiElement psiElement = parent.getPsi();

        PsiFile containingFile = psiElement.getContainingFile();
        FileViewProvider fileViewProvider = containingFile.getViewProvider();
        if (fileViewProvider instanceof MultiplePsiFilesPerDocumentFileViewProvider) {
            containingFile = fileViewProvider.getPsi(fileViewProvider.getBaseLanguage());
        }
        assert containingFile != null;

        TextRange textRange = addedElement.getTextRange();
        Document document = fileViewProvider.getDocument();
        if (document instanceof DocumentWindow documentWindow) {
            containingFile = InjectedLanguageManager.getInstance(containingFile.getProject()).getTopLevelFile(containingFile);
            textRange = documentWindow.injectedToHost(textRange);
        }

        FormattingModelBuilder builder = FormattingModelBuilder.forContext(containingFile);
        if (builder != null) {
            FormattingModel model =
                CoreFormatterUtil.buildModel(builder, containingFile, getSettings(containingFile), FormattingMode.REFORMAT);
            FormatterEx.getInstanceEx().formatAroundRange(model, getSettings(containingFile), containingFile, textRange);
        }

        adjustLineIndent(containingFile, textRange);
    }

    @Override
    public int adjustLineIndent(@Nonnull PsiFile file, int offset) throws IncorrectOperationException {
        return PostprocessReformattingAspect.getInstance(file.getProject())
            .disablePostprocessFormattingInside(() -> doAdjustLineIndentByOffset(
                file,
                offset,
                FormattingMode.ADJUST_INDENT
            ));
    }

    @Override
    public int adjustLineIndent(@Nonnull Document document, int offset, FormattingMode mode) {
        return PostprocessReformattingAspect.getInstance(getProject()).disablePostprocessFormattingInside(() -> {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
            documentManager.commitDocument(document);

            PsiFile file = documentManager.getPsiFile(document);
            if (file == null) {
                return offset;
            }

            return doAdjustLineIndentByOffset(file, offset, mode);
        });
    }

    @Override
    public int adjustLineIndent(@Nonnull Document document, int offset) {
        return adjustLineIndent(document, offset, FormattingMode.ADJUST_INDENT);
    }

    private int doAdjustLineIndentByOffset(@Nonnull PsiFile file, int offset, FormattingMode mode) {
        Integer result = new CodeStyleManagerRunnable<Integer>(this, mode) {
            @Override
            protected Integer doPerform(int offset, TextRange range) {
                return FormatterEx.getInstanceEx().adjustLineIndent(myModel, mySettings, myIndentOptions, offset, mySignificantRange);
            }

            @Override
            protected Integer computeValueInsidePlainComment(PsiFile file, int offset, Integer defaultValue) {
                return CharArrayUtil.shiftForward(file.getViewProvider().getContents(), offset, " \t");
            }

            @Override
            protected Integer adjustResultForInjected(Integer result, DocumentWindow documentWindow) {
                return result != null ? documentWindow.hostToInjected(result) : null;
            }
        }.perform(file, offset, null, null);

        return result != null ? result : offset;
    }

    @Override
    public void adjustLineIndent(@Nonnull PsiFile file, TextRange rangeToAdjust) throws IncorrectOperationException {
        new CodeStyleManagerRunnable<>(this, FormattingMode.ADJUST_INDENT) {
            @Override
            protected Object doPerform(int offset, TextRange range) {
                FormatterEx.getInstanceEx().adjustLineIndentsForRange(myModel, mySettings, myIndentOptions, range);
                return null;
            }
        }.perform(file, -1, rangeToAdjust, null);
    }

    @Override
    @Nullable
    public String getLineIndent(@Nonnull PsiFile file, int offset) {
        return getLineIndent(file, offset, FormattingMode.ADJUST_INDENT);
    }

    @Override
    @Nullable
    public String getLineIndent(@Nonnull PsiFile file, int offset, FormattingMode mode) {
        return new CodeStyleManagerRunnable<String>(this, mode) {
            @Override
            protected boolean useDocumentBaseFormattingModel() {
                return false;
            }

            @Override
            protected String doPerform(int offset, TextRange range) {
                return FormatterEx.getInstanceEx().getLineIndent(myModel, mySettings, myIndentOptions, offset, mySignificantRange);
            }
        }.perform(file, offset, null, null);
    }

    @Override
    @Nullable
    public String getLineIndent(@Nonnull Document document, int offset) {
        PsiFile file = PsiDocumentManager.getInstance(myProject).getPsiFile(document);
        if (file == null) {
            return "";
        }

        return getLineIndent(file, offset);
    }

    @Override
    @Deprecated
    @RequiredReadAction
    public boolean isLineToBeIndented(@Nonnull PsiFile file, int offset) {
        if (!SourceTreeToPsiMap.hasTreeElement(file)) {
            return false;
        }
        CharSequence chars = file.getViewProvider().getContents();
        int start = CharArrayUtil.shiftBackward(chars, offset - 1, " \t");
        if (start > 0 && chars.charAt(start) != '\n' && chars.charAt(start) != '\r') {
            return false;
        }
        int end = CharArrayUtil.shiftForward(chars, offset, " \t");
        if (end >= chars.length()) {
            return false;
        }
        ASTNode element = SourceTreeToPsiMap.psiElementToTree(CoreCodeStyleUtil.findElementInTreeWithFormatterEnabled(file, end));
        if (element == null) {
            return false;
        }
        if (element.getElementType() == TokenType.WHITE_SPACE) {
            return false;
        }
        if (element.getElementType() == PlainTextTokenTypes.PLAIN_TEXT) {
            return false;
        }
    /*
    if( element.getElementType() instanceof IJspElementType )
    {
      return false;
    }
    */
        return !getSettings(file).getCommonSettings(file.getLanguage()).KEEP_FIRST_COLUMN_COMMENT
            || !isCommentToken(element)
            || IndentHelper.getInstance().getIndent(myProject, file.getFileType(), element, true) != 0;
    }

    private static boolean isCommentToken(ASTNode element) {
        Language language = element.getElementType().getLanguage();
        return Commenter.forLanguage(language) instanceof CodeDocumentationAwareCommenter docAwareCommenter
            && (element.getElementType() == docAwareCommenter.getBlockCommentTokenType()
            || element.getElementType() == docAwareCommenter.getLineCommentTokenType());
    }


    public IndentOld getIndent(String text, FileType fileType) {
        int indent = IndentHelperImpl.getIndent(CodeStyle.getSettings(myProject).getIndentOptions(fileType), text, true);
        int indentLevel = indent / IndentHelperImpl.INDENT_FACTOR;
        int spaceCount = indent - indentLevel * IndentHelperImpl.INDENT_FACTOR;
        return new IndentOldImpl(CodeStyle.getSettings(myProject), indentLevel, spaceCount, fileType);
    }

    public String fillIndent(IndentOld indent, FileType fileType) {
        IndentOldImpl indent1 = (IndentOldImpl)indent;
        int indentLevel = indent1.getIndentLevel();
        int spaceCount = indent1.getSpaceCount();
        CodeStyleSettings settings = CodeStyle.getSettings(myProject);
        if (indentLevel < 0) {
            spaceCount += indentLevel * settings.getIndentSize(fileType);
            indentLevel = 0;
            if (spaceCount < 0) {
                spaceCount = 0;
            }
        }
        else {
            if (spaceCount < 0) {
                int v = (-spaceCount + settings.getIndentSize(fileType) - 1) / settings.getIndentSize(fileType);
                indentLevel -= v;
                spaceCount += v * settings.getIndentSize(fileType);
                if (indentLevel < 0) {
                    indentLevel = 0;
                }
            }
        }
        return IndentHelper.getInstance().fillIndent(myProject, fileType, indentLevel * IndentHelperImpl.INDENT_FACTOR + spaceCount);
    }

    public IndentOld zeroIndent() {
        return new IndentOldImpl(CodeStyle.getSettings(myProject), 0, 0, null);
    }

    @Nonnull
    private static CodeStyleSettings getSettings(@Nonnull PsiFile file) {
        return CodeStyle.getSettings(file);
    }

    @Override
    public boolean isSequentialProcessingAllowed() {
        return CoreCodeStyleUtil.isSequentialProcessingAllowed();
    }

    @Override
    public void performActionWithFormatterDisabled(Runnable r) {
        performActionWithFormatterDisabled(() -> {
            r.run();
            return null;
        });
    }

    @Override
    public <T extends Throwable> void performActionWithFormatterDisabled(ThrowableRunnable<T> r) throws T {
        Throwable[] throwable = new Throwable[1];

        performActionWithFormatterDisabled(() -> {
            try {
                r.run();
            }
            catch (Throwable t) {
                throwable[0] = t;
            }
            return null;
        });

        if (throwable[0] != null) {
            //noinspection unchecked
            throw (T)throwable[0];
        }
    }

    @Override
    public <T> T performActionWithFormatterDisabled(Supplier<T> r) {
        return ((FormatterImpl)FormatterEx.getInstanceEx()).runWithFormattingDisabled(() -> {
            PostprocessReformattingAspect component = PostprocessReformattingAspect.getInstance(getProject());
            return component.disablePostprocessFormattingInside(r);
        });
    }

    private static class RangeFormatInfo {
        private final SmartPsiElementPointer<?> startPointer;
        private final SmartPsiElementPointer<?> endPointer;
        private final boolean fromStart;
        private final boolean toEnd;

        RangeFormatInfo(
            @Nullable SmartPsiElementPointer<?> startPointer,
            @Nullable SmartPsiElementPointer<?> endPointer,
            boolean fromStart,
            boolean toEnd
        ) {
            this.startPointer = startPointer;
            this.endPointer = endPointer;
            this.fromStart = fromStart;
            this.toEnd = toEnd;
        }
    }

    // There is a possible case that cursor is located at the end of the line that contains only white spaces. For example:
    //     public void foo() {
    //         <caret>
    //     }
    // Formatter removes such white spaces, i.e. keeps only line feed symbol. But we want to preserve caret position then.
    // So, if 'virtual space in editor' is enabled, we save target visual column. Caret indent is ensured otherwise
    private static class CaretPositionKeeper {
        Editor myEditor;
        Document myDocument;
        CaretModel myCaretModel;
        RangeMarker myBeforeCaretRangeMarker;
        String myCaretIndentToRestore;
        int myVisualColumnToRestore = -1;
        boolean myBlankLineIndentPreserved;

        CaretPositionKeeper(@Nonnull Editor editor, @Nonnull CodeStyleSettings settings, @Nonnull Language language) {
            myEditor = editor;
            myCaretModel = editor.getCaretModel();
            myDocument = editor.getDocument();
            myBlankLineIndentPreserved = isBlankLineIndentPreserved(settings, language);

            int caretOffset = getCaretOffset();
            int lineStartOffset = getLineStartOffsetByTotalOffset(caretOffset);
            int lineEndOffset = getLineEndOffsetByTotalOffset(caretOffset);
            boolean shouldFixCaretPosition = rangeHasWhiteSpaceSymbolsOnly(myDocument.getCharsSequence(), lineStartOffset, lineEndOffset);

            if (shouldFixCaretPosition) {
                initRestoreInfo(caretOffset);
            }
        }

        private static boolean isBlankLineIndentPreserved(@Nonnull CodeStyleSettings settings, @Nonnull Language language) {
            CommonCodeStyleSettings langSettings = settings.getCommonSettings(language);
            CommonCodeStyleSettings.IndentOptions indentOptions = langSettings.getIndentOptions();
            return indentOptions != null && indentOptions.KEEP_INDENTS_ON_EMPTY_LINES;
        }

        private void initRestoreInfo(int caretOffset) {
            int lineStartOffset = getLineStartOffsetByTotalOffset(caretOffset);

            myVisualColumnToRestore = myCaretModel.getVisualPosition().column;
            myCaretIndentToRestore = myDocument.getText(TextRange.create(lineStartOffset, caretOffset));
            myBeforeCaretRangeMarker = myDocument.createRangeMarker(0, lineStartOffset);
        }

        public void restoreCaretPosition() {
            if (isVirtualSpaceEnabled()) {
                restoreVisualPosition();
            }
            else {
                restorePositionByIndentInsertion();
            }
        }

        private void restorePositionByIndentInsertion() {
            if (myBeforeCaretRangeMarker == null || !myBeforeCaretRangeMarker.isValid() || myCaretIndentToRestore == null || myBlankLineIndentPreserved) {
                return;
            }
            int newCaretLineStartOffset = myBeforeCaretRangeMarker.getEndOffset();
            myBeforeCaretRangeMarker.dispose();
            if (myCaretModel.getVisualPosition().column == myVisualColumnToRestore) {
                return;
            }
            Project project = myEditor.getProject();
            if (project == null || PsiDocumentManager.getInstance(project).isDocumentBlockedByPsi(myDocument)) {
                return;
            }
            insertWhiteSpaceIndentIfNeeded(newCaretLineStartOffset);
        }

        private void restoreVisualPosition() {
            if (myVisualColumnToRestore < 0) {
                EditorUtil.runWithAnimationDisabled(myEditor, () -> myEditor.getScrollingModel().scrollToCaret(ScrollType.RELATIVE));
                return;
            }
            VisualPosition position = myCaretModel.getVisualPosition();
            if (myVisualColumnToRestore != position.column) {
                myCaretModel.moveToVisualPosition(new VisualPosition(position.line, myVisualColumnToRestore));
            }
        }

        private void insertWhiteSpaceIndentIfNeeded(int caretLineOffset) {
            int lineToInsertIndent = myDocument.getLineNumber(caretLineOffset);
            if (!lineContainsWhiteSpaceSymbolsOnly(lineToInsertIndent)) {
                return;
            }

            int lineToInsertStartOffset = myDocument.getLineStartOffset(lineToInsertIndent);

            if (lineToInsertIndent != getCurrentCaretLine()) {
                myCaretModel.moveToOffset(lineToInsertStartOffset);
            }
            myDocument.replaceString(lineToInsertStartOffset, caretLineOffset, myCaretIndentToRestore);
        }

        private static boolean rangeHasWhiteSpaceSymbolsOnly(CharSequence text, int lineStartOffset, int lineEndOffset) {
            for (int i = lineStartOffset; i < lineEndOffset; i++) {
                char c = text.charAt(i);
                if (c != ' ' && c != '\t' && c != '\n') {
                    return false;
                }
            }
            return true;
        }

        private boolean isVirtualSpaceEnabled() {
            return myEditor.getSettings().isVirtualSpace();
        }

        private int getLineStartOffsetByTotalOffset(int offset) {
            int line = myDocument.getLineNumber(offset);
            return myDocument.getLineStartOffset(line);
        }

        private int getLineEndOffsetByTotalOffset(int offset) {
            int line = myDocument.getLineNumber(offset);
            return myDocument.getLineEndOffset(line);
        }

        private int getCaretOffset() {
            int caretOffset = myCaretModel.getOffset();
            caretOffset = Math.max(Math.min(caretOffset, myDocument.getTextLength() - 1), 0);
            return caretOffset;
        }

        private boolean lineContainsWhiteSpaceSymbolsOnly(int lineNumber) {
            int startOffset = myDocument.getLineStartOffset(lineNumber);
            int endOffset = myDocument.getLineEndOffset(lineNumber);
            return rangeHasWhiteSpaceSymbolsOnly(myDocument.getCharsSequence(), startOffset, endOffset);
        }

        private int getCurrentCaretLine() {
            return myDocument.getLineNumber(myCaretModel.getOffset());
        }
    }

    private static void postProcessEnabledRanges(@Nonnull PsiFile file, @Nonnull TextRange range, CodeStyleSettings settings) {
        List<TextRange> enabledRanges = new FormatterTagHandler(getSettings(file)).getEnabledRanges(file.getNode(), range);
        int delta = 0;
        for (TextRange enabledRange : enabledRanges) {
            enabledRange = enabledRange.shiftRight(delta);
            for (PostFormatProcessor processor : PostFormatProcessor.EP_NAME.getExtensionList()) {
                TextRange processedRange = processor.processText(file, enabledRange, settings);
                delta += processedRange.getLength() - enabledRange.getLength();
            }
        }
    }

    @Override
    public FormattingMode getCurrentFormattingMode() {
        return myCurrentFormattingMode.get();
    }

    void setCurrentFormattingMode(@Nonnull FormattingMode mode) {
        myCurrentFormattingMode.set(mode);
    }

    @Override
    @RequiredReadAction
    public int getSpacing(@Nonnull PsiFile file, int offset) {
        FormattingModel model = createFormattingModel(file);
        return model == null ? -1 : FormatterEx.getInstanceEx().getSpacingForBlockAtOffset(model, offset);
    }

    @Override
    @RequiredReadAction
    public int getMinLineFeeds(@Nonnull PsiFile file, int offset) {
        FormattingModel model = createFormattingModel(file);
        return model == null ? -1 : FormatterEx.getInstanceEx().getMinLineFeedsBeforeBlockAtOffset(model, offset);
    }

    @Nullable
    @RequiredReadAction
    private static FormattingModel createFormattingModel(@Nonnull PsiFile file) {
        FormattingModelBuilder builder = FormattingModelBuilder.forContext(file);
        if (builder == null) {
            return null;
        }
        CodeStyleSettings settings = CodeStyle.getSettings(file);
        return builder.createModel(FormattingContext.create(file, settings));
    }

    @Override
    @RequiredWriteAction
    public void runWithDocCommentFormattingDisabled(@Nonnull PsiFile file, @Nonnull @RequiredWriteAction Runnable runnable) {
        DocCommentSettings docSettings = getDocCommentSettings(file);
        boolean currDocFormattingEnabled = docSettings.isDocFormattingEnabled();
        docSettings.setDocFormattingEnabled(false);
        try {
            runnable.run();
        }
        finally {
            docSettings.setDocFormattingEnabled(currDocFormattingEnabled);
        }
    }

    @Override
    @Nonnull
    @RequiredReadAction
    public DocCommentSettings getDocCommentSettings(@Nonnull PsiFile file) {
        Language language = file.getLanguage();
        LanguageCodeStyleSettingsProvider settingsProvider = LanguageCodeStyleSettingsProvider.forLanguage(language);
        if (settingsProvider != null) {
            return settingsProvider.getDocCommentSettings(CodeStyle.getSettings(file));
        }
        return DocCommentSettings.DEFAULTS;
    }

    @Override
    @RequiredUIAccess
    public void scheduleIndentAdjustment(@Nonnull Document document, int offset) {
        FormatterBasedIndentAdjuster.scheduleIndentAdjustment(myProject, document, offset);
    }
}
