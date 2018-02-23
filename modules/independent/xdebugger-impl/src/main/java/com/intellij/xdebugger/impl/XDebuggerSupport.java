/*
 * Copyright 2000-2014 JetBrains s.r.o.
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

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.project.Project;
import com.intellij.xdebugger.AbstractDebuggerSession;
import com.intellij.xdebugger.XDebugSession;
import com.intellij.xdebugger.XDebuggerManager;
import com.intellij.xdebugger.impl.actions.*;
import com.intellij.xdebugger.impl.actions.handlers.*;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointPanelProvider;
import com.intellij.xdebugger.impl.breakpoints.ui.BreakpointPanelProvider;
import com.intellij.xdebugger.impl.evaluate.quick.XQuickEvaluateHandler;
import com.intellij.xdebugger.impl.evaluate.quick.common.QuickEvaluateHandler;
import javax.annotation.Nonnull;

/**
 * @author nik
 */
public class XDebuggerSupport extends DebuggerSupport {
  private final XBreakpointPanelProvider myBreakpointPanelProvider;
  private final XToggleLineBreakpointActionHandler myToggleLineBreakpointActionHandler;
  private final XToggleLineBreakpointActionHandler myToggleTemporaryLineBreakpointActionHandler;
  private final XDebuggerSuspendedActionHandler myStepOverHandler;
  private final XDebuggerSuspendedActionHandler myStepIntoHandler;
  private final XDebuggerSuspendedActionHandler myStepOutHandler;
  private final XDebuggerSuspendedActionHandler myForceStepOverHandler;
  private final XDebuggerSuspendedActionHandler myForceStepIntoHandler;
  private final XDebuggerRunToCursorActionHandler myRunToCursorHandler;
  private final XDebuggerRunToCursorActionHandler myForceRunToCursor;
  private final XDebuggerActionHandler myResumeHandler;
  private final XDebuggerPauseActionHandler myPauseHandler;
  private final XDebuggerSuspendedActionHandler myShowExecutionPointHandler;
  private final XDebuggerEvaluateActionHandler myEvaluateHandler;
  private final XQuickEvaluateHandler myQuickEvaluateHandler;

  private final XAddToWatchesFromEditorActionHandler myAddToWatchesActionHandler;
  private final DebuggerActionHandler myEvaluateInConsoleActionHandler = new XEvaluateInConsoleFromEditorActionHandler();

  private final DebuggerToggleActionHandler myMuteBreakpointsHandler;
  private final DebuggerActionHandler mySmartStepIntoHandler;
  private final XMarkObjectActionHandler myMarkObjectActionHandler;
  private final EditBreakpointActionHandler myEditBreakpointActionHandler;

  public XDebuggerSupport() {
    myBreakpointPanelProvider = new XBreakpointPanelProvider();
    myToggleLineBreakpointActionHandler = new XToggleLineBreakpointActionHandler(false);
    myToggleTemporaryLineBreakpointActionHandler = new XToggleLineBreakpointActionHandler(true);
    myAddToWatchesActionHandler = new XAddToWatchesFromEditorActionHandler();
    myStepOverHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.stepOver(false);
      }
    };
    myStepIntoHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.stepInto();
      }
    };
    myStepOutHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.stepOut();
      }
    };
    myForceStepOverHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.stepOver(true);
      }
    };
    myForceStepIntoHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.forceStepInto();
      }
    };
    mySmartStepIntoHandler = new XDebuggerSmartStepIntoHandler();
    myRunToCursorHandler = new XDebuggerRunToCursorActionHandler(false);
    myForceRunToCursor = new XDebuggerRunToCursorActionHandler(true);
    myResumeHandler = new XDebuggerActionHandler() {
      @Override
      protected boolean isEnabled(@Nonnull final XDebugSession session, final DataContext dataContext) {
        return session.isPaused();
      }

      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.resume();
      }
    };
    myPauseHandler = new XDebuggerPauseActionHandler();
    myShowExecutionPointHandler = new XDebuggerSuspendedActionHandler() {
      @Override
      protected void perform(@Nonnull final XDebugSession session, final DataContext dataContext) {
        session.showExecutionPoint();
      }
    };
    myMuteBreakpointsHandler = new XDebuggerMuteBreakpointsHandler();
    myEvaluateHandler = new XDebuggerEvaluateActionHandler();
    myQuickEvaluateHandler = new XQuickEvaluateHandler();
    myMarkObjectActionHandler = new XMarkObjectActionHandler();
    myEditBreakpointActionHandler = new XDebuggerEditBreakpointActionHandler();
  }

  @Override
  @Nonnull
  public BreakpointPanelProvider<?> getBreakpointPanelProvider() {
    return myBreakpointPanelProvider;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getStepOverHandler() {
    return myStepOverHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getStepIntoHandler() {
    return myStepIntoHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getSmartStepIntoHandler() {
    return mySmartStepIntoHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getStepOutHandler() {
    return myStepOutHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getForceStepOverHandler() {
    return myForceStepOverHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getForceStepIntoHandler() {
    return myForceStepIntoHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getRunToCursorHandler() {
    return myRunToCursorHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getForceRunToCursorHandler() {
    return myForceRunToCursor;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getResumeActionHandler() {
    return myResumeHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getPauseHandler() {
    return myPauseHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getToggleLineBreakpointHandler() {
    return myToggleLineBreakpointActionHandler;
  }

  @Nonnull
  @Override
  public DebuggerActionHandler getToggleTemporaryLineBreakpointHandler() {
    return myToggleTemporaryLineBreakpointActionHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getShowExecutionPointHandler() {
    return myShowExecutionPointHandler;
  }

  @Override
  @Nonnull
  public DebuggerActionHandler getEvaluateHandler() {
    return myEvaluateHandler;
  }

  @Override
  @Nonnull
  public QuickEvaluateHandler getQuickEvaluateHandler() {
    return myQuickEvaluateHandler;
  }

  @Nonnull
  @Override
  public DebuggerActionHandler getAddToWatchesActionHandler() {
    return myAddToWatchesActionHandler;
  }

  @Nonnull
  @Override
  public DebuggerActionHandler getEvaluateInConsoleActionHandler() {
    return myEvaluateInConsoleActionHandler;
  }

  @Override
  @Nonnull
  public DebuggerToggleActionHandler getMuteBreakpointsHandler() {
    return myMuteBreakpointsHandler;
  }

  @Nonnull
  @Override
  public MarkObjectActionHandler getMarkObjectHandler() {
    return myMarkObjectActionHandler;
  }

  @Override
  public AbstractDebuggerSession getCurrentSession(@Nonnull Project project) {
    return XDebuggerManager.getInstance(project).getCurrentSession();
  }

  @Nonnull
  @Override
  public EditBreakpointActionHandler getEditBreakpointAction() {
    return myEditBreakpointActionHandler;
  }
}
