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
package com.intellij.mock;

import com.intellij.openapi.components.StateStorage;
import com.intellij.openapi.components.TrackingPathMacroSubstitutor;
import consulo.annotation.access.RequiredWriteAction;
import consulo.components.impl.stores.IProjectStore;
import consulo.components.impl.stores.storage.StateStorageManager;
import com.intellij.openapi.project.impl.ProjectImpl;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.components.impl.stores.StateComponentInfo;
import consulo.ui.UIAccess;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author peter
 */
public class MockProjectStore implements IProjectStore {
  @Override
  public void setProjectFilePathNoUI(@Nonnull String filePath) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setProjectFilePath(@Nonnull final String filePath) {
    throw new UnsupportedOperationException("Method setProjectFilePath is not yet implemented in " + getClass().getName());
  }

  @Override
  public void reinitComponents(@Nonnull Set<String> componentNames, boolean reloadData) {
    throw new UnsupportedOperationException("Method reinitComponents is not yet implemented in " + getClass().getName());
  }

  @Override
  public TrackingPathMacroSubstitutor[] getSubstitutors() {
    return new TrackingPathMacroSubstitutor[0];
  }

  @Override
  public VirtualFile getProjectBaseDir() {
    throw new UnsupportedOperationException("Method getProjectBaseDir is not yet implemented in " + getClass().getName());
  }

  @Override
  public String getProjectBasePath() {
    throw new UnsupportedOperationException("Method getProjectBasePath is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public String getProjectName() {
    throw new UnsupportedOperationException("Method getProjectName not implemented in " + getClass());
  }

  @Override
  @Nullable
  public VirtualFile getProjectFile() {
    throw new UnsupportedOperationException("Method getProjectFile is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public VirtualFile getWorkspaceFile() {
    throw new UnsupportedOperationException("Method getWorkspaceFile is not yet implemented in " + getClass().getName());
  }

  @Override
  public void loadProjectFromTemplate(@Nonnull ProjectImpl project) {
    throw new UnsupportedOperationException("Method loadProjectFromTemplate is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nonnull
  public String getProjectFilePath() {
    throw new UnsupportedOperationException("Method getProjectFilePath is not yet implemented in " + getClass().getName());
  }

  @Override
  public <T> StateComponentInfo<T> loadStateIfStorable(@Nonnull T component) {
    throw new UnsupportedOperationException("Method initComponent is not yet implemented in " + getClass().getName());
  }

  @Override
  public void load() throws IOException {
    throw new UnsupportedOperationException("Method load is not yet implemented in " + getClass().getName());
  }

  @Override
  public void save(boolean force, @Nonnull List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {

  }

  @RequiredWriteAction
  @Override
  public void saveAsync(@Nonnull UIAccess uiAccess, @Nonnull List<Pair<StateStorage.SaveSession, File>> readonlyFiles) {

  }

  @Override
  @Nullable
  public String getPresentableUrl() {
    throw new UnsupportedOperationException("Method getPresentableUrl not implemented in " + getClass());
  }

  @Override
  public boolean reload(@Nonnull Collection<? extends StateStorage> changedStorages) {
    return false;
  }

  @Nonnull
  @Override
  public StateStorageManager getStateStorageManager() {
    throw new UnsupportedOperationException("Method getStateStorageManager not implemented in " + getClass());
  }
}
