// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.documentation;

import consulo.annotation.component.ExtensionImpl;
import consulo.document.util.TextRange;
import consulo.language.editor.documentation.InlineDocumentation;
import consulo.language.editor.documentation.InlineDocumentationProvider;
import consulo.language.editor.documentation.PsiCommentInlineDocumentation;
import consulo.language.editor.internal.DocumentationManagerHelper;
import consulo.language.psi.PsiDocCommentBase;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A provider which delegates to older {@link consulo.language.editor.documentation.DocumentationProvider} API.
 */
@ExtensionImpl
public class CompatibilityInlineDocumentationProvider implements InlineDocumentationProvider {

    @Override
    public Collection<InlineDocumentation> inlineDocumentationItems(PsiFile file) {
        List<InlineDocumentation> result = new ArrayList<>();
        DocumentationManagerHelper.getProviderFromElement(file)
            .collectDocComments(file, comment -> result.add(new PsiCommentInlineDocumentation(comment)));
        return result;
    }

    @Override
    public @Nullable InlineDocumentation findInlineDocumentation(PsiFile file, TextRange textRange) {
        PsiDocCommentBase comment = DocumentationManagerHelper.getProviderFromElement(file).findDocComment(file, textRange);
        if (comment == null) {
            return null;
        }
        return new PsiCommentInlineDocumentation(comment);
    }
}
