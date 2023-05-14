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
package consulo.virtualFileSystem;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.component.ComponentManager;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import jakarta.annotation.Nonnull;
import java.util.Collection;

@ServiceAPI(ComponentScope.PROJECT)
public abstract class ReadonlyStatusHandler {
  public static boolean ensureFilesWritable(@Nonnull ComponentManager project, @Nonnull VirtualFile... files) {
    return !getInstance(project).ensureFilesWritable(files).hasReadonlyFiles();
  }

  public static ReadonlyStatusHandler getInstance(ComponentManager project) {
    return project.getInstance(ReadonlyStatusHandler.class);
  }

  public abstract static class OperationStatus {

    @Nonnull
    public abstract VirtualFile[] getReadonlyFiles();

    public abstract boolean hasReadonlyFiles();

    @Nonnull
    public abstract String getReadonlyFilesMessage();

  }

  public abstract OperationStatus ensureFilesWritable(@Nonnull VirtualFile... files);

  public OperationStatus ensureFilesWritable(@Nonnull Collection<VirtualFile> files) {
    return ensureFilesWritable(VirtualFileUtil.toVirtualFileArray(files));
  }
}
