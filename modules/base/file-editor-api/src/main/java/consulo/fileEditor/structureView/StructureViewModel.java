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
import consulo.disposer.Disposable;
import consulo.fileEditor.structureView.event.FileEditorPositionListener;
import consulo.fileEditor.structureView.event.ModelListener;
import consulo.fileEditor.structureView.tree.TreeModel;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Defines the model for the data displayed in the standard structure view or file structure
 * popup component. The model of the standard structure view is represented as a tree of elements.
 *
 * @see TreeBasedStructureViewBuilder#createStructureViewModel(Editor)
 * @see TextEditorBasedStructureViewModel
 */
public interface StructureViewModel extends TreeModel, Disposable {
  /**
   * Returns the element currently selected in the editor linked to the structure view.
   *
   * @return the selected element, or null if the current editor position does not
   * correspond to any element that can be shown in the structure view.
   */
  @Nullable
  Object getCurrentEditorElement();

  /**
   * Adds a listener which gets notified when the selection in the editor linked to the
   * structure view moves to a different element visible in the structure view.
   *
   * @param listener the listener to add.
   */
  void addEditorPositionListener(@Nonnull FileEditorPositionListener listener);

  /**
   * Removes a listener which gets notified when the selection in the editor linked to the
   * structure view moves to a different element visible in the structure view.
   *
   * @param listener the listener to remove.
   */
  void removeEditorPositionListener(@Nonnull FileEditorPositionListener listener);

  /**
   * Adds a listener which gets notified when the data represented by the structure view
   * is changed and the structure view needs to be rebuilt.
   *
   * @param modelListener the listener to add.
   */
  void addModelListener(@Nonnull ModelListener modelListener);

  /**
   * Removes a listener which gets notified when the data represented by the structure view
   * is changed and the structure view needs to be rebuilt.
   *
   * @param modelListener the listener to remove.
   */
  void removeModelListener(@Nonnull ModelListener modelListener);

  /**
   * Returns the root element of the structure view tree.
   *
   * @return the structure view root.
   */
  @Override
  @Nonnull
  StructureViewTreeElement getRoot();

  /**
   * Disposes of the model.
   */
  @Override
  void dispose();

  boolean shouldEnterElement(Object element);

  interface ElementInfoProvider extends StructureViewModel {
    boolean isAlwaysShowsPlus(StructureViewTreeElement element);

    boolean isAlwaysLeaf(StructureViewTreeElement element);
  }

  interface ExpandInfoProvider {
    boolean isAutoExpand(@Nonnull StructureViewTreeElement element);

    boolean isSmartExpand();

    /**
     * @return number of levels that would be always expanded in structure view.
     * Returns 2 by default: root node and its immediate children.
     * @apiNote Be careful with using this method because this approach is planned to be rewritten.
     */
    default int getMinimumAutoExpandDepth() {
      return 2;
    }
  }
}
