// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.highlight;

import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

public class UpdateHighlightersUtil {
    public static boolean isFileLevelOrGutterAnnotation(HighlightInfo info) {
        return info.isFileLevelAnnotation() || info.getGutterIconRenderer() != null;
    }

    @RequiredUIAccess
    public static void setHighlightersToEditor(@Nonnull Project project,
                                               @Nonnull Document document,
                                               int startOffset,
                                               int endOffset,
                                               @Nonnull Collection<HighlightInfo> highlights,
                                               // if null global scheme will be used
                                               @Nullable EditorColorsScheme colorsScheme,
                                               int group) {
        LanguageEditorInternalHelper.getInstance().setHighlightersToEditor(project, document, startOffset, endOffset, highlights, colorsScheme, group);
    }

    @Deprecated //for teamcity
    @RequiredUIAccess
    public static void setHighlightersToEditor(@Nonnull Project project, @Nonnull Document document, int startOffset, int endOffset, @Nonnull Collection<HighlightInfo> highlights, int group) {
        setHighlightersToEditor(project, document, startOffset, endOffset, highlights, null, group);
    }
}
