// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.codeEditor.Editor;
import consulo.language.psi.PsiFile;
import consulo.project.Project;

import java.util.Collections;
import java.util.Set;

public interface OwnBypassCollector extends InlayHintsCollector {
    /**
     * Collects all inlays for a given file.
     */
    void collectHintsForFile(PsiFile file, InlayTreeSink sink);

    /**
     * @return true iff this particular place (e.g. element near the caret) may contribute inlays if the provider is enabled.
     * It will be called inside intention, make sure it runs fast.
     */
    default boolean shouldSuggestToggling(Project project, Editor editor, PsiFile file) {
        return false;
    }

    /**
     * @return true iff this particular place (e.g. element near the caret) may contribute inlays if the provider and particular set of options are enabled.
     * It will be called inside intention, make sure it runs fast.
     */
    default Set<String> getOptionsToToggle(Project project, Editor editor, PsiFile file) {
        return Collections.emptySet();
    }
}