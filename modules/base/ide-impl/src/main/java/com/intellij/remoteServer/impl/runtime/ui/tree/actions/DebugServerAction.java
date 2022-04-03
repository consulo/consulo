package com.intellij.remoteServer.impl.runtime.ui.tree.actions;

import consulo.execution.executor.Executor;
import consulo.execution.debug.DefaultDebugExecutor;
import consulo.application.AllIcons;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public class DebugServerAction extends RunServerActionBase {
  public DebugServerAction() {
    super("Debug", "Start the selected server in debug mode", AllIcons.Actions.StartDebugger);
  }

  @Override
  protected Executor getExecutor() {
    return DefaultDebugExecutor.getDebugExecutorInstance();
  }
}
