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
package com.intellij.compiler.options;

import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.consulo.compiler.CompilerSettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class CompilerConfigurable implements SearchableConfigurable.Parent, Configurable.NoScroll {

  private final Project myProject;
  private Configurable[] myKids;

  public CompilerConfigurable(Project project) {
    myProject = project;
  }

  public String getDisplayName() {
    return CompilerBundle.message("compiler.configurable.display.name");
  }

  public String getHelpTopic() {
    return "project.propCompiler";
  }

  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Nullable
  public Runnable enableSearch(String option) {
    return null;
  }

  public JComponent createComponent() {
    return null;
  }

  public boolean hasOwnContent() {
    return true;
  }

  public boolean isVisible() {
    return true;
  }

  public boolean isModified() {
    return false;
  }

  public void apply() throws ConfigurationException {

  }

  public void reset() {

  }

  public void disposeUIResources() {

  }

  public Configurable[] getConfigurables() {
    if (myKids == null) {
      final CompilerManager compilerManager = CompilerManager.getInstance(myProject);
      final com.intellij.openapi.compiler.Compiler[] compilers = compilerManager.getAllCompilers();
      List<Configurable> configurables = new ArrayList<Configurable>(compilers.length);
      for (Compiler compiler : compilers) {
        final CompilerSettings<Compiler> settings = compilerManager.getSettings(compiler);

        final Configurable configurable = settings.createConfigurable();
        if(configurable != null) {
          configurables.add(configurable);
        }
      }

      myKids = configurables.toArray(new Configurable[configurables.size()]);
    }

    return myKids;
  }
}
