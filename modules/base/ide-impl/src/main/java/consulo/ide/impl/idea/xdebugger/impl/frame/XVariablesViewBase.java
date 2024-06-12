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
package consulo.ide.impl.idea.xdebugger.impl.frame;

import consulo.application.util.registry.Registry;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.codeEditor.event.SelectionEvent;
import consulo.codeEditor.event.SelectionListener;
import consulo.document.util.TextRange;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerActions;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.ExpressionInfo;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValueContainer;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.setting.XDebuggerSettingsManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.ide.impl.idea.util.containers.ObjectLongHashMap;
import consulo.ide.impl.idea.xdebugger.impl.XDebuggerInlayUtil;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.XValueHint;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.ValueHintType;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTree;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTreePanel;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTreeRestorer;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.XDebuggerTreeState;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XStackFrameNode;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueContainerNode;
import consulo.project.Project;
import consulo.ui.ex.awt.dnd.DnDManager;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;

/**
 * @author nik
 */
public abstract class XVariablesViewBase extends XDebugView {
  private final XDebuggerTreePanel myTreePanel;
  private XDebuggerTreeState myTreeState;
  private XDebuggerTreeRestorer myTreeRestorer;

  private Object myFrameEqualityObject;
  private MySelectionListener mySelectionListener;

  protected XVariablesViewBase(@Nonnull Project project, @Nonnull XDebuggerEditorsProvider editorsProvider, @Nullable XValueMarkers<?, ?> markers) {
    myTreePanel = new XDebuggerTreePanel(
      project,
      editorsProvider,
      this,
      null,
      this instanceof XWatchesView ? XDebuggerActions.WATCHES_TREE_POPUP_GROUP : XDebuggerActions.VARIABLES_TREE_POPUP_GROUP,
      markers
    );
    getTree().getEmptyText().setText(XDebuggerLocalize.debuggerVariablesNotAvailable().get());
    DnDManager.getInstance().registerSource(myTreePanel, getTree());
  }

  protected void buildTreeAndRestoreState(@Nonnull final XStackFrame stackFrame) {
    XSourcePosition position = stackFrame.getSourcePosition();
    XDebuggerTree tree = getTree();
    tree.setSourcePosition(position);
    createNewRootNode(stackFrame);
    final Project project = tree.getProject();
    project.putUserData(XVariablesView.DEBUG_VARIABLES, new XVariablesView.InlineVariablesInfo());
    project.putUserData(XVariablesView.DEBUG_VARIABLES_TIMESTAMPS, new ObjectLongHashMap<>());
    clearInlays(tree);
    Object newEqualityObject = stackFrame.getEqualityObject();
    if (myFrameEqualityObject != null && newEqualityObject != null && myFrameEqualityObject.equals(newEqualityObject) && myTreeState != null) {
      disposeTreeRestorer();
      myTreeRestorer = myTreeState.restoreState(tree);
    }
    if (position != null && XDebuggerSettingsManager.getInstance().getDataViewSettings().isValueTooltipAutoShowOnSelection()) {
      registerInlineEvaluator(stackFrame, position, project);
    }
  }

  protected static void clearInlays(XDebuggerTree tree) {
    if (Registry.is("debugger.show.values.inplace")) XDebuggerInlayUtil.clearInlays(tree.getProject());
  }

  protected XValueContainerNode createNewRootNode(@Nullable XStackFrame stackFrame) {
    XValueContainerNode root;
    if (stackFrame == null) {
      root = new XValueContainerNode<XValueContainer>(getTree(), null, new XValueContainer() {
      }) {
      };
    }
    else {
      root = new XStackFrameNode(getTree(), stackFrame);
    }
    getTree().setRoot(root, false);
    return root;
  }

  private void registerInlineEvaluator(final XStackFrame stackFrame, final XSourcePosition position, final Project project) {
    final VirtualFile file = position.getFile();
    final FileEditor fileEditor = FileEditorManager.getInstance(project).getSelectedEditor(file);
    if (fileEditor instanceof TextEditor) {
      final Editor editor = ((TextEditor)fileEditor).getEditor();
      removeSelectionListener();
      mySelectionListener = new MySelectionListener(editor, stackFrame, project);
      editor.getSelectionModel().addSelectionListener(mySelectionListener);
    }
  }

  protected void saveCurrentTreeState(@Nullable XStackFrame stackFrame) {
    removeSelectionListener();
    myFrameEqualityObject = stackFrame != null ? stackFrame.getEqualityObject() : null;
    if (myTreeRestorer == null || myTreeRestorer.isFinished()) {
      myTreeState = XDebuggerTreeState.saveState(getTree());
    }
    disposeTreeRestorer();
  }

  private void removeSelectionListener() {
    if (mySelectionListener != null) {
      mySelectionListener.remove();
      mySelectionListener = null;
    }
  }

  @Override
  protected void clear() {
    removeSelectionListener();
  }

  private void disposeTreeRestorer() {
    if (myTreeRestorer != null) {
      myTreeRestorer.dispose();
      myTreeRestorer = null;
    }
  }

  @Nonnull
  public final XDebuggerTree getTree() {
    return myTreePanel.getTree();
  }

  public JComponent getPanel() {
    return myTreePanel.getMainPanel();
  }

  @Override
  public void dispose() {
    disposeTreeRestorer();
    removeSelectionListener();
    DnDManager.getInstance().unregisterSource(myTreePanel, getTree());
  }

  private class MySelectionListener implements SelectionListener {
    private final Editor myEditor;
    private final XStackFrame myStackFrame;
    private final Project myProject;

    public MySelectionListener(Editor editor, XStackFrame stackFrame, Project project) {
      myEditor = editor;
      myStackFrame = stackFrame;
      myProject = project;
    }

    public void remove() {
      myEditor.getSelectionModel().removeSelectionListener(this);
    }

    @Override
    public void selectionChanged(final SelectionEvent e) {
      if (!XDebuggerSettingsManager.getInstance().getDataViewSettings().isValueTooltipAutoShowOnSelection() || myEditor.getCaretModel().getCaretCount() > 1) {
        return;
      }
      final String text = myEditor.getDocument().getText(e.getNewRange());
      if (!StringUtil.isEmpty(text) && !(text.contains("exec(") || text.contains("++") || text.contains("--") || text.contains("="))) {
        final XDebugSession session = getSession(getTree());
        if (session == null) return;
        XDebuggerEvaluator evaluator = myStackFrame.getEvaluator();
        if (evaluator == null) return;
        TextRange range = e.getNewRange();
        ExpressionInfo info = new ExpressionInfo(range);
        int offset = range.getStartOffset();
        LogicalPosition pos = myEditor.offsetToLogicalPosition(offset);
        Point point = myEditor.logicalPositionToXY(pos);
        new XValueHint(myProject, myEditor, point, ValueHintType.MOUSE_OVER_HINT, info, evaluator, session).invokeHint();
      }
    }
  }
}
