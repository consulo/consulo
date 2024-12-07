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
package consulo.execution.debug.impl.internal.ui.tree;

import consulo.execution.debug.XDebuggerBundle;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.evaluation.XDebuggerEditorsProvider;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueMarkers;
import consulo.execution.debug.impl.internal.evaluate.DebuggerTreeWithHistoryPanel;
import consulo.execution.debug.impl.internal.evaluate.XDebuggerTreeCreator;
import consulo.project.Project;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;

/**
 * @author nik
 */
public class XInspectDialog extends DialogWrapper {
  private final DebuggerTreeWithHistoryPanel<Pair<XValue, String>> myDebuggerTreePanel;

  public XInspectDialog(@Nonnull Project project,
                        XDebuggerEditorsProvider editorsProvider,
                        XSourcePosition sourcePosition,
                        @Nonnull String name,
                        @Nonnull XValue value,
                        XValueMarkers<?, ?> markers) {
    super(project, false);

    setTitle(XDebuggerBundle.message("inspect.value.dialog.title", name));
    setModal(false);

    Pair<XValue, String> initialItem = Pair.create(value, name);
    XDebuggerTreeCreator creator = new XDebuggerTreeCreator(project, editorsProvider, sourcePosition, markers);
    myDebuggerTreePanel = new DebuggerTreeWithHistoryPanel<Pair<XValue, String>>(initialItem, creator, project);
    init();
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    return myDebuggerTreePanel.getMainPanel();
  }

  @Override
  @Nullable
  protected JComponent createSouthPanel() {
    return null;
  }

  @Override
  @NonNls
  protected String getDimensionServiceKey() {
    return "#xdebugger.XInspectDialog";
  }
}