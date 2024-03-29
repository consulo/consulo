// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.content.bundle;

import consulo.content.OrderRootType;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileManager;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.file.Path;
import java.util.Arrays;

/**
 * The way to modify the {@link Sdk} roots, home path etc.<br>
 * First you call {@link Sdk#getSdkModificator()}<br>
 * Then you modify things via SdkModificator setters, e.g. {@link #setHomePath(String)}<br>
 * Last, you must call {@link #commitChanges()}
 */
public interface SdkModificator {
  @Nonnull
  String getName();

  void setName(@Nonnull String name);

  String getHomePath();

  void setHomePath(String path);

  @Nonnull
  Path getHomeNioPath();

  void setHomeNioPath(@Nonnull Path path);

  @Nullable
  String getVersionString();

  void setVersionString(String versionString);

  SdkAdditionalData getSdkAdditionalData();

  void setSdkAdditionalData(SdkAdditionalData data);

  @Nonnull
  VirtualFile[] getRoots(@Nonnull OrderRootType rootType);

  @Nonnull
  default Object[] getUrls(@Nonnull OrderRootType rootType) {
    return Arrays.stream(getRoots(rootType)).map(VirtualFile::getUrl).toArray(ArrayUtil.STRING_ARRAY_FACTORY);
  }

  void addRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType);

  default void addRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    VirtualFile rootFile = VirtualFileManager.getInstance().findFileByUrl(url);
    if (rootFile != null) {
      addRoot(rootFile, rootType);
    }
  }

  void removeRoot(@Nonnull VirtualFile root, @Nonnull OrderRootType rootType);

  default void removeRoot(@Nonnull String url, @Nonnull OrderRootType rootType) {
    for (VirtualFile file : getRoots(rootType)) {
      if (file.getUrl().equals(url)) {
        removeRoot(file, rootType);
        break;
      }
    }
  }

  void removeRoots(@Nonnull OrderRootType rootType);

  void removeAllRoots();

  void commitChanges();

  boolean isWritable();
}
