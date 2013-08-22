/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.compiler.impl.javaCompiler.JavaCompilerConfiguration;
import com.intellij.compiler.impl.javaCompiler.annotationProcessing.ProcessorConfigProfile;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 5, 2009
 */
public class AnnotationProcessorsConfigurable implements SearchableConfigurable, Configurable.NoScroll {

  private final Project myProject;
  private AnnotationProcessorsPanel myMainPanel;
  private JavaCompilerConfiguration myCompilerConfiguration;

  public AnnotationProcessorsConfigurable(final Project project) {
    myProject = project;
    myCompilerConfiguration = JavaCompilerConfiguration.getInstance(project);
  }

  @Override
  public String getDisplayName() {
    return "Annotation Processors";
  }

  @Override
  public String getHelpTopic() {
    return "reference.projectsettings.compiler.annotationProcessors";
  }

  @Override
  @NotNull
  public String getId() {
    return getHelpTopic();
  }

  @Override
  public Runnable enableSearch(String option) {
    return null;
  }

  @Override
  public JComponent createComponent() {
    myMainPanel = new AnnotationProcessorsPanel(myProject);
    return myMainPanel;
  }

  @Override
  public boolean isModified() {
    if (!myCompilerConfiguration.getDefaultProcessorProfile().equals(myMainPanel.getDefaultProfile())) {
      return true;
    }

    final Map<String, ProcessorConfigProfile> configProfiles = new HashMap<String, ProcessorConfigProfile>();
    for (ProcessorConfigProfile profile : myCompilerConfiguration.getModuleProcessorProfiles()) {
      configProfiles.put(profile.getName(), profile);
    }
    final List<ProcessorConfigProfile> panelProfiles = myMainPanel.getModuleProfiles();
    if (configProfiles.size() != panelProfiles.size()) {
      return true;
    }
    for (ProcessorConfigProfile panelProfile : panelProfiles) {
      final ProcessorConfigProfile configProfile = configProfiles.get(panelProfile.getName());
      if (configProfile == null || !configProfile.equals(panelProfile)) {
        return true;
      }
    }

    return false;
  }

  @Override
  public void apply() throws ConfigurationException {
    myCompilerConfiguration.setDefaultProcessorProfile(myMainPanel.getDefaultProfile());
    myCompilerConfiguration.setModuleProcessorProfiles(myMainPanel.getModuleProfiles());
  }

  @Override
  public void reset() {
    myMainPanel.initProfiles(myCompilerConfiguration.getDefaultProcessorProfile(), myCompilerConfiguration.getModuleProcessorProfiles());
  }

  @Override
  public void disposeUIResources() {
  }

}
