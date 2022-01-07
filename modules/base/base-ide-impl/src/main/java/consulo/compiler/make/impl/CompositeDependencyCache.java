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
package consulo.compiler.make.impl;

import com.intellij.compiler.impl.ExitException;
import com.intellij.compiler.make.CacheCorruptedException;
import consulo.compiler.make.DependencyCache;
import consulo.compiler.make.DependencyCacheEP;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 14:45/20.10.13
 */
public class CompositeDependencyCache implements DependencyCache {
  private final DependencyCache[] myDependencyCaches;

  public CompositeDependencyCache(Project project, String cacheDir) {
    List<DependencyCacheEP> extensions = EP_NAME.getExtensionList();
    List<DependencyCache> list = new ArrayList<>(extensions.size());
    for (DependencyCacheEP extension : extensions) {
      list.add(extension.create(project, cacheDir));
    }
    myDependencyCaches = list.toArray(new DependencyCache[list.size()]);
  }

  @Override
  public void findDependentFiles(CompileContextEx context,
                                 Ref<CacheCorruptedException> exceptionRef,
                                 Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> filter,
                                 Set<VirtualFile> dependentFiles,
                                 Set<VirtualFile> compiledWithErrors) throws CacheCorruptedException, ExitException {
    for (DependencyCache dependencyCache : myDependencyCaches) {
      dependencyCache.findDependentFiles(context, exceptionRef, filter, dependentFiles, compiledWithErrors);

      CacheCorruptedException exception = exceptionRef.get();
      if (exception != null) {
        throw exception;
      }
    }
  }

  @Override
  public boolean hasUnprocessedTraverseRoots() {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      if(ourDependencyExtension.hasUnprocessedTraverseRoots()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void resetState() {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      ourDependencyExtension.resetState();
    }
  }

  @Override
  public void clearTraverseRoots() {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      ourDependencyExtension.clearTraverseRoots();
    }
  }

  @Override
  public void update() throws CacheCorruptedException {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      ourDependencyExtension.update();
    }
  }

  @Nullable
  @Override
  public String relativePathToQName(@Nonnull String path, char separator) {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      String s = ourDependencyExtension.relativePathToQName(path, separator);
      if(s != null) {
        return s;
      }
    }
    return null;
  }

  @Override
  public void syncOutDir(Trinity<File, String, Boolean> trinity) throws CacheCorruptedException {
    for (DependencyCache ourDependencyExtension : myDependencyCaches) {
      ourDependencyExtension.syncOutDir(trinity);
    }
  }

  @Nonnull
  public <T extends DependencyCache> T findChild(Class<T> clazz) {
    for (DependencyCache dependencyCach : myDependencyCaches) {
      if(dependencyCach.getClass() == clazz) {
        return (T) dependencyCach;
      }
    }
    throw new IllegalArgumentException("Child is not found for class: " + clazz.getName());
  }
}
