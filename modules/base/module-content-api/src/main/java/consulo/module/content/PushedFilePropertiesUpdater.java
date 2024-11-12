// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.module.content;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.util.function.Predicate;

@ServiceAPI(value = ComponentScope.PROJECT)
public abstract class PushedFilePropertiesUpdater {
  @Nonnull
  public static PushedFilePropertiesUpdater getInstance(@Nonnull Project project) {
    return project.getInstance(PushedFilePropertiesUpdater.class);
  }

  public abstract void initializeProperties();

  public abstract void pushAll(final FilePropertyPusher<?>... pushers);

  /**
   * @deprecated Use {@link #filePropertiesChanged(VirtualFile, Predicate)}
   */
  @Deprecated
  public abstract void filePropertiesChanged(@Nonnull final VirtualFile file);

  public abstract void pushAllPropertiesNow();

  public abstract <T> void findAndUpdateValue(final VirtualFile fileOrDir, final FilePropertyPusher<T> pusher, final T moduleValue);

  /**
   * Invalidates indices and other caches for the given file or its immediate children (in case it's a directory).
   * Only files matching the condition are processed.
   */
  public abstract void filePropertiesChanged(@Nonnull VirtualFile fileOrDir, @Nonnull Predicate<? super VirtualFile> acceptFileCondition);
}
