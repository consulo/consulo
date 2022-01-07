/*
 * Copyright 2013-2017 consulo.io
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
package consulo.moduleImport;

import consulo.disposer.Disposable;
import com.intellij.openapi.project.Project;
import consulo.ide.wizard.newModule.NewModuleWizardContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public class ModuleImportContext implements NewModuleWizardContext, Disposable {
  @Nullable
  private final Project myProject;
  private String myFileToImport;

  private String myName;
  private String myPath;

  public ModuleImportContext(@Nullable Project project) {
    myProject = project;
  }

  public void setFileToImport(String fileToImport) {
    myFileToImport = fileToImport;
  }

  public String getFileToImport() {
    return myFileToImport;
  }

  @Override
  @Nullable
  public Project getProject() {
    return myProject;
  }

  @Override
  public void dispose() {
  }

  @Override
  public void setName(@Nonnull String name) {
    myName = name;
  }

  @Override
  public void setPath(@Nonnull String path) {
    myPath = path;
  }

  @Nonnull
  @Override
  public String getName() {
    return myName;
  }

  @Nonnull
  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public boolean isNewProject() {
    return myProject == null;
  }
}
