/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.compiler.impl.internal;

import consulo.compiler.CacheCorruptedException;
import consulo.compiler.CompileContextEx;
import consulo.compiler.DependencyCache;
import consulo.compiler.ExitException;
import consulo.compiler.localize.CompilerLocalize;
import consulo.localize.LocalizeValue;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.SimpleReference;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * @author Eugene Zhuravlev
 * @since 2003-08-18
 */
public class CacheUtils {
    public static Collection<VirtualFile> findDependentFiles(
        CompileContextEx context,
        Set<VirtualFile> compiledWithErrors,
        @Nullable Function<Pair<int[], Set<VirtualFile>>, Pair<int[], Set<VirtualFile>>> filter
    ) throws CacheCorruptedException, ExitException {
        context.getProgressIndicator().setTextValue(CompilerLocalize.progressCheckingDependencies());

        DependencyCache dependencyCache = context.getDependencyCache();
        Set<VirtualFile> dependentFiles = new HashSet<>();

        dependencyCache.findDependentFiles(context, new SimpleReference<>(), filter, dependentFiles, compiledWithErrors);

        context.getProgressIndicator().setTextValue(
            dependentFiles.size() > 0 ? CompilerLocalize.progressFoundDependentFiles(dependentFiles.size()) : LocalizeValue.empty()
        );

        return dependentFiles;
    }
}
