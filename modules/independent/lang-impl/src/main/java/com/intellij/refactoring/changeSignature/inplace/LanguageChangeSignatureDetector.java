/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.refactoring.changeSignature.inplace;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

public interface LanguageChangeSignatureDetector<C extends ChangeInfo> {

  @Nonnull
  C createInitialChangeInfo(final @Nonnull PsiElement element);
  boolean ignoreChanges(PsiElement element);
  @Nullable C createNextChangeInfo(String signature, @Nonnull C currentInfo, boolean delegate);

  void performChange(C changeInfo, Editor editor, @Nonnull String oldText);

  boolean isChangeSignatureAvailableOnElement(@Nonnull PsiElement element, C currentInfo);

  TextRange getHighlightingRange(@Nonnull C changeInfo);

  default @Nonnull
  String extractSignature(@Nonnull C initialChangeInfo) {
    final TextRange signatureRange = getHighlightingRange(initialChangeInfo);
    return signatureRange.shiftRight(-signatureRange.getStartOffset()).substring(initialChangeInfo.getMethod().getText());
  }

  default String getMethodSignaturePreview(C info, final List<TextRange> deleteRanges, final List<TextRange> newRanges) {
    return extractSignature(info);
  }

  FileType getFileType();
}
