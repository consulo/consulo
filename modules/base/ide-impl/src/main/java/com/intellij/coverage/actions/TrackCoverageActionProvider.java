/*
 * User: anna
 * Date: 28-Aug-2009
 */
package com.intellij.coverage.actions;

import consulo.execution.test.action.ToggleModelActionProvider;
import consulo.execution.test.action.ToggleModelAction;
import consulo.execution.test.TestConsoleProperties;

public class TrackCoverageActionProvider implements ToggleModelActionProvider{
  public ToggleModelAction createToggleModelAction(TestConsoleProperties properties) {
    return new TrackCoverageAction(properties);
  }
}