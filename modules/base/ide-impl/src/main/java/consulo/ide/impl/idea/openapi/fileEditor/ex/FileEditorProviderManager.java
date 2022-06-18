/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileEditor.ex;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.Service;
import consulo.ide.ServiceManager;
import consulo.fileEditor.FileEditorProvider;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
@Service(ComponentScope.APPLICATION)
public abstract class FileEditorProviderManager {
  public static FileEditorProviderManager getInstance() {
    return ServiceManager.getService(FileEditorProviderManager.class);
  }

  /**
   * @param file cannot be <code>null</code>
   * @return array of all editors providers that can create editor
   * for the specified <code>file</code>. The method returns
   * an empty array if there are no such providers. Please note that returned array
   * is constructed with respect to editor policies.
   */
  @Nonnull
  public abstract FileEditorProvider[] getProviders(@Nonnull Project project, @Nonnull VirtualFile file);

  /**
   * @return may be null
   */
  @Nullable
  public abstract FileEditorProvider getProvider(@Nonnull String editorTypeId);
}
