/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.content.bundle;

import consulo.component.util.pointer.Named;
import consulo.content.RootProvider;
import consulo.platform.Platform;
import consulo.util.collection.ArrayFactory;
import consulo.util.dataholder.UserDataHolder;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.nio.file.Path;

/**
 * @author Eugene Zhuravlev
 * @since 2004-09-23
 */
public interface Sdk extends UserDataHolder, Named {
  public static final Sdk[] EMPTY_ARRAY = new Sdk[0];

  public static ArrayFactory<Sdk> ARRAY_FACTORY = count -> count == 0 ? EMPTY_ARRAY : new Sdk[count];

  @Nonnull
  SdkTypeId getSdkType();

  boolean isPredefined();

  @Nonnull
  @Override
  String getName();

  @Nullable
  String getVersionString();

  @Nullable
  String getHomePath();

  @Nonnull
  Path getHomeNioPath();

  @Nonnull
  Platform getPlatform();

  @Nullable
  VirtualFile getHomeDirectory();

  @Nonnull
  RootProvider getRootProvider();

  @Nonnull
  SdkModificator getSdkModificator();

  @Nullable
  SdkAdditionalData getSdkAdditionalData();

  @Nonnull
  Object clone() throws CloneNotSupportedException;
}
