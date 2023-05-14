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
package consulo.ui.ex.awt;

import consulo.navigation.Navigatable;
import consulo.ui.ex.awt.tree.ExpandOnDoubleClick;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.dataholder.Key;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

/**
 * @author VISTALL
 * @since 26-Feb-22
 */
public class EditSourceOnDoubleClickHandlerBase {
  public static final Key<Boolean> INSTALLED = Key.create("EditSourceOnDoubleClickHandlerInstalled");

  /**
   * @return {@code true} to expand/collapse the node, {@code false} to navigate to source if possible
   */
  public static boolean isExpandPreferable(@Nonnull JTree tree, @Nullable TreePath path) {
    if (path == null) return false; // path is not specified

    ExpandOnDoubleClick behavior = ExpandOnDoubleClick.getBehavior(tree);
    if (behavior == ExpandOnDoubleClick.NEVER) return false; // disable expand/collapse

    TreeModel model = tree.getModel();
    if (model == null || model.isLeaf(path.getLastPathComponent())) return false;
    if (!UIUtil.isClientPropertyTrue(tree, INSTALLED)) return true; // expand by default if handler is not installed

    // navigate to source is preferred if the tree provides a navigatable object for the given path
    if (behavior == ExpandOnDoubleClick.NAVIGATABLE) {
      Navigatable navigatable = TreeUtil.getNavigatable(tree, path);
      if (navigatable != null && navigatable.canNavigateToSource()) return false;
    }
    if (behavior == ExpandOnDoubleClick.ALWAYS) return true;
    // for backward compatibility
    NodeDescriptor<?> descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, path);
    return descriptor == null || descriptor.expandOnDoubleClick();
  }

}
