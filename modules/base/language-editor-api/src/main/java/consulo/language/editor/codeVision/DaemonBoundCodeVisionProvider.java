// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.codeVision;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointName;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/**
 * Almost the same thing as {@code CodeVisionProvider}, but run in the {@code DaemonCodeAnalyzer}
 * and that's why it has built-in support of interruption.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface DaemonBoundCodeVisionProvider {
    ExtensionPointName<DaemonBoundCodeVisionProvider> EP_NAME = ExtensionPointName.create(DaemonBoundCodeVisionProvider.class);

    default void preparePreview(Editor editor, PsiFile file) {
    }

    /**
     * Computes code lens data in read action in background for a given editor.
     */
    default List<Pair<TextRange, CodeVisionEntry>> computeForEditor(Editor editor, PsiFile file) {
        return Collections.emptyList();
    }

    default void handleClick(Editor editor, TextRange textRange, CodeVisionEntry entry) {
        if (entry instanceof CodeVisionPredefinedActionEntry actionEntry) {
            actionEntry.onClick(editor);
        }
    }

    default @Nullable CodeVisionPlaceholderCollector getPlaceholderCollector(Editor editor, @Nullable PsiFile psiFile) {
        return null;
    }

    /**
     * Name in settings.
     */
    LocalizeValue getName();

    List<CodeVisionRelativeOrdering> getRelativeOrderings();

    CodeVisionAnchorKind getDefaultAnchor();

    /**
     * Unique identifier (among all instances of {@link DaemonBoundCodeVisionProvider})
     */
    String getId();

    default String getGroupId() {
        return getId();
    }
}
