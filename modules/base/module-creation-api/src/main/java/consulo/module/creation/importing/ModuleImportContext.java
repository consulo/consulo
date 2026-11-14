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
package consulo.module.creation.importing;

import consulo.application.Application;
import consulo.application.platform.PlatformTarget;
import consulo.application.platform.PlatformTargetManager;
import consulo.disposer.Disposable;
import consulo.module.creation.NewModuleWizardContext;
import consulo.platform.Platform;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 30-Jan-17
 */
public class ModuleImportContext implements NewModuleWizardContext, Disposable {
  private final @Nullable Project myProject;
  private String myFileToImport;

  private String myName;
  private String myPath;
  private FileSystem myFileSystem = FileSystems.getDefault();

  public ModuleImportContext(@Nullable Project project) {
    myProject = project;
  }

  public void setFileToImport(String fileToImport) {
    myFileToImport = fileToImport;
  }

  public String getFileToImport() {
    return myFileToImport;
  }

  /**
   * The file system the imported file belongs to - the local one, or a remote platform's.
   * The paths of this context are paths of it.
   */
  public void setFileSystem(FileSystem fileSystem) {
    myFileSystem = fileSystem;
  }

  /**
   * {@link #getPath()} as a path of the file system the project is imported on.
   */
  public Path getNioPath() {
    return myFileSystem.getPath(myPath);
  }

  @Override
  public @Nullable Project getProject() {
    return myProject;
  }

  @Override
  public void dispose() {
  }

  @Override
  public void setName(String name) {
    myName = name;
  }

  @Override
  public void setPath(String path) {
    myPath = path;
  }

  
  @Override
  public String getName() {
    return myName;
  }

  
  @Override
  public String getPath() {
    return myPath;
  }

  @Override
  public boolean isNewProject() {
    return myProject == null;
  }

  /**
   * The machine of the imported files - the path tells it.
   */
  @Override
  public boolean isPlatformChoosable() {
    return false;
  }

  @Override
  public Platform getPlatform() {
    PlatformTarget target = findTarget();
    Platform platform = target == null ? null : target.getPlatform();
    if (platform != null) {
      return platform;
    }
    return myProject != null ? myProject.getPlatform() : Platform.current();
  }

  @Override
  public @Nullable String getPlatformTargetId() {
    if (myFileSystem == FileSystems.getDefault()) {
      return myProject != null ? myProject.getPlatformTargetId() : null;
    }
    PlatformTarget target = findTarget();
    return target != null ? target.getId() : null;
  }

  private @Nullable PlatformTarget findTarget() {
    if (myFileSystem == FileSystems.getDefault() || myPath == null) {
      return null;
    }
    return Application.get().getInstance(PlatformTargetManager.class).findTargetOf(getNioPath());
  }
}
