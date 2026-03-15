// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.module.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.module.content.internal.PushedFilePropertiesUpdaterInternal;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;

import java.util.function.Predicate;

@ServiceAPI(value = ComponentScope.PROJECT)
public sealed interface PushedFilePropertiesUpdater permits PushedFilePropertiesUpdaterInternal {
  
  public static PushedFilePropertiesUpdater getInstance(Project project) {
    return project.getInstance(PushedFilePropertiesUpdater.class);
  }

  void initializeProperties();

  void pushAll(FilePropertyPusher<?>... pushers);

  /**
   * @deprecated Use {@link #filePropertiesChanged(VirtualFile, Predicate)}
   */
  @Deprecated
  void filePropertiesChanged(VirtualFile file);

  void pushAllPropertiesNow();

  <T> void findAndUpdateValue(VirtualFile fileOrDir, FilePropertyPusher<T> pusher, T moduleValue);

  /**
   * Invalidates indices and other caches for the given file or its immediate children (in case it's a directory).
   * Only files matching the condition are processed.
   */
  void filePropertiesChanged(VirtualFile fileOrDir, Predicate<? super VirtualFile> acceptFileCondition);
}
