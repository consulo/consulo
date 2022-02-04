package com.intellij.coverage;

import consulo.configurable.ConfigurableProvider;
import consulo.project.Project;
import consulo.component.extension.ExtensionType;
import consulo.component.extension.ExtensionList;

/**
 * @author traff
 */
@ExtensionType(value = "coverageOptions", component = Project.class)
public abstract class CoverageOptions extends ConfigurableProvider {
  public static final ExtensionList<CoverageOptions, Project> EP_NAME = ExtensionList.of(CoverageOptions.class);
}
