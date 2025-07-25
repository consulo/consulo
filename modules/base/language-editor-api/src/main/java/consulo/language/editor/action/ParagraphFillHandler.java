package consulo.language.editor.action;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.document.util.UnfairTextRange;
import consulo.language.Language;
import consulo.language.ast.IElementType;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.FormatterTagHandler;
import consulo.language.editor.internal.DefaultParagraphFillHandler;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.plain.psi.PsiPlainTextFile;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.CharFilter;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Defines general re-flow paragraph functionality.
 * Serves plain text files.
 *
 * @author ktisha
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ParagraphFillHandler implements LanguageExtension {
    private static final ExtensionPointCacheKey<ParagraphFillHandler, ByLanguageValue<ParagraphFillHandler>> KEY =
        ExtensionPointCacheKey.create("ParagraphFillHandler", LanguageOneToOne.build(new DefaultParagraphFillHandler()));

    @Nonnull
    public static ParagraphFillHandler forLanguage(@Nonnull Language language) {
        return Application.get().getExtensionPoint(ParagraphFillHandler.class).getOrBuildCache(KEY).requiredGet(language);
    }

    @RequiredReadAction
    @RequiredUIAccess
    public final void performOnElement(@Nonnull PsiElement element, @Nonnull Editor editor) {
        Document document = editor.getDocument();

        TextRange textRange = getTextRange(element, editor);
        if (textRange.isEmpty()) {
            return;
        }
        String text = textRange.substring(element.getContainingFile().getText());

        List<String> subStrings = StringUtil.split(text, "\n", true);
        String prefix = getPrefix(element);
        String postfix = getPostfix(element);

        StringBuilder stringBuilder = new StringBuilder();
        appendPrefix(element, text, stringBuilder);

        for (String string : subStrings) {
            String startTrimmed = StringUtil.trimStart(string.trim(), prefix.trim());
            String str = StringUtil.trimEnd(startTrimmed, postfix.trim());
            String finalString = str.trim();
            if (!StringUtil.isEmptyOrSpaces(finalString)) {
                stringBuilder.append(finalString).append(" ");
            }
        }
        appendPostfix(element, text, stringBuilder);

        String replacementText = stringBuilder.toString();

        CommandProcessor.getInstance().newCommand()
            .project(element.getProject())
            .groupId(document)
            .run(() -> {
                document.replaceString(textRange.getStartOffset(), textRange.getEndOffset(), replacementText);
                PsiFile file = element.getContainingFile();
                FormatterTagHandler formatterTagHandler = new FormatterTagHandler(CodeStyleSettingsManager.getSettings(file.getProject()));
                List<TextRange> enabledRanges =
                    formatterTagHandler.getEnabledRanges(file.getNode(), TextRange.create(0, document.getTextLength()));

                LanguageEditorInternalHelper helper = LanguageEditorInternalHelper.getInstance();

                helper.doWrapLongLinesIfNecessary(
                    editor,
                    element.getProject(),
                    element.getLanguage(),
                    document,
                    textRange.getStartOffset(),
                    textRange.getStartOffset() + replacementText.length() + 1,
                    enabledRanges
                );
            });
    }

    protected void appendPostfix(
        @Nonnull PsiElement element,
        @Nonnull String text,
        @Nonnull StringBuilder stringBuilder
    ) {
        String postfix = getPostfix(element);
        if (text.endsWith(postfix.trim())) {
            stringBuilder.append(postfix);
        }
    }

    protected void appendPrefix(@Nonnull PsiElement element, @Nonnull String text, @Nonnull StringBuilder stringBuilder) {
        String prefix = getPrefix(element);
        if (text.startsWith(prefix.trim())) {
            stringBuilder.append(prefix);
        }
    }

    @RequiredReadAction
    private TextRange getTextRange(@Nonnull PsiElement element, @Nonnull Editor editor) {
        int startOffset = getStartOffset(element, editor);
        int endOffset = getEndOffset(element, editor);
        return new UnfairTextRange(startOffset, endOffset);
    }

    @RequiredReadAction
    private int getStartOffset(@Nonnull PsiElement element, @Nonnull Editor editor) {
        if (isBunchOfElement(element)) {
            PsiElement firstElement = getFirstElement(element);
            return firstElement != null ? firstElement.getTextRange().getStartOffset() : element.getTextRange().getStartOffset();
        }
        int offset = editor.getCaretModel().getOffset();
        int elementTextOffset = element.getTextOffset();
        Document document = editor.getDocument();
        int lineNumber = document.getLineNumber(offset);

        while (lineNumber != document.getLineNumber(elementTextOffset)) {
            String text =
                document.getText(TextRange.create(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)));
            if (StringUtil.isEmptyOrSpaces(text)) {
                lineNumber += 1;
                break;
            }
            lineNumber -= 1;
        }
        int lineStartOffset = lineNumber == document.getLineNumber(elementTextOffset)
            ? elementTextOffset
            : document.getLineStartOffset(lineNumber);
        String lineText = document.getText(TextRange.create(lineStartOffset, document.getLineEndOffset(lineNumber)));
        int shift = StringUtil.findFirst(lineText, CharFilter.NOT_WHITESPACE_FILTER);

        return lineStartOffset + shift;
    }

    protected boolean isBunchOfElement(PsiElement element) {
        return element instanceof PsiComment;
    }

    @RequiredReadAction
    private int getEndOffset(@Nonnull PsiElement element, @Nonnull Editor editor) {
        if (isBunchOfElement(element)) {
            PsiElement next = getLastElement(element);
            return next != null ? next.getTextRange().getEndOffset() : element.getTextRange().getEndOffset();
        }
        int offset = editor.getCaretModel().getOffset();
        int elementTextOffset = element.getTextRange().getEndOffset();
        Document document = editor.getDocument();
        int lineNumber = document.getLineNumber(offset);

        while (lineNumber != document.getLineNumber(elementTextOffset)) {
            String text =
                document.getText(TextRange.create(document.getLineStartOffset(lineNumber), document.getLineEndOffset(lineNumber)));
            if (StringUtil.isEmptyOrSpaces(text)) {
                lineNumber -= 1;
                break;
            }
            lineNumber += 1;
        }
        return document.getLineEndOffset(lineNumber);
    }

    @Nullable
    @RequiredReadAction
    private PsiElement getFirstElement(@Nonnull PsiElement element) {
        IElementType elementType = element.getNode().getElementType();
        PsiElement prevSibling = element.getPrevSibling();
        PsiElement result = element;
        while (prevSibling != null && (prevSibling.getNode().getElementType().equals(elementType)
            || (atWhitespaceToken(prevSibling) && StringUtil.countChars(prevSibling.getText(), '\n') <= 1))) {
            String text = prevSibling.getText();
            String prefix = getPrefix(element);
            String postfix = getPostfix(element);
            text = StringUtil.trimStart(text.trim(), prefix.trim());
            text = StringUtil.trimEnd(text, postfix);

            if (prevSibling.getNode().getElementType().equals(elementType) && StringUtil.isEmptyOrSpaces(text)) {
                break;
            }
            if (prevSibling.getNode().getElementType().equals(elementType)) {
                result = prevSibling;
            }
            prevSibling = prevSibling.getPrevSibling();
        }
        return result;
    }

    @Nullable
    @RequiredReadAction
    private PsiElement getLastElement(@Nonnull PsiElement element) {
        IElementType elementType = element.getNode().getElementType();
        PsiElement nextSibling = element.getNextSibling();
        PsiElement result = element;
        while (nextSibling != null && (nextSibling.getNode().getElementType().equals(elementType)
            || (atWhitespaceToken(nextSibling) && StringUtil.countChars(nextSibling.getText(), '\n') <= 1))) {
            String text = nextSibling.getText();
            String prefix = getPrefix(element);
            String postfix = getPostfix(element);
            text = StringUtil.trimStart(text.trim(), prefix.trim());
            text = StringUtil.trimEnd(text, postfix);

            if (nextSibling.getNode().getElementType().equals(elementType) && StringUtil.isEmptyOrSpaces(text)) {
                break;
            }
            if (nextSibling.getNode().getElementType().equals(elementType)) {
                result = nextSibling;
            }
            nextSibling = nextSibling.getNextSibling();
        }
        return result;
    }

    public boolean atWhitespaceToken(@Nullable PsiElement element) {
        return element instanceof PsiWhiteSpace;
    }

    public boolean isAvailableForElement(@Nullable PsiElement element) {
        return element != null;
    }

    public boolean isAvailableForFile(@Nullable PsiFile psiFile) {
        return psiFile instanceof PsiPlainTextFile;
    }

    @Nonnull
    protected String getPrefix(@Nonnull PsiElement element) {
        return "";
    }

    @Nonnull
    protected String getPostfix(@Nonnull PsiElement element) {
        return "";
    }
}
