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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.TreeCellEditor;

/**
 * @author Vladislav.Soroka
 * @since 2014-03-26
 */
public interface EditableMessageElement {
  @Nonnull
  TreeCellEditor getRightSelfEditor();
  @Nullable
  CustomizeColoredTreeCellRenderer getLeftSelfRenderer();

  boolean startEditingOnMouseMove();
}
