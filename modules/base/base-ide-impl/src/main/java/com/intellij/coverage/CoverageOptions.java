package com.intellij.coverage;

import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import consulo.component.extension.StrictExtensionPointName;

/**
 * @author traff
 */
public abstract class CoverageOptions extends ConfigurableProvider {
  public static final StrictExtensionPointName<Project, CoverageOptions> EP_NAME = StrictExtensionPointName.of(Project.class, "com.intellij.coverageOptions");
}
