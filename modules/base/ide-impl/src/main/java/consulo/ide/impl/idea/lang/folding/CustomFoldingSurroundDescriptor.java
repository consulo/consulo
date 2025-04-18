/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.lang.folding;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.Commenter;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.codeStyle.CodeStyleManager;
import consulo.language.codeStyle.CodeStyleSettingsManager;
import consulo.language.codeStyle.CommonCodeStyleSettings;
import consulo.language.editor.folding.CustomFoldingBuilder;
import consulo.language.editor.folding.CustomFoldingProvider;
import consulo.language.editor.folding.FoldingBuilder;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.surroundWith.Surrounder;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiWhiteSpace;
import consulo.language.util.IncorrectOperationException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.StringUtil;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Rustam Vishnyakov
 */
public class CustomFoldingSurroundDescriptor implements SurroundDescriptor {
    private static final ExtensionPointCacheKey<CustomFoldingProvider, Surrounder[]> CUSTOM_SURROUNDERS = ExtensionPointCacheKey.create(
        "CUSTOM_SURROUNDERS",
        customFoldingProviders -> {
            List<CustomFoldingRegionSurrounder> surrounderList = new ArrayList<>();
            CustomFoldingProvider.EP_NAME.forEachExtensionSafe(it -> surrounderList.add(new CustomFoldingRegionSurrounder(it)));
            return surrounderList.toArray(new CustomFoldingRegionSurrounder[surrounderList.size()]);
        }
    );

    public final static CustomFoldingSurroundDescriptor INSTANCE = new CustomFoldingSurroundDescriptor();

    private final static String DEFAULT_DESC_TEXT = "Description";

    @Nonnull
    @Override
    @RequiredReadAction
    public PsiElement[] getElementsToSurround(PsiFile file, int startOffset, int endOffset) {
        if (startOffset >= endOffset - 1) {
            return PsiElement.EMPTY_ARRAY;
        }
        Commenter commenter = Commenter.forLanguage(file.getLanguage());
        if (commenter == null || commenter.getLineCommentPrefix() == null) {
            return PsiElement.EMPTY_ARRAY;
        }
        PsiElement startElement = file.findElementAt(startOffset);
        if (startElement instanceof PsiWhiteSpace) {
            startElement = startElement.getNextSibling();
        }
        PsiElement endElement = file.findElementAt(endOffset - 1);
        if (endElement instanceof PsiWhiteSpace) {
            endElement = endElement.getPrevSibling();
        }
        if (startElement != null && endElement != null) {
            if (startElement.getTextRange().getStartOffset() > endElement.getTextRange().getStartOffset()) {
                return PsiElement.EMPTY_ARRAY;
            }
            startElement = findClosestParentAfterLineBreak(startElement);
            if (startElement != null) {
                endElement = findClosestParentBeforeLineBreak(endElement);
                if (endElement != null) {
                    PsiElement commonParent = startElement.getParent();
                    if (endElement.getParent() == commonParent) {
                        if (startElement == endElement) {
                            return new PsiElement[]{startElement};
                        }
                        return new PsiElement[]{startElement, endElement};
                    }
                }
            }
        }
        return PsiElement.EMPTY_ARRAY;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement findClosestParentAfterLineBreak(PsiElement element) {
        PsiElement parent = element;
        while (parent != null) {
            PsiElement prev = parent.getPrevSibling();
            while (prev != null && prev.getTextLength() <= 0) {
                prev = prev.getPrevSibling();
            }
            if (isWhiteSpaceWithLineFeed(prev)) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    private static PsiElement findClosestParentBeforeLineBreak(PsiElement element) {
        PsiElement parent = element;
        while (parent != null) {
            PsiElement next = parent.getNextSibling();
            if (isWhiteSpaceWithLineFeed(next)) {
                return parent;
            }
            parent = parent.getParent();
        }
        return null;
    }

    @RequiredReadAction
    private static boolean isWhiteSpaceWithLineFeed(@Nullable PsiElement element) {
        if (element == null) {
            return false;
        }
        if (element instanceof PsiWhiteSpace) {
            return element.textContains('\n');
        }
        ASTNode node = element.getNode();
        if (node == null) {
            return false;
        }
        CharSequence text = node.getChars();
        boolean lineFeedFound = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (!StringUtil.isWhiteSpace(c)) {
                return false;
            }
            lineFeedFound |= c == '\n';
        }
        return lineFeedFound;
    }

    @Nonnull
    @Override
    public Surrounder[] getSurrounders() {
        return Application.get().getExtensionPoint(CustomFoldingProvider.class).getOrBuildCache(CUSTOM_SURROUNDERS);
    }

    @Override
    public boolean isExclusive() {
        return false;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    private static class CustomFoldingRegionSurrounder implements Surrounder {

        private final CustomFoldingProvider myProvider;

        public CustomFoldingRegionSurrounder(@Nonnull CustomFoldingProvider provider) {
            myProvider = provider;
        }

        @Override
        public String getTemplateDescription() {
            return myProvider.getDescription();
        }

        @Override
        @RequiredReadAction
        public boolean isApplicable(@Nonnull PsiElement[] elements) {
            if (elements.length == 0) {
                return false;
            }
            for (FoldingBuilder each : FoldingBuilder.forLanguage(elements[0].getLanguage())) {
                if (each instanceof CustomFoldingBuilder) {
                    return true;
                }
            }
            return false;
        }

        @Override
        @RequiredUIAccess
        public TextRange surroundElements(
            @Nonnull Project project,
            @Nonnull Editor editor,
            @Nonnull PsiElement[] elements
        ) throws IncorrectOperationException {
            if (elements.length == 0) {
                return null;
            }
            PsiElement firstElement = elements[0];
            PsiElement lastElement = elements[elements.length - 1];
            PsiFile psiFile = firstElement.getContainingFile();
            Language language = psiFile.getLanguage();
            Commenter commenter = Commenter.forLanguage(language);
            if (commenter == null) {
                return null;
            }
            String linePrefix = commenter.getLineCommentPrefix();
            if (linePrefix == null) {
                return null;
            }
            int prefixLength = linePrefix.length();
            int startOffset = firstElement.getTextRange().getStartOffset();
            int endOffset = lastElement.getTextRange().getEndOffset();
            int delta = 0;
            TextRange rangeToSelect = new TextRange(startOffset, startOffset);
            String startText = myProvider.getStartString();
            int descPos = startText.indexOf("?");
            if (descPos >= 0) {
                startText = startText.replace("?", DEFAULT_DESC_TEXT);
                rangeToSelect = new TextRange(startOffset + descPos, startOffset + descPos + DEFAULT_DESC_TEXT.length());
            }
            String startString = linePrefix + startText + "\n";
            String endString = "\n" + linePrefix + myProvider.getEndString();
            editor.getDocument().insertString(endOffset, endString);
            delta += endString.length();
            editor.getDocument().insertString(startOffset, startString);
            delta += startString.length();
            rangeToSelect = rangeToSelect.shiftRight(prefixLength);
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            documentManager.commitDocument(documentManager.getDocument(psiFile));
            adjustLineIndent(project, psiFile, language, new TextRange(endOffset + delta - endString.length(), endOffset + delta));
            adjustLineIndent(project, psiFile, language, new TextRange(startOffset, startOffset + startString.length()));
            return rangeToSelect;
        }

        private static void adjustLineIndent(@Nonnull Project project, PsiFile file, Language language, TextRange range) {
            CommonCodeStyleSettings formatSettings = CodeStyleSettingsManager.getSettings(project).getCommonSettings(language);
            boolean keepAtFirstCol = formatSettings.KEEP_FIRST_COLUMN_COMMENT;
            formatSettings.KEEP_FIRST_COLUMN_COMMENT = false;
            CodeStyleManager.getInstance(project).adjustLineIndent(file, range);
            formatSettings.KEEP_FIRST_COLUMN_COMMENT = keepAtFirstCol;
        }
    }
}
