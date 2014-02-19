/*
 * Copyright 2013 must-be.org
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
package org.consulo.projectImport;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.consulo.projectImport.model.ProjectModel;
import org.jetbrains.annotations.NotNull;

/**
 * @author VISTALL
 * @since 17:26/19.06.13
 */
public abstract class ProjectModelProcessor<T> {
  public static final ExtensionPointName<ProjectModelProcessor> EP_NAME = ExtensionPointName.create("com.intellij.projectModelProcessor");

  public static <T> void processModel(@NotNull ProjectModel projectModel, @NotNull T o) {
    for (ProjectModelProcessor<?> processor : EP_NAME.getExtensions()) {
      if(processor.isAccepted(o)) {
        ProjectModelProcessor<T> temp = (ProjectModelProcessor<T>)processor;

        temp.process(o, projectModel);
      }
    }
  }

  public abstract void process(@NotNull T otherModel, @NotNull  ProjectModel projectModel);

  public abstract boolean isAccepted(Object o);
}
