// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.highlight;

import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.language.editor.internal.LanguageEditorInternalHelper;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.annotation.access.RequiredReadAction;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

public class UpdateHighlightersUtil {
    public static boolean isFileLevelOrGutterAnnotation(HighlightInfo info) {
        return info.isFileLevelAnnotation() || info.getGutterIconRenderer() != null;
    }

    @RequiredReadAction
    public static void setHighlightersToEditor(Project project,
                                               Document document,
                                               int startOffset,
                                               int endOffset,
                                               Collection<HighlightInfo> highlights,
                                               // if null global scheme will be used
                                               @Nullable EditorColorsScheme colorsScheme,
                                               int group) {
        LanguageEditorInternalHelper.getInstance().setHighlightersToEditor(project, document, startOffset, endOffset, highlights, colorsScheme, group);
    }

    @Deprecated //for teamcity
    @RequiredReadAction
    public static void setHighlightersToEditor(Project project, Document document, int startOffset, int endOffset, Collection<HighlightInfo> highlights, int group) {
        setHighlightersToEditor(project, document, startOffset, endOffset, highlights, null, group);
    }
}
