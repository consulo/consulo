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
import consulo.ui.ex.awt.SimpleColoredComponent;

import javax.swing.*;
import java.awt.*;

/**
 * @author Vladislav.Soroka
 * @since 2014-03-25
 */
public abstract class CustomizeColoredTreeCellRendererReplacement extends CustomizeColoredTreeCellRenderer {
  @Override
  public final void customizeCellRenderer(SimpleColoredComponent renderer,
                                          JTree tree,
                                          Object value,
                                          boolean selected,
                                          boolean expanded,
                                          boolean leaf,
                                          int row,
                                          boolean hasFocus) {
  }

  public abstract Component getTreeCellRendererComponent(JTree tree,
                                                         Object value,
                                                         boolean selected,
                                                         boolean expanded,
                                                         boolean leaf,
                                                         int row,
                                                         boolean hasFocus);
}
