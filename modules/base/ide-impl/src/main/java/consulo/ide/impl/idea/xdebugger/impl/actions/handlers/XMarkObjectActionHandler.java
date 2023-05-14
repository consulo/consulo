/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl.actions.handlers;

import consulo.ui.ex.action.AnActionEvent;
import consulo.project.Project;
import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.execution.debug.frame.XValue;
import consulo.ide.impl.idea.xdebugger.impl.XDebugSessionImpl;
import consulo.ide.impl.idea.xdebugger.impl.actions.MarkObjectActionHandler;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.ValueMarkerPresentationDialog;
import consulo.execution.debug.ui.ValueMarkup;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.actions.XDebuggerTreeActionBase;
import consulo.ide.impl.idea.xdebugger.impl.ui.tree.nodes.XValueNodeImpl;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public class XMarkObjectActionHandler extends MarkObjectActionHandler {
  @Override
  public void perform(@Nonnull Project project, AnActionEvent event) {
    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    if (session == null) return;

    XValueMarkers<?, ?> markers = ((XDebugSessionImpl)session).getValueMarkers();
    XValueNodeImpl node = XDebuggerTreeActionBase.getSelectedNode(event.getDataContext());
    if (markers == null || node == null) return;
    XValue value = node.getValueContainer();

    ValueMarkup existing = markers.getMarkup(value);
    if (existing != null) {
      markers.unmarkValue(value);
    }
    else {
      ValueMarkerPresentationDialog dialog = new ValueMarkerPresentationDialog(node.getName());
      dialog.show();
      ValueMarkup markup = dialog.getConfiguredMarkup();
      if (dialog.isOK() && markup != null) {
        markers.markValue(value, markup);
      }
    }
    session.rebuildViews();
  }

  @Override
  public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
    XValueMarkers<?, ?> markers = getValueMarkers(project);
    if (markers == null) return false;

    XValue value = XDebuggerTreeActionBase.getSelectedValue(event.getDataContext());
    return value != null && markers.canMarkValue(value);
  }

  @Override
  public boolean isMarked(@Nonnull Project project, @Nonnull AnActionEvent event) {
    XValueMarkers<?, ?> markers = getValueMarkers(project);
    if (markers == null) return false;

    XValue value = XDebuggerTreeActionBase.getSelectedValue(event.getDataContext());
    return value != null && markers.getMarkup(value) != null;
  }

  @Override
  public boolean isHidden(@Nonnull Project project, AnActionEvent event) {
    return getValueMarkers(project) == null;
  }

  @Nullable
  private static XValueMarkers<?, ?> getValueMarkers(@Nonnull Project project) {
    XDebugSession session = XDebuggerManager.getInstance(project).getCurrentSession();
    return session != null ? ((XDebugSessionImpl)session).getValueMarkers() : null;
  }
}
