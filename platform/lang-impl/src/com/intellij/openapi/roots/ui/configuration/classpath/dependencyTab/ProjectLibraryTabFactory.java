/*
 * Copyright 2013-2014 must-be.org
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
package com.intellij.openapi.roots.ui.configuration.classpath.dependencyTab;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.roots.ui.configuration.classpath.ClasspathPanel;
import com.intellij.openapi.roots.ui.configuration.projectRoot.StructureConfigurableContext;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author VISTALL
 * @since 27.09.14
 */
public class ProjectLibraryTabFactory implements AddModuleDependencyTabFactory {
  @NotNull
  @Override
  public AddModuleDependencyTabContext createTabContext(@NotNull Disposable parent,
                                                        @NotNull ClasspathPanel panel,
                                                        @NotNull StructureConfigurableContext context) {
    return new AddModuleDependencyTabContext(panel, context) {
      @NotNull
      @Override
      public String getTabName() {
        return "Project Library";
      }

      @Override
      public boolean isEmpty() {
        return true;
      }

      @NotNull
      @Override
      public JComponent getComponent() {
        return new JPanel();
      }
    };
  }
}
