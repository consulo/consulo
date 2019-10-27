/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ide.wizard.newModule;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2019-08-20
 */
public class NewModuleWizardContextBase implements NewModuleWizardContext {
  private String myName;
  private String myPath;

  private final boolean myIsNewProject;

  public NewModuleWizardContextBase(boolean isNewProject) {
    myIsNewProject = isNewProject;
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
    return myIsNewProject;
  }
}
