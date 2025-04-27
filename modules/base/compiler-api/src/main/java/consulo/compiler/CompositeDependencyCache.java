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
package consulo.compiler;

import consulo.project.Project;
import consulo.util.lang.Pair;
import consulo.util.lang.Trinity;
import consulo.util.lang.ref.Ref;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 14:45/20.10.13
 */
public class CompositeDependencyCache implements DependencyCache {
    private final List<DependencyCache> myDependencyCaches = new ArrayList<>();

    public CompositeDependencyCache(Project project, String cacheDir) {
        project.getExtensionPoint(DependencyCacheFactory.class)
            .forEachExtensionSafe(factory -> myDependencyCaches.add(factory.create(cacheDir)));
    }

    @Override
    public void findDependentFiles(
        CompileContext context,
        Ref<CacheCorruptedException> exceptionRef,
        Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> filter,
        Set<VirtualFile> dependentFiles,
        Set<VirtualFile> compiledWithErrors
    ) throws CacheCorruptedException, ExitException {
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
            if (ourDependencyExtension.hasUnprocessedTraverseRoots()) {
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
            if (s != null) {
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
            if (dependencyCach.getClass() == clazz) {
                return (T)dependencyCach;
            }
        }
        throw new IllegalArgumentException("Child is not found for class: " + clazz.getName());
    }
}
