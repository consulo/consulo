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
package consulo.execution.test.ui;

import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.execution.test.AbstractTestProxy;
import consulo.execution.test.TestConsoleProperties;
import consulo.ui.ex.awt.tree.AbstractTreeBuilder;
import consulo.ui.ex.tree.AbstractTreeStructure;
import consulo.ui.ex.tree.AlphaComparator;
import consulo.ui.ex.tree.IndexComparator;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

/**
 * @author: Roman Chernyatchik
 */
public abstract class AbstractTestTreeBuilder extends AbstractTreeBuilder {
  public AbstractTestTreeBuilder(
    final JTree tree,
    final DefaultTreeModel defaultTreeModel,
    final AbstractTreeStructure structure,
    final IndexComparator instance
  ) {
    super(tree, defaultTreeModel, structure, instance);
  }

  public AbstractTestTreeBuilder() {
    super();
  }

  public void repaintWithParents(final AbstractTestProxy testProxy) {
    AbstractTestProxy current = testProxy;
    do {
      DefaultMutableTreeNode node = getNodeForElement(current);
      if (node != null) {
        JTree tree = getTree();
        ((DefaultTreeModel)tree.getModel()).nodeChanged(node);
      }
      current = current.getParent();
    }
    while (current != null);
  }

  @Override
  protected boolean isAlwaysShowPlus(final NodeDescriptor descriptor) {
    return false;
  }

  @Override
  protected boolean isSmartExpand() {
    return false;
  }

  public void setTestsComparator(boolean sortAlphabetically) {
    setNodeDescriptorComparator(sortAlphabetically ? AlphaComparator.INSTANCE : null);
    queueUpdate();
  }

  public void setStatisticsComparator(TestConsoleProperties properties, boolean sortByStatistics) {
    if (!sortByStatistics) {
      setTestsComparator(TestConsoleProperties.SORT_ALPHABETICALLY.value(properties));
    }
    else {
      setNodeDescriptorComparator((o1, o2) -> {
        if (o1.getParentDescriptor() == o2.getParentDescriptor()
          && o1 instanceof BaseTestProxyNodeDescriptor nodeDescriptor1
          && o2 instanceof BaseTestProxyNodeDescriptor nodeDescriptor2) {
          final Long d1 = nodeDescriptor1.getElement().getDuration();
          final Long d2 = nodeDescriptor2.getElement().getDuration();
          return Comparing.compare(d2, d1);
        }
        return 0;
      });
    }
    queueUpdate();
  }
}
