/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package consulo.language.editor.structureView;

import consulo.fileEditor.structureView.tree.Sorter;
import consulo.codeEditor.Editor;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
public class StructureViewModelBase extends TextEditorBasedStructureViewModel {
  private final StructureViewTreeElement myRoot;
  private Sorter[] mySorters = Sorter.EMPTY_ARRAY;
  private Class[] mySuitableClasses = null;

  public StructureViewModelBase(@Nonnull PsiFile psiFile, @Nullable Editor editor, @Nonnull StructureViewTreeElement root) {
    super(editor, psiFile);

    myRoot = root;
  }

  public StructureViewModelBase(@Nonnull PsiFile psiFile, @Nonnull StructureViewTreeElement root) {
    this(psiFile, null, root);
  }

  @Override
  @Nonnull
  public StructureViewTreeElement getRoot() {
    return myRoot;
  }

  @Nonnull
  public StructureViewModelBase withSorters(@Nonnull Sorter... sorters) {
    mySorters = sorters;
    return this;
  }

  @Nonnull
  public StructureViewModelBase withSuitableClasses(@Nonnull Class... suitableClasses) {
    mySuitableClasses = suitableClasses;
    return this;
  }

  @Nonnull
  @Override
  public Sorter[] getSorters() {
    return mySorters;
  }

  @Nonnull
  @Override
  protected Class[] getSuitableClasses() {
    if (mySuitableClasses != null) {
      return mySuitableClasses;
    }
    return super.getSuitableClasses();
  }
}
