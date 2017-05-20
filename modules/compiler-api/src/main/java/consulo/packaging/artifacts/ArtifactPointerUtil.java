/*
 * Copyright 2013-2016 consulo.io
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
package consulo.packaging.artifacts;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.packaging.artifacts.ArtifactPointerManager;

/**
 * @author VISTALL
 * @since 16:57/05.07.13
 */
public class ArtifactPointerUtil {
  public static ArtifactPointerManager getPointerManager(Project project) {
    return  ServiceManager.getService(project, ArtifactPointerManager.class);
  }
}
