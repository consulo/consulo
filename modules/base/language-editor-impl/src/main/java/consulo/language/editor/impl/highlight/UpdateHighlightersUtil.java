// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.highlight;

import consulo.codeEditor.DocumentMarkupModel;
import consulo.codeEditor.markup.MarkupModel;
import consulo.codeEditor.markup.MarkupModelEx;
import consulo.colorScheme.EditorColorsScheme;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.impl.internal.daemon.DaemonCodeAnalyzerEx;
import consulo.language.editor.impl.internal.highlight.UpdateHighlightersUtilImpl;
import consulo.language.editor.impl.internal.rawHighlight.HighlightInfoImpl;
import consulo.language.editor.rawHighlight.HighlightInfo;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;

public class UpdateHighlightersUtil {
  public static boolean isFileLevelOrGutterAnnotation(HighlightInfo info) {
    return ((HighlightInfoImpl)info).isFileLevelAnnotation() || info.getGutterIconRenderer() != null;
  }

  @RequiredUIAccess
  public static void setHighlightersToEditor(@Nonnull Project project,
                                             @Nonnull Document document,
                                             int startOffset,
                                             int endOffset,
                                             @Nonnull Collection<HighlightInfo> highlights,
                                             // if null global scheme will be used
                                             @Nullable final EditorColorsScheme colorsScheme,
                                             int group) {
    TextRange range = new TextRange(startOffset, endOffset);
    UIAccess.assertIsUIThread();

    PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
    final DaemonCodeAnalyzerEx codeAnalyzer = DaemonCodeAnalyzerEx.getInstanceEx(project);
    codeAnalyzer.cleanFileLevelHighlights(project, group, psiFile);

    MarkupModel markup = DocumentMarkupModel.forDocument(document, project, true);
    UpdateHighlightersUtilImpl.assertMarkupConsistent(markup, project);

    UpdateHighlightersUtilImpl.setHighlightersInRange(project, document, range, colorsScheme, new ArrayList<>(highlights), (MarkupModelEx)markup, group);
  }

  @Deprecated //for teamcity
  @RequiredUIAccess
  public static void setHighlightersToEditor(@Nonnull Project project, @Nonnull Document document, int startOffset, int endOffset, @Nonnull Collection<HighlightInfo> highlights, int group) {
    setHighlightersToEditor(project, document, startOffset, endOffset, highlights, null, group);
  }
}
