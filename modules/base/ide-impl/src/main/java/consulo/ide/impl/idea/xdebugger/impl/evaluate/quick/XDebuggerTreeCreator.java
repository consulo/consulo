/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.evaluate.quick;

import consulo.util.concurrent.ResultConsumer;
import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.ui.ex.awt.tree.Tree;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.XDebuggerActions;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.DebuggerTreeCreator;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import javax.annotation.Nonnull;

public class XDebuggerTreeCreator implements DebuggerTreeCreator<Pair<XValue,String>> {
  @Nonnull
  private final Project myProject;
  private final XDebuggerEditorsProvider myProvider;
  private final XSourcePosition myPosition;
  private final XValueMarkers<?, ?> myMarkers;

  public XDebuggerTreeCreator(@Nonnull Project project, XDebuggerEditorsProvider editorsProvider, XSourcePosition sourcePosition,
                              XValueMarkers<?, ?> markers) {
    myProject = project;
    myProvider = editorsProvider;
    myPosition = sourcePosition;
    myMarkers = markers;
  }

  @Nonnull
  @Override
  public Tree createTree(@Nonnull Pair<XValue, String> descriptor) {
    XDebuggerTree tree = new XDebuggerTree(myProject, myProvider, myPosition, XDebuggerActions.INSPECT_TREE_POPUP_GROUP, myMarkers);
    tree.setRoot(new XValueNodeImpl(tree, null, descriptor.getSecond(), descriptor.getFirst()), true);
    return tree;
  }

  @Nonnull
  @Override
  public String getTitle(@Nonnull Pair<XValue, String> descriptor) {
    return descriptor.getSecond();
  }

  @Override
  public void createDescriptorByNode(Object node, ResultConsumer<Pair<XValue, String>> resultConsumer) {
    if (node instanceof XValueNodeImpl) {
      XValueNodeImpl valueNode = (XValueNodeImpl)node;
      resultConsumer.onSuccess(Pair.create(valueNode.getValueContainer(), valueNode.getName()));
    }
  }
}