package consulo.ide.impl.idea.coverage;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Extension;
import consulo.component.extension.ExtensionList;
import consulo.configurable.ConfigurableProvider;
import consulo.project.Project;

/**
 * @author traff
 */
@Extension(ComponentScope.PROJECT)
public abstract class CoverageOptions extends ConfigurableProvider {
  public static final ExtensionList<CoverageOptions, Project> EP_NAME = ExtensionList.of(CoverageOptions.class);
}
