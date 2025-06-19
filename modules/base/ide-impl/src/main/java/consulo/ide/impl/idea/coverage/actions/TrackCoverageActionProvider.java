package consulo.ide.impl.idea.coverage.actions;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.action.ToggleModelAction;
import consulo.execution.test.action.ToggleModelActionProvider;

/**
 * @author anna
 * @since 2009-08-28
 */
@ExtensionImpl
public class TrackCoverageActionProvider implements ToggleModelActionProvider {
    @Override
    public ToggleModelAction createToggleModelAction(TestConsoleProperties properties) {
        return new TrackCoverageAction(properties);
    }
}