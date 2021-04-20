/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.roots.ui.configuration;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.projectRoot.BaseLibrariesConfigurable;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nonnull;
import javax.swing.*;

@Singleton
@Deprecated
public class ProjectStructureConfigurable implements SearchableConfigurable, Configurable.HoldPreferredFocusedComponent {
  @Override
  @Nonnull
  public String getId() {
    return "project.structure";
  }

  @Override
  @Nls
  public String getDisplayName() {
    return ProjectBundle.message("project.settings.display.name");
  }

  @Override
  public JComponent createComponent() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isModified() {


    return false;
  }

  @Override
  public void apply() throws ConfigurationException {

  }

  public static ProjectStructureConfigurable getInstance(final Project project) {
    throw new UnsupportedOperationException();
  }


  public ProjectConfigurable getProjectConfigurable() {
    return null;
  }

  public BaseLibrariesConfigurable getConfigurableFor(final Library library) {
    return null;

  }
}
