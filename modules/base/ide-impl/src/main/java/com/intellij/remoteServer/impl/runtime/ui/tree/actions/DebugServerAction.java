package com.intellij.remoteServer.impl.runtime.ui.tree.actions;

import consulo.execution.Executor;
import com.intellij.execution.executors.DefaultDebugExecutor;
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
