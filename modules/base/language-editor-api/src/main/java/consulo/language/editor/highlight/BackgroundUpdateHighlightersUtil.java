// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.highlight;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.Document;
import consulo.language.editor.internal.BackgroundUpdateHighlightersInternal;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import java.util.Collection;

/**
 * Document markup manipulation methods during the highlighting, in the background thread under read action.
 * Must be used inside the highlighting process only (e.g., in your {@link TextEditorHighlightingPass#collectInformation})
 */
public final class BackgroundUpdateHighlightersUtil {
    private BackgroundUpdateHighlightersUtil() {
    }

    @RequiredReadAction
    public static void setHighlightersToEditor(
        Project project,
        PsiFile psiFile,
        Document document,
        int startOffset,
        int endOffset,
        Collection<? extends HighlightInfo> highlights,
        int group
    ) {
        BackgroundUpdateHighlightersInternal.getInstance()
            .setHighlightersToEditor(project, psiFile, document, startOffset, endOffset, highlights, group);
    }
}
