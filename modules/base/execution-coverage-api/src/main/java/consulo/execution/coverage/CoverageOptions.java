package consulo.execution.coverage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.configurable.Configurable;
import jakarta.annotation.Nullable;

/**
 * @author traff
 */
@ExtensionAPI(ComponentScope.PROJECT)
public interface CoverageOptions {
    @Nullable
    Configurable createConfigurable();
}
