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

package consulo.ide.impl.idea.codeInsight.editorActions;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.editor.folding.CodeFoldingManager;
import consulo.ide.impl.idea.codeInsight.folding.impl.CodeFoldingManagerImpl;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.document.RangeMarker;
import consulo.project.Project;
import consulo.util.lang.ref.Ref;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@ExtensionImpl
public class CopyPasteFoldingProcessor extends CopyPastePostProcessor<FoldingTransferableData> {
  @Nonnull
  @Override
  public List<FoldingTransferableData> collectTransferableData(final PsiFile file, final Editor editor, final int[] startOffsets, final int[] endOffsets) {
    // might be slow
    //CodeFoldingManager.getInstance(file.getManager().getProject()).updateFoldRegions(editor);

    final ArrayList<FoldingData> list = new ArrayList<FoldingData>();
    final FoldRegion[] regions = editor.getFoldingModel().getAllFoldRegions();
    for (final FoldRegion region : regions) {
      if (!region.isValid()) continue;
      for (int j = 0; j < startOffsets.length; j++) {
        if (startOffsets[j] <= region.getStartOffset() && region.getEndOffset() <= endOffsets[j]) {
          list.add(
                  new FoldingData(
                          region.getStartOffset() - startOffsets[j],
                          region.getEndOffset() - startOffsets[j],
                          region.isExpanded()
                  )
          );
        }
      }
    }

    return Collections.singletonList(new FoldingTransferableData(list.toArray(new FoldingData[list.size()])));
  }

  @Nonnull
  @Override
  public List<FoldingTransferableData> extractTransferableData(final Transferable content) {
    FoldingTransferableData foldingData = null;
    try {
      final DataFlavor flavor = FoldingData.getDataFlavor();
      if (flavor != null) {
        foldingData = (FoldingTransferableData)content.getTransferData(flavor);
      }
    }
    catch (UnsupportedFlavorException e) {
      // do nothing
    }
    catch (IOException e) {
      // do nothing
    }

    if (foldingData != null) { // copy to prevent changing of original by convertLineSeparators
      return Collections.singletonList(foldingData.clone());
    }
    return Collections.emptyList();
  }

  @Override
  public void processTransferableData(final Project project,
                                      final Editor editor,
                                      final RangeMarker bounds,
                                      int caretOffset,
                                      Ref<Boolean> indented,
                                      final List<FoldingTransferableData> values) {
    assert values.size() == 1;
    final FoldingTransferableData value = values.get(0);
    if (value.getData().length == 0) return;

    final CodeFoldingManagerImpl foldingManager = (CodeFoldingManagerImpl)CodeFoldingManager.getInstance(project);
    foldingManager.updateFoldRegions(editor, true);

    Runnable operation = new Runnable() {
      @Override
      public void run() {
        for (FoldingData data : value.getData()) {
          FoldRegion region = foldingManager.findFoldRegion(editor, data.startOffset + bounds.getStartOffset(), data.endOffset + bounds.getStartOffset());
          if (region != null) {
            region.setExpanded(data.isExpanded);
          }
        }
      }
    };
    editor.getFoldingModel().runBatchFoldingOperation(operation);
  }
}
