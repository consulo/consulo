// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.documentation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * Extension point to provide inline documentation
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface InlineDocumentationProvider {

    /**
     * This defines {@link InlineDocumentation} in file, which can be rendered in place.
     * HTML content to be displayed will be obtained using {@link InlineDocumentation#renderText()} method.
     */
    Collection<InlineDocumentation> inlineDocumentationItems(PsiFile file);

    /**
     * Returns {@link InlineDocumentation} corresponding to the provided text range in a file.
     *
     * @see #inlineDocumentationItems(PsiFile)
     */
    @Nullable
    InlineDocumentation findInlineDocumentation(PsiFile file, TextRange textRange);
}
