/*
 * Copyright 2013-2016 consulo.io
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
package consulo.compiler.make;

import com.intellij.compiler.impl.ExitException;
import com.intellij.compiler.make.CacheCorruptedException;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.Set;

/**
 * @author VISTALL
 * @since 23:35/25.05.13
 */
public interface DependencyCache {

  ExtensionPointName<DependencyCacheEP> EP_NAME = ExtensionPointName.create("com.intellij.compiler.dependencyCache");

  void findDependentFiles(CompileContextEx context,
                          Ref<CacheCorruptedException> exceptionRef,
                          Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> filter,
                          Set<VirtualFile> dependentFiles, Set<VirtualFile> compiledWithErrors) throws CacheCorruptedException,
                                                                                                       ExitException;

  boolean hasUnprocessedTraverseRoots();

  void resetState();

  void clearTraverseRoots();

  void update() throws CacheCorruptedException;

  @Nullable
  String relativePathToQName(@Nonnull String path, char separator);

  void syncOutDir(Trinity<File, String, Boolean> trinity) throws CacheCorruptedException;
}
