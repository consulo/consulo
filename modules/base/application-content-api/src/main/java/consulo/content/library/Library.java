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
package consulo.content.library;

import consulo.content.OrderRootType;
import consulo.content.RootProvider;
import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import consulo.util.xml.serializer.JDOMExternalizable;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author dsl
 */
public interface Library extends JDOMExternalizable, Disposable {
  Key<Library> KEY = Key.create(Library.class);

  Library[] EMPTY_ARRAY = new Library[0];

  @Nullable
  String getName();

  @Nonnull
  String[] getUrls(@Nonnull OrderRootType rootType);

  @Nonnull
  VirtualFile[] getFiles(@Nonnull OrderRootType rootType);

  /**
   * As soon as you obtaining modifiable model you will have to commit it or call Disposer.dispose(model)!
   */
  @Nonnull
  ModifiableModel getModifiableModel();

  LibraryTable getTable();

  @Nonnull
  RootProvider getRootProvider();

  boolean isJarDirectory(@Nonnull String url);

  boolean isJarDirectory(@Nonnull String url, @Nonnull OrderRootType rootType);

  boolean isValid(@Nonnull String url, @Nonnull OrderRootType rootType);

  interface ModifiableModel extends Disposable {
    @Nonnull
    String[] getUrls(@Nonnull OrderRootType rootType);

    void setName(String name);

    String getName();

    void addRoot(@Nonnull String url, @Nonnull OrderRootType rootType);

    void addJarDirectory(@Nonnull String url, boolean recursive);

    void addJarDirectory(@Nonnull String url, boolean recursive, @Nonnull OrderRootType rootType);

    void addRoot(@Nonnull VirtualFile file, @Nonnull OrderRootType rootType);

    void addJarDirectory(@Nonnull VirtualFile file, boolean recursive);

    void addJarDirectory(@Nonnull VirtualFile file, boolean recursive, @Nonnull OrderRootType rootType);

    void moveRootUp(@Nonnull String url, @Nonnull OrderRootType rootType);

    void moveRootDown(@Nonnull String url, @Nonnull OrderRootType rootType);

    boolean removeRoot(@Nonnull String url, @Nonnull OrderRootType rootType);

    void commit();

    @Nonnull
    VirtualFile[] getFiles(@Nonnull OrderRootType rootType);

    boolean isChanged();

    boolean isJarDirectory(@Nonnull String url);

    boolean isJarDirectory(@Nonnull String url, @Nonnull OrderRootType rootType);

    boolean isValid(@Nonnull String url, @Nonnull OrderRootType rootType);
  }
}
