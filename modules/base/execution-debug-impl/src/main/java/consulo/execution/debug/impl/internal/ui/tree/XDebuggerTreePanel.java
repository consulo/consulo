/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.impl.internal.ui.tree;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.impl.internal.ui.DebuggerUIImplUtil;
import consulo.execution.debug.impl.internal.ui.tree.node.XValueNodeImpl;
import consulo.project.Project;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.dnd.DnDAction;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.dnd.DnDDragStartBean;
import consulo.ui.ex.awt.dnd.DnDSource;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public class XDebuggerTreePanel implements DnDSource {
  private final XDebuggerTree myTree;
  private final JPanel myMainPanel;

  public XDebuggerTreePanel(@Nonnull Project project,
                            @Nonnull XDebuggerEditorsProvider editorsProvider,
                            @Nonnull Disposable parentDisposable,
                            @Nullable XSourcePosition sourcePosition,
                            @Nonnull @NonNls String popupActionGroupId,
                            @Nullable XValueMarkers<?, ?> markers) {
    myTree = new XDebuggerTree(project, editorsProvider, sourcePosition, popupActionGroupId, markers);
    myMainPanel = new JPanel(new BorderLayout());
    myMainPanel.add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    Disposer.register(parentDisposable, myTree);
    Disposer.register(parentDisposable, myMainPanel::removeAll);
  }

  public XDebuggerTree getTree() {
    return myTree;
  }

  public JPanel getMainPanel() {
    return myMainPanel;
  }

  @Override
  public boolean canStartDragging(DnDAction action, Point dragOrigin) {
    return getNodesToDrag().length > 0;
  }

  private XValueNodeImpl[] getNodesToDrag() {
    return myTree.getSelectedNodes(XValueNodeImpl.class, node -> DebuggerUIImplUtil.hasEvaluationExpression(node.getValueContainer()));
  }

  @Override
  public DnDDragStartBean startDragging(DnDAction action, Point dragOrigin) {
    return new DnDDragStartBean(getNodesToDrag());
  }

  @Override
  public Pair<Image, Point> createDraggedImage(DnDAction action, Point dragOrigin, @Nonnull DnDDragStartBean bean) {
    XValueNodeImpl[] nodes = getNodesToDrag();
    if (nodes.length == 1) {
      return DnDAwareTree.getDragImage(myTree, nodes[0].getPath(), dragOrigin);
    }
    return DnDAwareTree.getDragImage(myTree, XDebuggerBundle.message("xdebugger.drag.text.0.elements", nodes.length), dragOrigin);
  }

  @Override
  public void dragDropEnd() {
  }

  @Override
  public void dropActionChanged(int gestureModifiers) {
  }
}
