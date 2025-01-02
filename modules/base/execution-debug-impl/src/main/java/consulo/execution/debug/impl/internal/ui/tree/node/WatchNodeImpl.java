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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.application.ApplicationManager;
import consulo.execution.debug.Obsolescent;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.evaluation.XInstanceEvaluator;
import consulo.execution.debug.frame.*;
import consulo.execution.debug.frame.presentation.XErrorValuePresentation;
import consulo.execution.debug.frame.presentation.XValuePresentation;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.evaluate.XEvaluationCallbackBase;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.util.concurrent.AsyncResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class WatchNodeImpl extends XValueNodeImpl implements WatchNode {
    private final XExpression myExpression;

    public WatchNodeImpl(@Nonnull XDebuggerTree tree,
                         @Nonnull WatchesRootNode parent,
                         @Nonnull XExpression expression,
                         @Nullable XStackFrame stackFrame) {
        super(tree, parent, expression.getExpression(), new XWatchValue(expression, tree, stackFrame));
        myExpression = expression;
    }

    WatchNodeImpl(@Nonnull XDebuggerTree tree,
                  @Nonnull WatchesRootNode parent,
                  @Nonnull XExpression expression,
                  @Nullable XStackFrame stackFrame,
                  @Nonnull String name) {
        this(tree, parent, expression, name, new XWatchValue(expression, tree, stackFrame));
    }

    WatchNodeImpl(@Nonnull XDebuggerTree tree,
                  @Nonnull WatchesRootNode parent,
                  @Nonnull XExpression expression,
                  @Nonnull String name,
                  @Nonnull XValue value) {
        super(tree, parent, name, value);
        myExpression = expression;
    }

    @Override
    @Nonnull
    public XExpression getExpression() {
        return myExpression;
    }

    @Nonnull
    @Override
    public XValue getValueContainer() {
        XValue container = super.getValueContainer();
        if (container instanceof XWatchValue) {
            XValue value = ((XWatchValue) container).myValue;
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
        private final XDebuggerTree myTree;
        private final XStackFrame myStackFrame;
        private volatile XValue myValue;

        public XWatchValue(XExpression expression, XDebuggerTree tree, XStackFrame stackFrame) {
            super(expression.getExpression());
            myExpression = expression;
            myTree = tree;
            myStackFrame = stackFrame;
        }

        @Override
        public void computeChildren(@Nonnull XCompositeNode node) {
            if (myValue != null) {
                myValue.computeChildren(node);
            }
        }

        @Override
        public void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
            if (myStackFrame != null) {
                if (myTree.isShowing() || ApplicationManager.getApplication().isUnitTestMode()) {
                    XDebuggerEvaluator evaluator = myStackFrame.getEvaluator();
                    if (evaluator != null) {
                        evaluator.evaluate(myExpression, new MyEvaluationCallback(node, place), myStackFrame.getSourcePosition());
                    }
                }
            }
            else {
                node.setPresentation(ExecutionDebugIconGroup.nodeWatch(), EMPTY_PRESENTATION, false);
            }
        }

        private class MyEvaluationCallback extends XEvaluationCallbackBase implements Obsolescent {
            @Nonnull
            private final XValueNode myNode;
            @Nonnull
            private final XValuePlace myPlace;

            public MyEvaluationCallback(@Nonnull XValueNode node, @Nonnull XValuePlace place) {
                myNode = node;
                myPlace = place;
            }

            @Override
            public boolean isObsolete() {
                return myNode.isObsolete();
            }

            @Override
            public void evaluated(@Nonnull XValue result) {
                myValue = result;

                if (myNode instanceof WatchNodeImpl watchNode) {
                    watchNode.evaluated();
                }

                result.computePresentation(myNode, myPlace);
            }

            @Override
            public void errorOccurred(@Nonnull String errorMessage) {
                myNode.setPresentation(XDebuggerUIConstants.ERROR_MESSAGE_ICON, new XErrorValuePresentation(errorMessage), false);
            }
        }

        private static final XValuePresentation EMPTY_PRESENTATION = new XValuePresentation() {
            @Nonnull
            @Override
            public String getSeparator() {
                return "";
            }

            @Override
            public void renderValue(@Nonnull XValueTextRenderer renderer) {
            }
        };

        @Override
        @Nullable
        public String getEvaluationExpression() {
            return myValue != null ? myValue.getEvaluationExpression() : null;
        }

        @Override
        @Nonnull
        public AsyncResult<XExpression> calculateEvaluationExpression() {
            return AsyncResult.done(myExpression);
        }

        @Override
        @Nullable
        public XInstanceEvaluator getInstanceEvaluator() {
            return myValue != null ? myValue.getInstanceEvaluator() : null;
        }

        @Override
        @Nullable
        public XValueModifier getModifier() {
            return myValue != null ? myValue.getModifier() : null;
        }

        @Override
        public void computeSourcePosition(@Nonnull XNavigatable navigatable) {
            if (myValue != null) {
                myValue.computeSourcePosition(navigatable);
            }
        }

        @Override
        @Nonnull
        public consulo.util.lang.ThreeState computeInlineDebuggerData(@Nonnull XInlineDebuggerDataCallback callback) {
            return consulo.util.lang.ThreeState.NO;
        }

        @Override
        public boolean canNavigateToSource() {
            return myValue != null && myValue.canNavigateToSource();
        }

        @Override
        public boolean canNavigateToTypeSource() {
            return myValue != null && myValue.canNavigateToTypeSource();
        }

        @Override
        public void computeTypeSourcePosition(@Nonnull XNavigatable navigatable) {
            if (myValue != null) {
                myValue.computeTypeSourcePosition(navigatable);
            }
        }

        @Override
        @Nullable
        public XReferrersProvider getReferrersProvider() {
            return myValue != null ? myValue.getReferrersProvider() : null;
        }
    }
}