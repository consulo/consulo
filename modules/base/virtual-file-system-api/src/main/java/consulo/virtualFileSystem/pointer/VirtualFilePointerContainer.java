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
package consulo.virtualFileSystem.pointer;

import consulo.util.xml.serializer.InvalidDataException;
import consulo.disposer.Disposable;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jdom.Element;

import org.jspecify.annotations.Nullable;
import java.util.List;

/**
 * @author dsl
 */
public interface VirtualFilePointerContainer {
  void killAll();

  void add(VirtualFile file);

  void add(String url);

  void remove(VirtualFilePointer pointer);

  
  List<VirtualFilePointer> getList();

  void addAll(VirtualFilePointerContainer that);

  
  String[] getUrls();

  boolean isEmpty();

  
  VirtualFile[] getFiles();

  
  VirtualFile[] getDirectories();

  @Nullable VirtualFilePointer findByUrl(String url);

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
  void readExternal(Element rootChild, String childElementName, boolean externalizeJarDirectories) throws InvalidDataException;

  void writeExternal(Element element, String childElementName, boolean externalizeJarDirectories);

  void moveUp(String url);

  void moveDown(String url);

  
  VirtualFilePointerContainer clone(Disposable parent);

  
  VirtualFilePointerContainer clone(Disposable parent, @Nullable VirtualFilePointerListener listener);

  /**
   * Adds {@code directory} as a root of jar files.
   * After this call the {@link #getFiles()} will additionally return jar files in this directory
   * (and, if {@code recursively} was set, the jar files in all-subdirectories).
   * {@link #getUrls()} will additionally return the {@code directoryUrl}.
   */
  void addJarDirectory(String directoryUrl, boolean recursively);

  /**
   * Removes {@code directory} from the roots of jar files.
   * After that the {@link #getFiles()} and {@link #getUrls()} etc will not return jar files in this directory anymore.
   *
   * @return true if removed
   */
  boolean removeJarDirectory(String directoryUrl);

  /**
   * Returns list of (directory url, isRecursive) which were added via {@link #addJarDirectory(String, boolean)} }
   */
  
  List<Pair<String, Boolean>> getJarDirectories();
}
