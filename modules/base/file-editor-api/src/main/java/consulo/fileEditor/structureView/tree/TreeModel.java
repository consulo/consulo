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
package consulo.fileEditor.structureView.tree;

import jakarta.annotation.Nonnull;

/**
 * Model of a generic tree control displayed in the IDEA user interface, with a set
 * of actions for grouping, sorting and filtering. Used, for example, for the structure
 * view tree.
 */
public interface TreeModel {
  /**
   * Returns the root element of the tree.
   *
   * @return the tree root.
   */
  @Nonnull
  TreeElement getRoot();

  /**
   * Returns the list of actions for grouping items in the tree.
   *
   * @return the array of grouping actions.
   * @see Grouper#EMPTY_ARRAY
   */
  @Nonnull
  Grouper[] getGroupers();

  /**
   * Returns the array of actions for sorting items in the tree.
   *
   * @return the array of sorting actions.
   * @see Sorter#EMPTY_ARRAY
   */
  @Nonnull
  Sorter[] getSorters();

  /**
   * Returns the array of actions for filtering items in the tree.
   *
   * @return the array of filtering actions.
   * @see Filter#EMPTY_ARRAY
   */
  @Nonnull
  Filter[] getFilters();
}
