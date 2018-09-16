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
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import javax.annotation.Nonnull;

import java.util.function.Function;

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
  public OffsetsInFile toFileCopy(PsiFile copyFile) {
    CompletionAssertions.assertCorrectOriginalFile("Given ", file, copyFile);
    assert copyFile.getViewProvider().getDocument().getTextLength() == file.getViewProvider().getDocument().getTextLength();
    return mapOffsets(copyFile, it -> it);
  }

  @Nonnull
  public OffsetsInFile toInjectedIfAny(int offset) {
    PsiFile injected = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset);
    if (injected == null) {
      return this;
    }
    DocumentWindow documentWindow = InjectedLanguageUtil.getDocumentWindow(injected);
    assert documentWindow != null;
    return mapOffsets(injected, documentWindow::hostToInjected);
  }

  @Nonnull
  public OffsetsInFile toTopLevelFile() {
    InjectedLanguageManager manager = InjectedLanguageManager.getInstance(file.getProject());
    PsiFile hostFile = manager.getTopLevelFile(file);
    if (hostFile == file) {
      return this;
    }
    else {
      return mapOffsets(hostFile, it -> manager.injectedToHost(file, it));
    }
  }

  @Nonnull
  public OffsetsInFile copyWithReplacement(int startOffset, int endOffset, String replacement) {
    PsiFile fileCopy = (PsiFile)file.copy();
    Document document = fileCopy.getViewProvider().getDocument();

    document.setText(file.getViewProvider().getDocument().getImmutableCharSequence()); // original file might be uncommitted

    OffsetsInFile result = toFileCopy(fileCopy);
    document.replaceString(startOffset, endOffset, replacement);

    PsiDocumentManager.getInstance(file.getProject()).commitDocument(document);

    return result;
  }

  @Nonnull
  private OffsetsInFile mapOffsets(PsiFile newFile, Function<Integer, Integer> offsetFun) {
    OffsetMap map = new OffsetMap(newFile.getViewProvider().getDocument());
    for (OffsetKey key : offsets.getAllOffsets()) {
      map.addOffset(key, offsetFun.apply(offsets.getOffset(key)));
    }
    return new OffsetsInFile(newFile, map);
  }
}
