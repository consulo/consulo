// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.documentation;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import org.jspecify.annotations.Nullable;

public interface InlineDocumentation {

    /**
     * The returned range might span over several elements,
     * the range can start or end in a middle of a PsiElement.
     *
     * @return absolute range of the documentation, e.g. JavaDoc element of a Java class
     */
    TextRange getDocumentationRange();

    /**
     * The returned range is used to find inline documentation by an offset outside of {@link #getDocumentationRange}.
     *
     * @return absolute range of the documentation owner, e.g. Java class,
     * or {@code null} if there is no owner
     */
    @Nullable
    TextRange getDocumentationOwnerRange();

    @RequiredReadAction
    @Nullable
    String renderText();
}
