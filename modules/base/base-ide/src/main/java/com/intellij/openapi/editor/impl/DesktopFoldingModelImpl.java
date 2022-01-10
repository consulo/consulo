// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.openapi.editor.impl;

import consulo.annotation.DeprecationInfo;
import consulo.editor.impl.CodeEditorFoldingModelBase;
import consulo.logging.Logger;

import javax.annotation.Nonnull;

@Deprecated
@DeprecationInfo("Desktop only")
@SuppressWarnings("deprecation")
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
