/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.xdebugger.impl;

import consulo.annotation.DeprecationInfo;
import consulo.codeEditor.Editor;
import consulo.execution.debug.AbstractDebuggerSession;
import consulo.execution.debug.XDebuggerManager;
import consulo.ide.impl.idea.xdebugger.impl.actions.DebuggerActionHandler;
import consulo.ide.impl.idea.xdebugger.impl.actions.DebuggerToggleActionHandler;
import consulo.ide.impl.idea.xdebugger.impl.actions.EditBreakpointActionHandler;
import consulo.ide.impl.idea.xdebugger.impl.actions.MarkObjectActionHandler;
import consulo.ide.impl.idea.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.AbstractValueHint;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler;
import consulo.ide.impl.idea.xdebugger.impl.evaluate.quick.common.ValueHintType;
import consulo.project.Project;
import consulo.ui.ex.action.AnActionEvent;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author nik
 */
@Deprecated
@DeprecationInfo("Remove it in future, just replace by calling XDebuggerSupport")
public abstract class DebuggerSupport {
  private static final XDebuggerSupport ourDefaultInstance = new XDebuggerSupport();
  private static final List<DebuggerSupport> ourDefaultValues = List.of(ourDefaultInstance);

  protected static final class DisabledActionHandler extends DebuggerActionHandler {
    public static final DisabledActionHandler INSTANCE = new DisabledActionHandler();

    @Override
    public void perform(@Nonnull Project project, AnActionEvent event) {
    }

    @Override
    public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
      return false;
    }
  }

  @Nonnull
  public static List<DebuggerSupport> getDebuggerSupports() {
    return ourDefaultValues;
  }

  @Nonnull
  public static <T extends DebuggerSupport> DebuggerSupport getDebuggerSupport(Class<T> aClass) {
    return ourDefaultInstance;
  }

  @Nonnull
  public abstract BreakpointPanelProvider<?> getBreakpointPanelProvider();

  @Nonnull
  public DebuggerActionHandler getStepOverHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getStepIntoHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getSmartStepIntoHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getStepOutHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getForceStepOverHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getForceStepIntoHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getRunToCursorHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getForceRunToCursorHandler() {
    return DisabledActionHandler.INSTANCE;
  }


  @Nonnull
  public DebuggerActionHandler getResumeActionHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getPauseHandler() {
    return DisabledActionHandler.INSTANCE;
  }


  @Nonnull
  public DebuggerActionHandler getToggleLineBreakpointHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getToggleTemporaryLineBreakpointHandler() {
    return DisabledActionHandler.INSTANCE;
  }


  @Nonnull
  public DebuggerActionHandler getShowExecutionPointHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public DebuggerActionHandler getEvaluateHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  @Nonnull
  public QuickEvaluateHandler getQuickEvaluateHandler() {
    return DISABLED_QUICK_EVALUATE;
  }

  private static final QuickEvaluateHandler DISABLED_QUICK_EVALUATE = new QuickEvaluateHandler() {
    @Override
    public boolean isEnabled(@Nonnull Project project) {
      return false;
    }

    @Nullable
    @Override
    public AbstractValueHint createValueHint(@Nonnull Project project, @Nonnull Editor editor, @Nonnull Point point, ValueHintType type) {
      return null;
    }

    @Override
    public boolean canShowHint(@Nonnull Project project) {
      return false;
    }

    @Override
    public int getValueLookupDelay(Project project) {
      return 0;
    }
  };

  @Nonnull
  public DebuggerActionHandler getAddToWatchesActionHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  public DebuggerActionHandler getEvaluateInConsoleActionHandler() {
    return DisabledActionHandler.INSTANCE;
  }

  protected static final DebuggerToggleActionHandler DISABLED_TOGGLE_HANDLER = new DebuggerToggleActionHandler() {
    @Override
    public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
      return false;
    }

    @Override
    public boolean isSelected(@Nonnull Project project, AnActionEvent event) {
      return false;
    }

    @Override
    public void setSelected(@Nonnull Project project, AnActionEvent event, boolean state) {
    }
  };

  @Nonnull
  public DebuggerToggleActionHandler getMuteBreakpointsHandler() {
    return DISABLED_TOGGLE_HANDLER;
  }

  protected static final MarkObjectActionHandler DISABLED_MARK_HANDLER = new MarkObjectActionHandler() {
    @Override
    public boolean isMarked(@Nonnull Project project, @Nonnull AnActionEvent event) {
      return false;
    }

    @Override
    public void perform(@Nonnull Project project, AnActionEvent event) {
    }

    @Override
    public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
      return false;
    }
  };

  @Nonnull
  public MarkObjectActionHandler getMarkObjectHandler() {
    return DISABLED_MARK_HANDLER;
  }

  /**
   * @deprecated {@link XDebuggerManager#getCurrentSession()} is used instead
   */
  @Nullable
  @Deprecated
  public AbstractDebuggerSession getCurrentSession(@Nonnull Project project) {
    return null;
  }

  protected static final EditBreakpointActionHandler DISABLED_EDIT = new EditBreakpointActionHandler() {
    @Override
    protected void doShowPopup(Project project, JComponent component, Point whereToShow, Object breakpoint) {
    }

    @Override
    public boolean isEnabled(@Nonnull Project project, AnActionEvent event) {
      return false;
    }
  };

  @Nonnull
  public EditBreakpointActionHandler getEditBreakpointAction() {
    return DISABLED_EDIT;
  }
}
