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

import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import consulo.disposer.Disposable;
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

  boolean isEmpty();

  @Nonnull
  VirtualFile[] getFiles();

  @Nonnull
  VirtualFile[] getDirectories();

  @Nullable
  VirtualFilePointer findByUrl(@Nonnull String url);

  void clear();

  int size();

  /**
   * For example, to read from the xml below, call {@code readExternal(myRootTag, "childElementName"); }
   * <pre>{@code
   * <myroot>
   *   <childElementName url="xxx1"/>
   *   <childElementName url="xxx2"/>
   * </myroot>
   * }</pre>
   */
  void readExternal(@Nonnull Element rootChild, @Nonnull String childElementName, boolean externalizeJarDirectories) throws InvalidDataException;

  void writeExternal(@Nonnull Element element, @Nonnull String childElementName, boolean externalizeJarDirectories);

  void moveUp(@Nonnull String url);

  void moveDown(@Nonnull String url);

  @Nonnull
  VirtualFilePointerContainer clone(@Nonnull Disposable parent);

  @Nonnull
  VirtualFilePointerContainer clone(@Nonnull Disposable parent, @Nullable VirtualFilePointerListener listener);

  /**
   * Adds {@code directory} as a root of jar files.
   * After this call the {@link #getFiles()} will additionally return jar files in this directory
   * (and, if {@code recursively} was set, the jar files in all-subdirectories).
   * {@link #getUrls()} will additionally return the {@code directoryUrl}.
   */
  void addJarDirectory(@Nonnull String directoryUrl, boolean recursively);

  /**
   * Removes {@code directory} from the roots of jar files.
   * After that the {@link #getFiles()} and {@link #getUrls()} etc will not return jar files in this directory anymore.
   *
   * @return true if removed
   */
  boolean removeJarDirectory(@Nonnull String directoryUrl);

  /**
   * Returns list of (directory url, isRecursive) which were added via {@link #addJarDirectory(String, boolean)} }
   */
  @Nonnull
  List<Pair<String, Boolean>> getJarDirectories();
}
