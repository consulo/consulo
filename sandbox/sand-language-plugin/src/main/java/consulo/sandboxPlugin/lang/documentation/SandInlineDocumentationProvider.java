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
package consulo.sandboxPlugin.lang.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.editor.documentation.InlineDocumentation;
import consulo.language.editor.documentation.InlineDocumentationProvider;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiRecursiveElementWalkingVisitor;
import consulo.language.psi.util.PsiTreeUtil;
import consulo.sandboxPlugin.lang.SandLanguage;
import consulo.sandboxPlugin.lang.psi.SandTokens;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Renders runs of adjacent {@code //} line comments as a single inline documentation block.
 * <p>
 * Sand has no block comment concept, so there is no {@code PsiDocCommentBase} to hang documentation on - this provider
 * implements {@link InlineDocumentationProvider} directly instead of going through the compatibility bridge.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class SandInlineDocumentationProvider implements InlineDocumentationProvider {
    private static final class SandCommentDocumentation implements InlineDocumentation {
        private final PsiFile myFile;
        private final TextRange myRange;

        private SandCommentDocumentation(PsiFile file, TextRange range) {
            myFile = file;
            myRange = range;
        }

        @Override
        public TextRange getDocumentationRange() {
            return myRange;
        }

        @Override
        public @Nullable TextRange getDocumentationOwnerRange() {
            return null;
        }

        @RequiredReadAction
        @Override
        public @Nullable String renderText() {
            String text = myRange.substring(myFile.getText());
            StringBuilder builder = new StringBuilder("<html><body>");
            for (String line : text.split("\n")) {
                builder.append(line.trim().replaceFirst("^//", "").trim()).append("<br>");
            }
            return builder.append("</body></html>").toString();
        }
    }

    @RequiredReadAction
    @Override
    public Collection<InlineDocumentation> inlineDocumentationItems(PsiFile file) {
        if (file.getLanguage() != SandLanguage.INSTANCE) {
            return List.of();
        }

        List<InlineDocumentation> result = new ArrayList<>();
        for (TextRange range : collectCommentRuns(file)) {
            result.add(new SandCommentDocumentation(file, range));
        }
        return result;
    }

    @RequiredReadAction
    @Override
    public @Nullable InlineDocumentation findInlineDocumentation(PsiFile file, TextRange textRange) {
        if (file.getLanguage() != SandLanguage.INSTANCE) {
            return null;
        }

        for (TextRange range : collectCommentRuns(file)) {
            if (range.equals(textRange)) {
                return new SandCommentDocumentation(file, textRange);
            }
        }
        return null;
    }

    /**
     * Collects maximal runs of adjacent line comments, so a block of {@code //} lines renders as one region.
     */
    @RequiredReadAction
    private static List<TextRange> collectCommentRuns(PsiFile file) {
        List<PsiElement> comments = new ArrayList<>();
        file.accept(new PsiRecursiveElementWalkingVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element.getNode() != null && element.getNode().getElementType() == SandTokens.LINE_COMMENT) {
                    comments.add(element);
                }
                super.visitElement(element);
            }
        });

        List<TextRange> result = new ArrayList<>();
        int i = 0;
        while (i < comments.size()) {
            PsiElement first = comments.get(i);
            PsiElement last = first;
            int j = i + 1;
            while (j < comments.size() && isDirectlyBelow(last, comments.get(j))) {
                last = comments.get(j);
                j++;
            }
            result.add(new TextRange(first.getTextRange().getStartOffset(), last.getTextRange().getEndOffset()));
            i = j;
        }
        return result;
    }

    /**
     * Two comments belong to the same run when only whitespace without a blank line separates them.
     */
    @RequiredReadAction
    private static boolean isDirectlyBelow(PsiElement previous, PsiElement next) {
        PsiElement between = PsiTreeUtil.nextLeaf(previous);
        int newLines = 0;
        while (between != null && between != next) {
            String text = between.getText();
            if (!text.isBlank()) {
                return false;
            }
            newLines += text.chars().filter(c -> c == '\n').count();
            between = PsiTreeUtil.nextLeaf(between);
        }
        return between == next && newLines <= 1;
    }
}
