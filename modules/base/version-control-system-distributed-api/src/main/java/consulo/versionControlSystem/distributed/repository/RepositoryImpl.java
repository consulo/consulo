/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.repository;

import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Nadya Zabrodina
 */
public abstract class RepositoryImpl implements Repository {

  @Nonnull
  private final Project myProject;
  @Nonnull
  private final VirtualFile myRootDir;


  @Nonnull
  protected volatile State myState;
  @Nullable protected volatile String myCurrentRevision;

  protected RepositoryImpl(@Nonnull Project project,
                           @Nonnull VirtualFile dir,
                           @Nonnull Disposable parentDisposable) {
    myProject = project;
    myRootDir = dir;
    Disposer.register(parentDisposable, this);
  }

  @Override
  @Nonnull
  public VirtualFile getRoot() {
    return myRootDir;
  }

  @Override
  @Nonnull
  public String getPresentableUrl() {
    return getRoot().getPresentableUrl();
  }

  @Override
  public String toString() {
    return getPresentableUrl();
  }

  @Override
  @Nonnull
  public Project getProject() {
    return myProject;
  }

  @Nonnull
  @Override
  public State getState() {
    return myState;
  }


  @Override
  @jakarta.annotation.Nullable
  public String getCurrentRevision() {
    return myCurrentRevision;
  }

  @Override
  public void dispose() {
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Repository that = (Repository)o;

    if (!getProject().equals(that.getProject())) return false;
    if (!getRoot().equals(that.getRoot())) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = getProject().hashCode();
    result = 31 * result + (getRoot().hashCode());
    return result;
  }
}


