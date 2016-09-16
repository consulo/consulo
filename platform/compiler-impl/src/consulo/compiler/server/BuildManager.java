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
package consulo.compiler.server;

import com.intellij.openapi.project.Project;

import java.io.File;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 23:49/25.05.13
 */
public class BuildManager {
  public static final BuildManager ourInstance = new BuildManager();

  public static BuildManager getInstance() {
    return ourInstance;
  }

  public void notifyFilesDeleted(Collection<File> paths) {

  }

  public void notifyFilesChanged(Collection<File> paths) {

  }

  public File getProjectSystemDirectory(Project project) {
    return null;
  }

  public void clearState(Project project) {

  }
  public void runCommand(Runnable runnable) {
    runnable.run();
  }
}
