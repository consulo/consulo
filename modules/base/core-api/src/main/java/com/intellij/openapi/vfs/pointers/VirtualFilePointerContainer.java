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
package com.intellij.openapi.vfs.pointers;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.vfs.VirtualFile;
import org.jdom.Element;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.List;

/**
 * @author dsl
 */
public interface VirtualFilePointerContainer {
  void killAll();

  void add(@Nonnull VirtualFile file);

  void add(@Nonnull String url);

  void remove(@Nonnull VirtualFilePointer pointer);

  @Nonnull
  List<VirtualFilePointer> getList();

  void addAll(@Nonnull VirtualFilePointerContainer that);

  @Nonnull
  String[] getUrls();

  @Nonnull
  VirtualFile[] getFiles();

  @Nonnull
  VirtualFile[] getDirectories();

  @Nullable
  VirtualFilePointer findByUrl(@Nonnull String url);

  void clear();

  int size();

  void readExternal(@Nonnull Element rootChild, @Nonnull String childElementName) throws InvalidDataException;

  void writeExternal(@Nonnull Element element, @Nonnull String childElementName);

  void moveUp(@Nonnull String url);

  void moveDown(@Nonnull String url);

  @Nonnull
  VirtualFilePointerContainer clone(@Nonnull Disposable parent);

  @Nonnull
  VirtualFilePointerContainer clone(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);
}
