/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.structureView.impl;

import consulo.fileEditor.structureView.event.FileEditorPositionListener;
import consulo.fileEditor.structureView.event.ModelListener;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.StructureViewTreeElement;
import consulo.fileEditor.structureView.tree.Filter;
import consulo.fileEditor.structureView.tree.Grouper;
import consulo.fileEditor.structureView.tree.Sorter;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import jakarta.annotation.Nonnull;

/**
 * @author cdr
 */
public class StructureViewModelWrapper implements StructureViewModel {
  private final StructureViewModel myStructureViewModel;
  private final PsiFile myMainFile;

  public StructureViewModelWrapper(StructureViewModel structureViewModel, PsiFile mainFile) {
    myStructureViewModel = structureViewModel;
    myMainFile = mainFile;
  }

  @Override
  public Object getCurrentEditorElement() {
    return myStructureViewModel.getCurrentEditorElement();
  }

  @Override
  public void addEditorPositionListener(final FileEditorPositionListener listener) {
    myStructureViewModel.addEditorPositionListener(listener);
  }

  @Override
  public void removeEditorPositionListener(final FileEditorPositionListener listener) {
    myStructureViewModel.removeEditorPositionListener(listener);
  }

  @Override
  public void addModelListener(final ModelListener modelListener) {
    myStructureViewModel.addModelListener(modelListener);
  }

  @Override
  public void removeModelListener(final ModelListener modelListener) {
    myStructureViewModel.removeModelListener(modelListener);
  }

  @Override
  @Nonnull
  public StructureViewTreeElement getRoot() {
    return new StructureViewElementWrapper<PsiElement>(myStructureViewModel.getRoot(), myMainFile);
  }

  @Override
  public void dispose() {
    myStructureViewModel.dispose();
  }

  @Override
  public boolean shouldEnterElement(final Object element) {
    return false;
  }

  @Override
  @Nonnull
  public Grouper[] getGroupers() {
    return myStructureViewModel.getGroupers();
  }

  @Override
  @Nonnull
  public Sorter[] getSorters() {
    return myStructureViewModel.getSorters();
  }

  @Override
  @Nonnull
  public Filter[] getFilters() {
    return myStructureViewModel.getFilters();
  }
}
