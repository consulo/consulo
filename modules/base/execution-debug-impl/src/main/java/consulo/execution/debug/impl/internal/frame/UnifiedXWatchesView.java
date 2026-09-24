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
package consulo.execution.debug.impl.internal.frame;

import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.icon.ExecutionDebugIconGroup;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.ui.XDebugSessionTab;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedWatchNodeImpl;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedWatchesRootNode;
import consulo.execution.debug.impl.internal.ui.tree.node.UnifiedXValueContainerNode;
import consulo.execution.debug.impl.internal.ui.tree.node.WatchNode;
import consulo.execution.debug.impl.internal.ui.tree.node.XDebuggerTreeNode;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.details.KeyCode;
import consulo.ui.event.details.KeyboardInputDetails;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The variables with the watches before them, built of unified components - the counterpart of the swing
 * {@link XWatchesViewImpl}. The field above the tree evaluates an expression in the frame, or adds it to the watches.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXWatchesView extends UnifiedXVariablesView implements XWatchesView {
    private UnifiedWatchesRootNode myRootNode;

    private final boolean myWatchesInVariables;
    // made by createTopPanel, which the constructor of the super class calls
    private @Nullable TextBox myEvaluateField;
    private @Nullable Button myRemoveWatchButton;

    @RequiredUIAccess
    public UnifiedXWatchesView(XDebugSessionImpl session, boolean watchesInVariables) {
        super(session);
        myWatchesInVariables = watchesInVariables;

        createNewRootNode(null);

        // a key listener would keep the keymap off the tree - stepping with the keys has to work from it - so the
        // watch is removed with the button of the field
        getTree().addSelectionListener(this::updateRemoveWatchButton);
    }

    @Override
    @RequiredUIAccess
    protected @Nullable Component createTopPanel() {
        XDebuggerEditorsProvider provider = mySession.getDebugProcess().getEditorsProvider();
        if (!provider.isEvaluateExpressionFieldEnabled()) {
            return null;
        }

        TextBox evaluateField = TextBox.create();
        evaluateField.withPlaceholder(XDebuggerLocalize.debuggerEvaluateExpressionOrAddAWatchHint(
            "Enter",
            Platform.current().os().isMac() ? "Meta+Shift+Enter" : "Ctrl+Shift+Enter"
        ));
        evaluateField.addKeyPressedListener(event -> {
            if (!(event.getInputDetails() instanceof KeyboardInputDetails details) || details.getKeyCode() != KeyCode.ENTER) {
                return;
            }

            if ((details.withCtrl() || details.withMeta()) && details.withShift()) {
                XExpression expression = getEvaluateExpression();
                if (expression != null) {
                    addWatchExpression(expression, -1, false);
                }
            }
            else {
                addExpressionResultNode();
            }
        });

        Button removeWatchButton = Button.create(LocalizeValue.empty(), event -> removeSelectedWatches());
        removeWatchButton.setIcon(PlatformIconGroup.actionsCancel());
        removeWatchButton.addStyle(ButtonStyle.TOOLBAR);
        removeWatchButton.setToolTipText(XDebuggerLocalize.actionRemoveWatchText());
        removeWatchButton.setVisible(false);

        Button addToWatchesButton = Button.create(LocalizeValue.empty(), event -> {
            XExpression expression = getEvaluateExpression();
            if (expression != null) {
                addWatchExpression(expression, -1, false);
            }
        });
        addToWatchesButton.setIcon(ExecutionDebugIconGroup.actionAddtowatch());
        addToWatchesButton.addStyle(ButtonStyle.TOOLBAR);
        addToWatchesButton.setToolTipText(XDebuggerLocalize.actionAddToWatchText());

        HorizontalLayout actions = HorizontalLayout.create(Space.NONE);
        actions.add(removeWatchButton);
        actions.add(addToWatchesButton);
        evaluateField.setSuffixComponent(actions);

        myEvaluateField = evaluateField;
        myRemoveWatchButton = removeWatchButton;

        // the field is as wide as the view, the way the swing editor is
        DockLayout component = DockLayout.create();
        component.center(evaluateField);
        return component;
    }

    private @Nullable XExpression getEvaluateExpression() {
        TextBox evaluateField = myEvaluateField;
        String text = evaluateField == null ? null : evaluateField.getValue();
        if (StringUtil.isEmptyOrSpaces(text)) {
            return null;
        }
        return XExpression.fromText(text.trim());
    }

    @RequiredUIAccess
    private void addExpressionResultNode() {
        XExpression expression = getEvaluateExpression();
        if (!XDebuggerUtil.getInstance().isEmptyExpression(expression)) {
            myRootNode.addResultNode(mySession.getCurrentStackFrame(), expression);
        }
    }

    @RequiredUIAccess
    private void updateRemoveWatchButton() {
        Button removeWatchButton = myRemoveWatchButton;
        if (removeWatchButton != null) {
            removeWatchButton.setVisible(!getSelectedWatches().isEmpty());
        }
    }

    private List<UnifiedWatchNodeImpl> getSelectedWatches() {
        List<UnifiedWatchNodeImpl> watches = new ArrayList<>();
        List<UnifiedWatchNodeImpl> children = myRootNode.getWatchChildren();
        for (UnifiedWatchNodeImpl node : getTree().getSelectedNodes(UnifiedWatchNodeImpl.class)) {
            if (children.contains(node)) {
                watches.add(node);
            }
        }
        return watches;
    }

    @RequiredUIAccess
    private void removeSelectedWatches() {
        List<UnifiedWatchNodeImpl> watches = getSelectedWatches();
        if (!watches.isEmpty()) {
            myRootNode.removeChildren(watches);
            updateSessionData();
        }
        updateRemoveWatchButton();
    }

    @Override
    @RequiredUIAccess
    protected void buildTreeAndRestoreState(XStackFrame stackFrame) {
        super.buildTreeAndRestoreState(stackFrame);
        updateRemoveWatchButton();
    }

    @Override
    @RequiredUIAccess
    protected UnifiedXValueContainerNode<?> createNewRootNode(@Nullable XStackFrame stackFrame) {
        UnifiedWatchesRootNode node = new UnifiedWatchesRootNode(getTree(), this, getExpressions(), stackFrame, myWatchesInVariables);
        myRootNode = node;
        getTree().setRoot(node);
        return node;
    }

    @Override
    @RequiredUIAccess
    protected void addEmptyMessage(UnifiedXValueContainerNode<?> root) {
        if (myWatchesInVariables) {
            super.addEmptyMessage(root);
        }
    }

    private XExpression[] getExpressions() {
        return mySession.getSessionData().getWatchExpressions();
    }

    @Override
    protected void beforeTreeBuild(SessionEvent event) {
        if (event != SessionEvent.SETTINGS_CHANGED) {
            myRootNode.removeResultNode();
        }
    }

    /**
     * The nodes of the swing tree are asked for by their expressions - the actions which pass them work on either tree.
     */
    @Override
    @RequiredUIAccess
    public void removeWatches(List<? extends XDebuggerTreeNode> nodes) {
        Set<XExpression> expressions = new HashSet<>();
        for (XDebuggerTreeNode node : nodes) {
            if (node instanceof WatchNode watchNode) {
                expressions.add(watchNode.getExpression());
            }
        }

        List<UnifiedWatchNodeImpl> toRemove = new ArrayList<>();
        for (UnifiedWatchNodeImpl child : myRootNode.getWatchChildren()) {
            if (expressions.contains(child.getExpression())) {
                toRemove.add(child);
            }
        }
        myRootNode.removeChildren(toRemove);
        updateSessionData();
    }

    @Override
    @RequiredUIAccess
    public void removeAllWatches() {
        myRootNode.removeAllChildren();
        updateSessionData();
    }

    @Override
    @RequiredUIAccess
    public void addWatchExpression(XExpression expression, int index, boolean navigateToWatchNode) {
        myRootNode.addWatchExpression(mySession.getCurrentStackFrame(), expression, index, navigateToWatchNode);
        updateSessionData();
        if (navigateToWatchNode) {
            XDebugSessionTab.showWatchesView(mySession);
        }
    }

    @Override
    public void computeWatches() {
        myRootNode.computeWatches();
    }

    @Override
    public void updateSessionData() {
        List<XExpression> watchExpressions = myRootNode.getWatchExpressions();
        mySession.setWatchExpressions(watchExpressions.toArray(new XExpression[watchExpressions.size()]));
    }
}
