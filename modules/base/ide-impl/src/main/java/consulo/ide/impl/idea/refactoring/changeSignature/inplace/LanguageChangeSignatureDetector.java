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
package consulo.ide.impl.idea.refactoring.changeSignature.inplace;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.component.extension.ExtensionPointCacheKey;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.changeSignature.ChangeInfo;
import consulo.language.Language;
import consulo.language.extension.ByLanguageValue;
import consulo.language.extension.LanguageExtension;
import consulo.language.extension.LanguageOneToOne;
import consulo.language.file.FileTypeManager;
import consulo.language.psi.PsiElement;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface LanguageChangeSignatureDetector<C extends ChangeInfo> extends LanguageExtension {
  ExtensionPointCacheKey<LanguageChangeSignatureDetector, ByLanguageValue<LanguageChangeSignatureDetector>> KEY =
          ExtensionPointCacheKey.create("LanguageChangeSignatureDetector", LanguageOneToOne.build());

  @Nullable
  @SuppressWarnings("unchecked")
  static LanguageChangeSignatureDetector<ChangeInfo> forLanguage(@Nonnull Language language) {
    return Application.get().getExtensionPoint(LanguageChangeSignatureDetector.class).getOrBuildCache(KEY).get(language);
  }

  @Nonnull
  C createInitialChangeInfo(final @Nonnull PsiElement element);

  boolean ignoreChanges(PsiElement element);

  @Nullable
  C createNextChangeInfo(String signature, @Nonnull C currentInfo, boolean delegate);

  void performChange(C changeInfo, Editor editor, @Nonnull String oldText);

  boolean isChangeSignatureAvailableOnElement(@Nonnull PsiElement element, C currentInfo);

  TextRange getHighlightingRange(@Nonnull C changeInfo);

  default
  @Nonnull
  String extractSignature(@Nonnull C initialChangeInfo) {
    final TextRange signatureRange = getHighlightingRange(initialChangeInfo);
    return signatureRange.shiftRight(-signatureRange.getStartOffset()).substring(initialChangeInfo.getMethod().getText());
  }

  default String getMethodSignaturePreview(C info, final List<TextRange> deleteRanges, final List<TextRange> newRanges) {
    return extractSignature(info);
  }

  @Nonnull
  default FileType getFileType() {
    return FileTypeManager.getInstance().findFileTypeByLanguage(getLanguage());
  }
}
