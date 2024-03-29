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
import java.util.Collection;

/**
 * Action for grouping items in a generic tree.
 *
 * @see TreeModel#getGroupers()
 */
public interface Grouper extends TreeAction {
  Grouper[] EMPTY_ARRAY = new Grouper[0];

  /**
   * Returns the collection of groups into which the children of the specified parent node
   * are grouped.
   *
   * @param parent   the parent node.
   * @param children the children of the parent node.
   * @return the collection of groups
   */
  @Nonnull
  Collection<Group> group(final Object parent, Collection<TreeElement> children);
}
