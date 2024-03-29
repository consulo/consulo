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
package consulo.fileEditor.structureView;

import consulo.codeEditor.Editor;
import consulo.disposer.Disposer;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.project.Project;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Default implementation of the {@link StructureViewBuilder} interface which uses the
 * standard IDEA implementation of the {@link StructureView} component and allows to
 * customize the data displayed in the structure view.
 *
 * @see StructureViewModel
 * @see TextEditorBasedStructureViewModel
 * @see consulo.ide.impl.idea.lang.LanguageStructureViewBuilder#getStructureViewBuilder(PsiFile)
 */
public abstract class TreeBasedStructureViewBuilder implements StructureViewBuilder {
  /**
   * Returns the structure view model defining the data displayed in the structure view
   * for a specific file.
   *
   * @return the structure view model instance.
   * @see TextEditorBasedStructureViewModel
   */
  @Nonnull
  public abstract StructureViewModel createStructureViewModel(@Nullable Editor editor);

  @Override
  @Nonnull
  public StructureView createStructureView(FileEditor fileEditor, @Nonnull Project project) {
    final StructureViewModel model = createStructureViewModel(fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : null);
    StructureView view = StructureViewFactory.getInstance(project).createStructureView(fileEditor, model, project, isRootNodeShown());
    Disposer.register(view, model);
    return view;
  }

  /**
   * Override returning <code>false</code> if root node created by {@link #createStructureViewModel(Editor editor)} shall not be visible
   *
   * @return <code>false</code> if root node shall not be visible in structure tree.
   */
  public boolean isRootNodeShown() {
    return true;
  }
}
