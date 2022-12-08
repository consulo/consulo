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

package consulo.language.editor.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.language.editor.folding.EditorFolding;
import consulo.language.inject.InjectedLanguageManager;
import consulo.codeEditor.Editor;
import consulo.codeEditor.FoldRegion;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds {@code 'fold region -> PSI element'} mappings.
 * <p/>
 * Not thread-safe.
 */
public class EditorFoldingInfoImpl implements EditorFolding {
  private static final Key<EditorFoldingInfoImpl> KEY = Key.create("EditorFoldingInfoImpl.KEY");

  private final Map<FoldRegion, SmartPsiElementPointer<?>> myFoldRegionToSmartPointerMap = new HashMap<>();

  @Nonnull
  public static EditorFoldingInfoImpl get(@Nonnull Editor editor) {
    EditorFoldingInfoImpl info = editor.getUserData(KEY);
    if (info == null){
      info = new EditorFoldingInfoImpl();
      editor.putUserData(KEY, info);
    }
    return info;
  }

  @Override
  @Nullable
  @RequiredReadAction
  public PsiElement getPsiElement(@Nonnull FoldRegion region) {
    final SmartPsiElementPointer<?> pointer = myFoldRegionToSmartPointerMap.get(region);
    if (pointer == null) {
      return null;
    }
    PsiElement element = pointer.getElement();
    return element != null && element.isValid() ? element : null;
  }

  @Override
  @Nullable
  @RequiredReadAction
  public TextRange getPsiElementRange(@Nonnull FoldRegion region) {
    PsiElement element = getPsiElement(region);
    if (element == null) return null;
    PsiFile containingFile = element.getContainingFile();
    InjectedLanguageManager injectedManager = InjectedLanguageManager.getInstance(containingFile.getProject());
    boolean isInjected = injectedManager.isInjectedFragment(containingFile);
    TextRange range = element.getTextRange();
    if (isInjected) {
      range = injectedManager.injectedToHost(element, range);
    }
    return range;
  }

  public void addRegion(@Nonnull FoldRegion region, @Nonnull SmartPsiElementPointer<?> pointer){
    myFoldRegionToSmartPointerMap.put(region, pointer);
  }

  public void removeRegion(@Nonnull FoldRegion region){
    myFoldRegionToSmartPointerMap.remove(region);
  }

  public void dispose() {
    myFoldRegionToSmartPointerMap.clear();
  }
}
