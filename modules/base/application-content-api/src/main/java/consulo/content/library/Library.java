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

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author dsl
 */
public interface Library extends JDOMExternalizable, Disposable {
  Key<Library> KEY = Key.create(Library.class);

  Library[] EMPTY_ARRAY = new Library[0];

  @Nullable
  String getName();

  
  String[] getUrls(OrderRootType rootType);

  
  VirtualFile[] getFiles(OrderRootType rootType);

  /**
   * As soon as you obtaining modifiable model you will have to commit it or call Disposer.dispose(model)!
   */
  
  ModifiableModel getModifiableModel();

  LibraryTable getTable();

  
  RootProvider getRootProvider();

  boolean isJarDirectory(String url);

  boolean isJarDirectory(String url, OrderRootType rootType);

  boolean isValid(String url, OrderRootType rootType);

  @Nullable
  PersistentLibraryKind<?> getKind();

  LibraryProperties getProperties();

  
  String[] getExcludedRootUrls();

  
  VirtualFile[] getExcludedRoots();

  
  List<String> getInvalidRootUrls(OrderRootType type);

  default boolean isDisposed() {
    return false;
  }

  interface ModifiableModel extends Disposable {
    
    String[] getUrls(OrderRootType rootType);

    void setName(String name);

    String getName();

    void addRoot(String url, OrderRootType rootType);

    void addJarDirectory(String url, boolean recursive);

    void addJarDirectory(String url, boolean recursive, OrderRootType rootType);

    void addRoot(VirtualFile file, OrderRootType rootType);

    void addJarDirectory(VirtualFile file, boolean recursive);

    void addJarDirectory(VirtualFile file, boolean recursive, OrderRootType rootType);

    void moveRootUp(String url, OrderRootType rootType);

    void moveRootDown(String url, OrderRootType rootType);

    boolean removeRoot(String url, OrderRootType rootType);

    void commit();

    
    VirtualFile[] getFiles(OrderRootType rootType);

    boolean isChanged();

    boolean isJarDirectory(String url);

    boolean isJarDirectory(String url, OrderRootType rootType);

    boolean isValid(String url, OrderRootType rootType);

    void setProperties(LibraryProperties properties);

    LibraryProperties getProperties();

    void setKind(PersistentLibraryKind<?> type);

    PersistentLibraryKind<?> getKind();

    void addExcludedRoot(String url);

    boolean removeExcludedRoot(String url);

    
    String[] getExcludedRootUrls();
  }
}
