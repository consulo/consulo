package consulo.execution.coverage.impl.internal.action;

import consulo.annotation.component.ExtensionImpl;
import consulo.execution.test.TestConsoleProperties;
import consulo.execution.test.action.ToggleModelAction;
import consulo.execution.test.action.ToggleModelActionProvider;
import jakarta.annotation.Nonnull;

/**
 * @author anna
 * @since 2009-08-28
 */
@ExtensionImpl
public class TrackCoverageActionProvider implements ToggleModelActionProvider {
    @Nonnull
    @Override
    public ToggleModelAction createToggleModelAction(TestConsoleProperties properties) {
        return new TrackCoverageAction(properties);
    }
}