/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Comparator;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 12-Sep-17
 */
public interface TreeModel<N> {
  void buildChildren(@Nonnull Function<N, TreeNode<N>> nodeFactory, @Nullable N parentValue);

  /**
   * If return true - children will be build for current node, and node will be marked as leaf (if not children)
   */
  default boolean isNeedBuildChildrenBeforeOpen(@Nonnull TreeNode<N> node) {
    return false;
  }

  /**
   * @return expand on double click
   */
  default boolean onDoubleClick(@Nonnull Tree<N> tree, @Nonnull TreeNode<N> node) {
    return true;
  }

  @Nullable
  default Comparator<TreeNode<N>> getNodeComparator() {
    return null;
  }
}
