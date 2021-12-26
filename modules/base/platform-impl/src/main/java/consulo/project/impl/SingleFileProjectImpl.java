/*
 * Copyright 2013-2021 consulo.io
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
package consulo.project.impl;

import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.impl.ProjectImpl;
import org.jdom.Element;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author VISTALL
 * @since 25/12/2021
 */
public class SingleFileProjectImpl extends ProjectImpl {
  private Element myStateElement;
  private boolean myInitialized;

  protected SingleFileProjectImpl(@Nonnull Application application, @Nonnull ProjectManager manager, @Nonnull String dirPath, boolean isOptimiseTestLoadSpeed, String projectName, boolean noUIThread) {
    super(application, manager, dirPath, isOptimiseTestLoadSpeed, projectName, noUIThread);
  }

  @Nullable
  public Element getStateElement() {
    return myStateElement;
  }

  public void setStateElement(@Nullable Element stateElement) {
    myStateElement = stateElement;
  }

  public void setInitialized() {
    myInitialized = true;
  }

  @Override
  public boolean isInitialized() {
    return myInitialized;
  }
}
