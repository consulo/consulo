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
package com.intellij.openapi.projectRoots;

import com.intellij.openapi.roots.RootProvider;
import com.intellij.openapi.util.UserDataHolder;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayFactory;
import consulo.util.pointers.Named;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Eugene Zhuravlev
 *         Date: Sep 23, 2004
 */
public interface Sdk extends UserDataHolder, Named {
  public static final Sdk[] EMPTY_ARRAY = new Sdk[0];

  public static ArrayFactory<Sdk> ARRAY_FACTORY = new ArrayFactory<Sdk>() {
    @NotNull
    @Override
    public Sdk[] create(int count) {
      return count == 0 ? EMPTY_ARRAY : new Sdk[count];
    }
  };

  @NotNull
  SdkTypeId getSdkType();

  boolean isPredefined();

  @NotNull
  @Override
  String getName();

  @Nullable
  String getVersionString();

  @Nullable
  String getHomePath();

  @Nullable
  VirtualFile getHomeDirectory();

  @NotNull
  RootProvider getRootProvider();

  @NotNull
  SdkModificator getSdkModificator();

  @Nullable
  SdkAdditionalData getSdkAdditionalData();

  @NotNull
  Object clone() throws CloneNotSupportedException;
}
