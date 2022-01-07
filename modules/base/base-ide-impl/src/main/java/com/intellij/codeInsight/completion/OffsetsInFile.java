/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.completion;

import com.intellij.injected.editor.DocumentWindow;
import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.ChangedPsiRangeUtil;
import com.intellij.psi.impl.DiffLog;
import com.intellij.psi.impl.source.tree.FileElement;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.psi.text.BlockSupport;
import com.intellij.util.ObjectUtil;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 02-May-17
 * <p>
 * from kotlin platform\lang-impl\src\com\intellij\codeInsight\completion\OffsetsInFile.kt
 */
public class OffsetsInFile {
  private PsiFile file;
  private OffsetMap offsets;

  public OffsetsInFile(PsiFile file) {
    this(file, new OffsetMap(file.getViewProvider().getDocument()));
  }

  public OffsetsInFile(PsiFile file, OffsetMap offsets) {
    this.file = file;
    this.offsets = offsets;
  }

  public OffsetMap getOffsets() {
    return offsets;
  }

  public PsiFile getFile() {
    return file;
  }

  @Nonnull
  public OffsetsInFile toTopLevelFile() {
    InjectedLanguageManager manager = InjectedLanguageManager.getInstance(file.getProject());
    PsiFile hostFile = manager.getTopLevelFile(file);
    if (hostFile == file) {
      return this;
    }
    else {
      return new OffsetsInFile(hostFile, offsets.mapOffsets(hostFile.getViewProvider().getDocument(), it -> manager.injectedToHost(file, it)));
    }
  }

  @Nonnull
  public OffsetsInFile toInjectedIfAny(int offset) {
    PsiFile injected = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset);
    if (injected == null) {
      return this;
    }
    DocumentWindow documentWindow = InjectedLanguageUtil.getDocumentWindow(injected);
    assert documentWindow != null;
    return new OffsetsInFile(injected, offsets.mapOffsets(documentWindow, documentWindow::hostToInjected));
  }

  @Nonnull
  public OffsetsInFile copyWithReplacement(int startOffset, int endOffset, String replacement) {
    return replaceInCopy((PsiFile)file.copy(), startOffset, endOffset, replacement);
  }

  @Nonnull
  public OffsetsInFile replaceInCopy(PsiFile fileCopy, int startOffset, int endOffset, String replacement) {
    DocumentImpl tempDocument = new DocumentImpl(offsets.getDocument().getImmutableCharSequence(), true);

    OffsetMap tempMap = offsets.copyOffsets(tempDocument);

    tempDocument.replaceString(startOffset, endOffset, replacement);

    reparseFile(fileCopy, tempDocument.getImmutableCharSequence());

    OffsetMap copyOffsets = tempMap.copyOffsets(fileCopy.getViewProvider().getDocument());
    return new OffsetsInFile(fileCopy, copyOffsets);
  }

  private void reparseFile(PsiFile file, CharSequence newText) {
    FileElement node = ObjectUtil.tryCast(file.getNode(), FileElement.class);
    if(node == null) {
      throw new IllegalArgumentException(file.getClass() + " " + file.getFileType());
    }

    TextRange range = ChangedPsiRangeUtil.getChangedPsiRange(file, node, newText);
    if(range == null) {
      return;
    }

    ProgressIndicator indicator = ProgressManager.getGlobalProgressIndicator();
    if(indicator == null) {
      indicator = new EmptyProgressIndicator();
    }

    DiffLog log = BlockSupport.getInstance(file.getProject()).reparseRange(file, node, range, newText, indicator, file.getViewProvider().getContents());

    ProgressManager.getInstance().executeNonCancelableSection(() -> log.doActualPsiChange(file));
  }
}
