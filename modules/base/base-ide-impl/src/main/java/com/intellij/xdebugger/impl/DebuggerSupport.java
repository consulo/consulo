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
package com.intellij.xdebugger.impl;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.actions.DebuggerActionHandler;
import com.intellij.xdebugger.impl.actions.DebuggerToggleActionHandler;
import com.intellij.xdebugger.impl.actions.EditBreakpointActionHandler;
import com.intellij.xdebugger.impl.actions.MarkObjectActionHandler;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import com.intellij.xdebugger.impl.evaluate.quick.common.AbstractValueHint;
import com.intellij.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler;
import com.intellij.xdebugger.impl.evaluate.quick.common.ValueHintType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * @author nik
 */
@Deprecated
public abstract class DebuggerSupport {
  private static final ExtensionPointName<DebuggerSupport> EXTENSION_POINT = ExtensionPointName.create("com.intellij.xdebugger.debuggerSupport");

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
    return EXTENSION_POINT.getExtensionList();
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


  @Nonnull
  public static <T extends DebuggerSupport> DebuggerSupport getDebuggerSupport(Class<T> aClass) {
    for (DebuggerSupport support : getDebuggerSupports()) {
      if (support.getClass() == aClass) {
        return support;
      }
    }
    throw new IllegalStateException();
  }
}
