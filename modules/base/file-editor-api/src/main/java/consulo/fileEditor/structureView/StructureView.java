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
package consulo.fileEditor.structureView;

import consulo.disposer.Disposable;
import consulo.fileEditor.FileEditor;
import consulo.project.Project;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * Defines the implementation of a custom structure view or file structure popup component.
 * The structure view is linked to a file editor and displays the structure of the file
 * contained in that editor.
 *
 * @see StructureViewBuilder#createStructureView(FileEditor, Project)
 * @see TreeBasedStructureViewBuilder
 */

public interface StructureView extends Disposable {
  /**
   * Returns the editor whose structure is displayed in the structure view.
   *
   * @return the editor linked to the structure view.
   */
  FileEditor getFileEditor();

  /**
   * Selects the element which corresponds to the current cursor position in the editor
   * linked to the structure view.
   *
   * @param requestFocus if true, the structure view component also grabs the focus.
   */
  // TODO: drop return value?
  boolean navigateToSelectedElement(boolean requestFocus);

  /**
   * Returns the Swing component representing the structure view.
   *
   * @return the structure view component.
   */
  JComponent getComponent();

  // TODO: remove from OpenAPI?
  void centerSelectedRow();

  /**
   * Restores the state of the structure view (the expanded and selected elements)
   * from the user data of the file editor to which it is linked.
   *
   * @see FileEditor#getUserData(Key)
   */
  void restoreState();

  /**
   * Stores the state of the structure view (the expanded and selected elements)
   * in the user data of the file editor to which it is linked.
   *
   * @see FileEditor#putUserData(Key, Object)
   */
  void storeState();

  @Nonnull
  StructureViewModel getTreeModel();

  interface Scrollable extends StructureView {
    Dimension getCurrentSize();
    void setReferenceSizeWhileInitializing(Dimension size);
  }
}
