/*
 * User: anna
 * Date: 28-Aug-2009
 */
package consulo.ide.impl.idea.coverage.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.action.ToggleModelAction;
import consulo.execution.test.action.ToggleModelActionProvider;

@ExtensionImpl
public class TrackCoverageActionProvider implements ToggleModelActionProvider {
  @Override
  public ToggleModelAction createToggleModelAction(TestConsoleProperties properties) {
    return new TrackCoverageAction(properties);
  }
}