// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package consulo.desktop.awt.editor.impl;

import consulo.codeEditor.impl.CodeEditorFoldingModelBase;
import consulo.codeEditor.impl.FoldRegionImpl;
import consulo.logging.Logger;

import jakarta.annotation.Nonnull;

public class DesktopFoldingModelImpl extends CodeEditorFoldingModelBase {
  private static final Logger LOG = Logger.getInstance(DesktopFoldingModelImpl.class);

  DesktopFoldingModelImpl(@Nonnull DesktopEditorImpl editor) {
    super(editor);
  }

  @Override
  public void onPlaceholderTextChanged(FoldRegionImpl region) {
    if (!myIsBatchFoldingProcessing) {
      LOG.error("Fold regions must be changed inside batchFoldProcessing() only");
    }
    myFoldRegionsProcessed = true;
    ((DesktopEditorImpl)myEditor).myView.invalidateFoldRegionLayout(region);
    notifyListenersOnFoldRegionStateChange(region);
  }

  @Override
  protected void notifyBatchFoldingProcessingDoneToEditor() {
    DesktopEditorImpl editor = (DesktopEditorImpl)myEditor;

    editor.updateCaretCursor();
    editor.recalculateSizeAndRepaint();
    editor.getGutterComponentEx().updateSize();
    editor.getGutterComponentEx().repaint();
    editor.invokeDelayedErrorStripeRepaint();
  }
}
