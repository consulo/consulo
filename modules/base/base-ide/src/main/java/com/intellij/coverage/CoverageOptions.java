package com.intellij.coverage;

import com.intellij.openapi.options.ConfigurableProvider;
import com.intellij.openapi.project.Project;
import consulo.extensions.StrictExtensionPointName;

/**
 * @author traff
 */
public abstract class CoverageOptions extends ConfigurableProvider {
  public static final StrictExtensionPointName<Project, CoverageOptions> EP_NAME = StrictExtensionPointName.create(Project.class, "com.intellij.coverageOptions");
}
