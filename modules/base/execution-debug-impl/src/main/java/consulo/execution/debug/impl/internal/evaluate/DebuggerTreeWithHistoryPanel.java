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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.project.Project;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.tree.Tree;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class DebuggerTreeWithHistoryPanel<D> extends DebuggerTreeWithHistoryContainer<D> {
  private final JPanel myMainPanel;

  public DebuggerTreeWithHistoryPanel(@Nonnull D initialItem, @Nonnull DebuggerTreeCreator<D> creator, @Nonnull Project project) {
    super(initialItem, creator, project);
    myMainPanel = createMainPanel(myTreeCreator.createTree(initialItem));
  }

  @Override
  protected void updateContainer(Tree tree, String title) {
    Component component = ((BorderLayout)myMainPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
    myMainPanel.remove(component);
    myMainPanel.add(BorderLayout.CENTER, ScrollPaneFactory.createScrollPane(tree));
    myMainPanel.revalidate();
    myMainPanel.repaint();
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }
}
