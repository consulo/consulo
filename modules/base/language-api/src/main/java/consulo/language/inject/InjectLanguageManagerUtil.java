/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.inject;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.TextRange;
import consulo.language.file.inject.VirtualFileWindow;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.PsiUtilCore;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class InjectLanguageManagerUtil {
  /**
   * Used in RegExp plugin for value pattern annotator
   *
   * Annotator that is used to validate the "Value-Pattern" textfield: The regex entered there should contain exactly
   * one capturing group that determines the text-range the configured language will be injected into.
   */
  public static final Key<Boolean> VALUE_PATTERN_KEY_FOR_ADVANCED_INJECT = Key.create("IS_VALUE_REGEXP");

  @RequiredReadAction
  public static boolean isInInjectedLanguagePrefixSuffix(@Nonnull final PsiElement element) {
    PsiFile injectedFile = element.getContainingFile();
    if (injectedFile == null) return false;
    Project project = injectedFile.getProject();
    InjectedLanguageManager languageManager = InjectedLanguageManager.getInstance(project);
    if (!languageManager.isInjectedFragment(injectedFile)) return false;
    TextRange elementRange = element.getTextRange();
    List<TextRange> edibles = languageManager.intersectWithAllEditableFragments(injectedFile, elementRange);
    int combinedEdiblesLength = edibles.stream().mapToInt(TextRange::getLength).sum();

    return combinedEdiblesLength != elementRange.getLength();
  }

  @Nullable
  @RequiredReadAction
  public static PsiLanguageInjectionHost findInjectionHost(@Nullable PsiElement psi) {
    if (psi == null) return null;
    InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(psi.getProject());
    PsiFile containingFile = psi.getContainingFile().getOriginalFile();
    PsiElement fileContext = containingFile.getContext();
    if (fileContext instanceof PsiLanguageInjectionHost) return (PsiLanguageInjectionHost)fileContext;
    PsiLanguageInjectionHost.Place shreds = injectedLanguageManager.getShreds(containingFile.getViewProvider());
    if (shreds == null) {
      VirtualFile virtualFile = PsiUtilCore.getVirtualFile(containingFile);
      if (virtualFile instanceof LightVirtualFile) {
        virtualFile = ((LightVirtualFile)virtualFile).getOriginalFile();
      }
      if (virtualFile instanceof VirtualFileWindow) {
        shreds = injectedLanguageManager.getShreds(((VirtualFileWindow)virtualFile).getDocumentWindow());
      }
    }
    return shreds != null ? shreds.getHostPointer().getElement() : null;
  }
}
