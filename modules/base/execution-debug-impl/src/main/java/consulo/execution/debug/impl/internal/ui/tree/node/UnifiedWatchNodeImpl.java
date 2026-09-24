/*
 * Copyright 2013-2026 consulo.io
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

import consulo.execution.debug.Obsolescent;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.frame.presentation.XErrorValuePresentation;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.internal.XEvaluationCallbackBase;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * A watch - the counterpart of the swing {@link WatchNodeImpl}. Its expression is evaluated in the frame shown when
 * the node is made, and the node then is the value the expression evaluated to, or the error of the evaluator.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedWatchNodeImpl extends UnifiedXValueNodeImpl {
    private final XExpression myExpression;

    public UnifiedWatchNodeImpl(UnifiedXDebuggerTree tree, UnifiedWatchesRootNode parent, XExpression expression, @Nullable XStackFrame stackFrame) {
        this(tree, parent, expression, LocalizeValue.of(expression.getExpression()), new XWatchValue(expression, stackFrame));
    }

    UnifiedWatchNodeImpl(
        UnifiedXDebuggerTree tree,
        UnifiedWatchesRootNode parent,
        XExpression expression,
        @Nullable XStackFrame stackFrame,
        LocalizeValue name
    ) {
        this(tree, parent, expression, name, new XWatchValue(expression, stackFrame));
    }

    UnifiedWatchNodeImpl(UnifiedXDebuggerTree tree, UnifiedWatchesRootNode parent, XExpression expression, LocalizeValue name, XValue value) {
        super(tree, parent, name, value);
        myExpression = expression;
    }

    public XExpression getExpression() {
        return myExpression;
    }

    @Override
    public XValue getValueContainer() {
        XValue container = super.getValueContainer();
        if (container instanceof XWatchValue watchValue) {
            XValue value = watchValue.myValue;
            if (value != null) {
                return value;
            }
        }
        return container;
    }

    void computePresentationIfNeeded() {
        if (getValuePresentation() == null) {
            getValueContainer().computePresentation(this, XValuePlace.TREE);
        }
    }

    protected void evaluated() {
    }

    private static class XWatchValue extends XNamedValue {
        private final XExpression myExpression;
        private final @Nullable XStackFrame myStackFrame;
        private volatile @Nullable XValue myValue;

        public XWatchValue(XExpression expression, @Nullable XStackFrame stackFrame) {
            super(expression.getExpression());
            myExpression = expression;
            myStackFrame = stackFrame;
        }

        @Override
        public void computeChildren(XCompositeNode node) {
            XValue value = myValue;
            if (value != null) {
                value.computeChildren(node);
            }
            else {
                node.addChildren(XValueChildrenList.EMPTY, true);
            }
        }

        @Override
        public void computePresentation(XValueNode node, XValuePlace place) {
            XStackFrame stackFrame = myStackFrame;
            if (stackFrame != null) {
                XDebuggerEvaluator evaluator = stackFrame.getEvaluator();
                if (evaluator != null) {
                    evaluator.evaluate(myExpression, new MyEvaluationCallback(node, place), stackFrame.getSourcePosition());
                }
            }
            else {
                node.setPresentation(ExecutionDebugIconGroup.nodeWatch(), EMPTY_PRESENTATION, false);
            }
        }

        private class MyEvaluationCallback extends XEvaluationCallbackBase implements Obsolescent {
            private final XValueNode myNode;
            private final XValuePlace myPlace;

            public MyEvaluationCallback(XValueNode node, XValuePlace place) {
                myNode = node;
                myPlace = place;
            }

            @Override
            public boolean isObsolete() {
                return myNode.isObsolete();
            }

            @Override
            public void evaluated(XValue result) {
                myValue = result;

                if (myNode instanceof UnifiedWatchNodeImpl watchNode) {
                    watchNode.evaluated();
                }

                result.computePresentation(myNode, myPlace);
            }

            @Override
            public void errorOccurred(LocalizeValue errorMessage) {
                myNode.setPresentation(XDebuggerUIConstants.ERROR_MESSAGE_ICON, new XErrorValuePresentation(errorMessage), false);
            }
        }

        private static final XValuePresentation EMPTY_PRESENTATION = new XValuePresentation() {
            @Override
            public String getSeparator() {
                return "";
            }

            @Override
            public void renderValue(XValueTextRenderer renderer) {
            }
        };
    }
}
