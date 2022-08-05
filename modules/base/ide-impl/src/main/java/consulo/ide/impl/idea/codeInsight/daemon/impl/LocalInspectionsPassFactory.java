/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

package consulo.ide.impl.idea.codeInsight.daemon.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.impl.highlight.MainHighlightingPassFactory;
import consulo.language.editor.Pass;
import consulo.language.editor.impl.highlight.HighlightInfoProcessor;
import consulo.language.editor.impl.highlight.TextEditorHighlightingPass;
import consulo.language.editor.impl.highlight.VisibleHighlightingPassFactory;
import consulo.language.editor.impl.internal.daemon.FileStatusMap;
import consulo.language.editor.inspection.scheme.InspectionProfileWrapper;
import consulo.language.editor.inspection.scheme.LocalInspectionToolWrapper;
import consulo.logging.Logger;
import consulo.document.Document;
import consulo.codeEditor.Editor;
import consulo.document.util.TextRange;
import consulo.ide.impl.idea.profile.codeInspection.InspectionProjectProfileManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.ide.impl.idea.util.ArrayUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author cdr
 */
@ExtensionImpl
public class LocalInspectionsPassFactory implements MainHighlightingPassFactory {
  private static final Logger LOG = Logger.getInstance(LocalInspectionsPassFactory.class);

  @Override
  public void register(@Nonnull Registrar registrar) {
    registrar.registerTextEditorHighlightingPass(this, new int[]{Pass.UPDATE_ALL}, ArrayUtil.EMPTY_INT_ARRAY, true, Pass.LOCAL_INSPECTIONS);
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@Nonnull PsiFile file, @Nonnull final Editor editor) {
    TextRange textRange = calculateRangeToProcess(editor);
    if (textRange == null || !InspectionProjectProfileManager.getInstance(file.getProject()).isProfileLoaded()){
      return new ProgressableTextEditorHighlightingPass.EmptyPass(file.getProject(), editor.getDocument());
    }
    TextRange visibleRange = VisibleHighlightingPassFactory.calculateVisibleRange(editor);
    return new MyLocalInspectionsPass(file, editor.getDocument(), textRange, visibleRange, new DefaultHighlightInfoProcessor());
  }

  @Override
  public TextEditorHighlightingPass createMainHighlightingPass(@Nonnull PsiFile file,
                                                               @Nonnull Document document,
                                                               @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
    final TextRange textRange = file.getTextRange();
    LOG.assertTrue(textRange != null, "textRange is null for " + file + " (" + PsiUtilCore.getVirtualFile(file) + ")");
    return new MyLocalInspectionsPass(file, document, textRange, LocalInspectionsPass.EMPTY_PRIORITY_RANGE, highlightInfoProcessor);
  }

  private static TextRange calculateRangeToProcess(Editor editor) {
    return FileStatusMap.getDirtyTextRange(editor, Pass.LOCAL_INSPECTIONS);
  }

  private static class MyLocalInspectionsPass extends LocalInspectionsPass {
    private MyLocalInspectionsPass(PsiFile file,
                                   Document document,
                                   @Nonnull TextRange textRange,
                                   TextRange visibleRange,
                                   @Nonnull HighlightInfoProcessor highlightInfoProcessor) {
      super(file, document, textRange.getStartOffset(), textRange.getEndOffset(), visibleRange, true, highlightInfoProcessor);
    }

    @Nonnull
    @Override
    List<LocalInspectionToolWrapper> getInspectionTools(@Nonnull InspectionProfileWrapper profile) {
      List<LocalInspectionToolWrapper> tools = super.getInspectionTools(profile);
      List<LocalInspectionToolWrapper> result = new ArrayList<LocalInspectionToolWrapper>(tools.size());
      for (LocalInspectionToolWrapper tool : tools) {
        if (!tool.runForWholeFile()) result.add(tool);
      }
      return result;
    }
  }
}
