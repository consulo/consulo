package com.intellij.remoteServer.configuration.deployment;

import consulo.component.persist.PersistentStateComponent;

/**
 * @author nik
 */
public abstract class DeploymentConfiguration {
  public abstract PersistentStateComponent<?> getSerializer();
}
