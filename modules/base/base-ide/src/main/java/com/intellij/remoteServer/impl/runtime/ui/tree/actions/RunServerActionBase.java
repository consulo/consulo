package com.intellij.remoteServer.impl.runtime.ui.tree.actions;


import com.intellij.execution.Executor;
import com.intellij.remoteServer.impl.runtime.ui.tree.ServerNode;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;

/**
 * Created by IntelliJ IDEA.
 * User: michael.golubev
 */
public abstract class RunServerActionBase extends ServerActionBase {

  protected RunServerActionBase(String text, String description, Image icon) {
    super(text, description, icon);
  }

  protected void performAction(@Nonnull ServerNode serverNode) {
    if (serverNode.isStartActionEnabled(getExecutor())) {
      serverNode.startServer(getExecutor());
    }
  }

  @Override
  protected boolean isEnabledForServer(@Nonnull ServerNode serverNode) {
    return serverNode.isStartActionEnabled(getExecutor());
  }

  protected abstract Executor getExecutor();
}
