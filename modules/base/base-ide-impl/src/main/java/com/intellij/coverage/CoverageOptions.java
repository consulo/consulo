package com.intellij.coverage;

import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import consulo.component.extension.Extension;
import consulo.component.extension.ExtensionList;

/**
 * @author traff
 */
@Extension(name = "coverageOptions", component = Project.class)
public abstract class CoverageOptions extends ConfigurableProvider {
  public static final ExtensionList<CoverageOptions, Project> EP_NAME = ExtensionList.of(CoverageOptions.class);
}
