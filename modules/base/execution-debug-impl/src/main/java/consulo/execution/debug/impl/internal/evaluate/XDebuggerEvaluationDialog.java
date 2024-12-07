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
package consulo.execution.debug.impl.internal.evaluate;

import consulo.application.ApplicationManager;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataProvider;
import consulo.execution.debug.*;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.event.XDebugSessionListener;
import consulo.execution.debug.impl.internal.XDebugSessionImpl;
import consulo.execution.debug.impl.internal.setting.XDebuggerSettingManagerImpl;
import consulo.execution.debug.impl.internal.ui.XDebugSessionTab;
import consulo.execution.debug.impl.internal.ui.XDebuggerEditorBase;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTree;
import consulo.execution.debug.impl.internal.ui.tree.XDebuggerTreePanel;
import consulo.execution.debug.impl.internal.ui.tree.node.EvaluatingExpressionRootNode;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.CustomShortcutSet;
import consulo.ui.ex.awt.BorderLayoutPanel;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;

/**
 * @author nik
 */
public class XDebuggerEvaluationDialog extends DialogWrapper {
    public static final Key<XDebuggerEvaluationDialog> KEY = Key.create("DEBUGGER_EVALUATION_DIALOG");

    //can not use new SHIFT_DOWN_MASK etc because in this case ActionEvent modifiers do not match
    private static final int ADD_WATCH_MODIFIERS = (Platform.current().os().isMac() ? InputEvent.META_MASK : InputEvent.CTRL_MASK) | InputEvent.SHIFT_MASK;
    static KeyStroke ADD_WATCH_KEYSTROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, ADD_WATCH_MODIFIERS);

    private final JPanel myMainPanel;
    private final JPanel myResultPanel;
    private final XDebuggerTreePanel myTreePanel;
    private EvaluationInputComponent myInputComponent;
    private final XDebugSession mySession;
    private final XDebuggerEditorsProvider myEditorsProvider;
    private EvaluationMode myMode;
    private XSourcePosition mySourcePosition;
    private final SwitchModeAction mySwitchModeAction;
    private final boolean myIsCodeFragmentEvaluationSupported;

    public XDebuggerEvaluationDialog(@Nonnull XDebugSession session,
                                     @Nonnull XDebuggerEditorsProvider editorsProvider,
                                     @Nonnull XDebuggerEvaluator evaluator,
                                     @Nonnull XExpression text,
                                     @Nullable XSourcePosition sourcePosition) {
        super(TargetAWT.to(WindowManager.getInstance().getWindow(session.getProject())), true);
        mySession = session;
        myEditorsProvider = editorsProvider;
        mySourcePosition = sourcePosition;
        setModal(false);
        setOKButtonText(XDebuggerBundle.message("xdebugger.button.evaluate"));
        setCancelButtonText(XDebuggerBundle.message("xdebugger.evaluate.dialog.close"));

        mySession.addSessionListener(new XDebugSessionListener() {
            @Override
            public void sessionStopped() {
                ApplicationManager.getApplication().invokeLater(() -> close(CANCEL_EXIT_CODE));
            }

            @Override
            public void stackFrameChanged() {
                updateSourcePosition();
            }

            @Override
            public void sessionPaused() {
                updateSourcePosition();
            }
        }, myDisposable);

        myTreePanel = new XDebuggerTreePanel(session.getProject(), editorsProvider, myDisposable, sourcePosition, XDebuggerActions.EVALUATE_DIALOG_TREE_POPUP_GROUP,
            ((XDebugSessionImpl) session).getValueMarkers());
        myResultPanel = JBUI.Panels.simplePanel()
            .addToTop(new JLabel(XDebuggerBundle.message("xdebugger.evaluate.label.result")))
            .addToCenter(myTreePanel.getMainPanel());
        myMainPanel = new EvaluationMainPanel();

        mySwitchModeAction = new SwitchModeAction();

        new AnAction() {
            @RequiredUIAccess
            @Override
            public void update(AnActionEvent e) {
                Project project = e.getData(Project.KEY);
                e.getPresentation().setEnabled(project != null && LookupManager.getInstance(project).getActiveLookup() == null);
            }

            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                //doOKAction(); // do not evaluate on add to watches
                addToWatches();
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(ADD_WATCH_KEYSTROKE), getRootPane(), myDisposable);

        new AnAction() {
            @RequiredUIAccess
            @Override
            public void actionPerformed(AnActionEvent e) {
                ApplicationIdeFocusManager focusManager = ApplicationIdeFocusManager.getInstance();

                focusManager.getInstanceForProject(mySession.getProject()).requestFocus(myTreePanel.getTree(), true);
            }
        }.registerCustomShortcutSet(new CustomShortcutSet(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK)), getRootPane(),
            myDisposable);

        Condition<TreeNode> rootFilter = node -> node.getParent() instanceof EvaluatingExpressionRootNode;
        myTreePanel.getTree().expandNodesOnLoad(rootFilter);
        myTreePanel.getTree().selectNodeOnLoad(rootFilter);

        EvaluationMode mode = XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().getEvaluationDialogMode();
        myIsCodeFragmentEvaluationSupported = evaluator.isCodeFragmentEvaluationSupported();
        if (mode == EvaluationMode.CODE_FRAGMENT && !myIsCodeFragmentEvaluationSupported) {
            mode = EvaluationMode.EXPRESSION;
        }
        if (mode == EvaluationMode.EXPRESSION && text.getMode() == EvaluationMode.CODE_FRAGMENT && myIsCodeFragmentEvaluationSupported) {
            mode = EvaluationMode.CODE_FRAGMENT;
        }
        switchToMode(mode, text);
        init();
    }

    @Override
    protected void dispose() {
        super.dispose();
        myMainPanel.removeAll();
    }

    private void updateSourcePosition() {
        ApplicationManager.getApplication().invokeLater(() -> {
            mySourcePosition = mySession.getCurrentPosition();
            getInputEditor().setSourcePosition(mySourcePosition);
        });
    }

    @Override
    protected void doOKAction() {
        evaluate();
    }

    @Override
    protected void createDefaultActions() {
        super.createDefaultActions();
        myOKAction = new OkAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                super.actionPerformed(e);
                if ((e.getModifiers() & ADD_WATCH_MODIFIERS) == ADD_WATCH_MODIFIERS) {
                    addToWatches();
                }
            }
        };
    }

    private void addToWatches() {
        if (myMode == EvaluationMode.EXPRESSION) {
            XExpression expression = getInputEditor().getExpression();
            if (!XDebuggerUtil.getInstance().isEmptyExpression(expression)) {
                XDebugSessionTab tab = ((XDebugSessionImpl) mySession).getSessionTab();
                if (tab != null) {
                    tab.getWatchesView().addWatchExpression(expression, -1, true);
                    getInputEditor().requestFocusInEditor();
                }
            }
        }
    }

    @Nonnull
    @Override
    protected Action[] createActions() {
        if (myIsCodeFragmentEvaluationSupported) {
            return new Action[]{getOKAction(), mySwitchModeAction, getCancelAction()};
        }
        return super.createActions();
    }

    @Override
    protected String getHelpId() {
        return "debugging.debugMenu.evaluate";
    }

    @Override
    protected JButton createJButtonForAction(Action action) {
        final JButton button = super.createJButtonForAction(action);
        if (action == mySwitchModeAction) {
            int width1 = new JButton(getSwitchButtonText(EvaluationMode.EXPRESSION)).getPreferredSize().width;
            int width2 = new JButton(getSwitchButtonText(EvaluationMode.CODE_FRAGMENT)).getPreferredSize().width;
            final Dimension size = new Dimension(Math.max(width1, width2), button.getPreferredSize().height);
            button.setMinimumSize(size);
            button.setPreferredSize(size);
        }
        return button;
    }

    public XExpression getExpression() {
        return getInputEditor().getExpression();
    }

    private static String getSwitchButtonText(EvaluationMode mode) {
        return mode != EvaluationMode.EXPRESSION
            ? XDebuggerBundle.message("button.text.expression.mode")
            : XDebuggerBundle.message("button.text.code.fragment.mode");
    }

    private void switchToMode(EvaluationMode mode, XExpression text) {
        if (myMode == mode) {
            return;
        }

        myMode = mode;

        if (mode == EvaluationMode.EXPRESSION) {
            text = new XExpressionImpl(StringUtil.convertLineSeparators(text.getExpression(), " "), text.getLanguage(), text.getCustomInfo());
        }

        myInputComponent = createInputComponent(mode, text);
        myMainPanel.removeAll();
        myInputComponent.addComponent(myMainPanel, myResultPanel);

        setTitle(myInputComponent.getTitle());
        mySwitchModeAction.putValue(Action.NAME, getSwitchButtonText(mode));
        getInputEditor().requestFocusInEditor();
    }

    private XDebuggerEditorBase getInputEditor() {
        return myInputComponent.getInputEditor();
    }

    private EvaluationInputComponent createInputComponent(EvaluationMode mode, XExpression text) {
        final Project project = mySession.getProject();
        text = XExpression.changeMode(text, mode);
        if (mode == EvaluationMode.EXPRESSION) {
            return new ExpressionInputComponent(project, myEditorsProvider, mySourcePosition, text, myDisposable);
        }
        else {
            return new CodeFragmentInputComponent(project, myEditorsProvider, mySourcePosition, text,
                getDimensionServiceKey() + ".splitter", myDisposable);
        }
    }

    private void evaluate() {
        final XDebuggerEditorBase inputEditor = getInputEditor();
        int offset = -1;

        //try to save caret position
        Editor editor = inputEditor.getEditor();
        if (editor != null) {
            offset = editor.getCaretModel().getOffset();
        }

        final XDebuggerTree tree = myTreePanel.getTree();
        tree.markNodesObsolete();
        tree.setRoot(new EvaluatingExpressionRootNode(this, tree), false);

        myResultPanel.invalidate();

        //editor is already changed
        editor = inputEditor.getEditor();
        //selectAll puts focus back
        inputEditor.selectAll();

        //try to restore caret position and clear selection
        if (offset >= 0 && editor != null) {
            offset = Math.min(editor.getDocument().getTextLength(), offset);
            editor.getCaretModel().moveToOffset(offset);
            editor.getSelectionModel().setSelection(offset, offset);
        }
    }

    @Override
    public void doCancelAction() {
        getInputEditor().saveTextInHistory();
        super.doCancelAction();
    }

    @Override
    protected String getDimensionServiceKey() {
        return "#xdebugger.evaluate";
    }

    @Override
    protected JComponent createCenterPanel() {
        return myMainPanel;
    }

    public void startEvaluation(@Nonnull XDebuggerEvaluator.XEvaluationCallback evaluationCallback) {
        final XDebuggerEditorBase inputEditor = getInputEditor();
        inputEditor.saveTextInHistory();
        XExpression expression = inputEditor.getExpression();

        XDebuggerEvaluator evaluator = mySession.getDebugProcess().getEvaluator();
        if (evaluator == null) {
            evaluationCallback.errorOccurred(XDebuggerBundle.message("xdebugger.evaluate.stack.frame.has.not.evaluator"));
        }
        else {
            evaluator.evaluate(expression, evaluationCallback, null);
        }
    }

    public void evaluationDone() {
        mySession.rebuildViews();
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return getInputEditor().getPreferredFocusedComponent();
    }

    private class SwitchModeAction extends AbstractAction {
        @Override
        public void actionPerformed(ActionEvent e) {
            XExpression text = getInputEditor().getExpression();
            EvaluationMode newMode = (myMode == EvaluationMode.EXPRESSION) ? EvaluationMode.CODE_FRAGMENT : EvaluationMode.EXPRESSION;
            // remember only on user selection
            XDebuggerSettingManagerImpl.getInstanceImpl().getGeneralSettings().setEvaluationDialogMode(newMode);
            switchToMode(newMode, text);
        }
    }

    private class EvaluationMainPanel extends BorderLayoutPanel implements DataProvider {
        @Nullable
        @Override
        public Object getData(@Nonnull @NonNls Key<?> dataId) {
            if (KEY == dataId) {
                return XDebuggerEvaluationDialog.this;
            }
            return null;
        }
    }
}
