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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.frame.XCompositeNode;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueChildrenList;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.impl.internal.evaluate.XDebuggerEvaluationDialog;
import consulo.execution.debug.impl.internal.evaluate.XEvaluationCallbackBase;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.ui.ex.awt.UIUtil;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public class EvaluatingExpressionRootNode extends XValueContainerNode<EvaluatingExpressionRootNode.EvaluatingResultContainer> {
  public EvaluatingExpressionRootNode(XDebuggerEvaluationDialog evaluationDialog, final XDebuggerTree tree) {
    super(tree, null, new EvaluatingResultContainer(evaluationDialog));
    setLeaf(false);
  }

  @Override
  protected MessageTreeNode createLoadingMessageNode() {
    return MessageTreeNode.createEvaluatingMessage(myTree, this);
  }

  public static class EvaluatingResultContainer extends XValueContainer {
    private final XDebuggerEvaluationDialog myDialog;

    public EvaluatingResultContainer(final XDebuggerEvaluationDialog dialog) {
      myDialog = dialog;
    }

    @Override
    public void computeChildren(@Nonnull final XCompositeNode node) {
      myDialog.startEvaluation(new XEvaluationCallbackBase() {
        @Override
        public void evaluated(@Nonnull final XValue result) {
          String name = UIUtil.removeMnemonic(XDebuggerBundle.message("xdebugger.evaluate.result"));
          node.addChildren(XValueChildrenList.singleton(name, result), true);
          myDialog.evaluationDone();
        }

        @Override
        public void errorOccurred(@Nonnull final String errorMessage) {
          node.setErrorMessage(errorMessage);
          myDialog.evaluationDone();
        }
      });
    }
  }
}
