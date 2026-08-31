/*
 * Copyright 2013-2023 consulo.io
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

import consulo.util.collection.primitive.ints.IntSet;
import consulo.util.collection.primitive.ints.IntSets;
import consulo.util.lang.Pair;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

class DependentClassesCumulativeFilter implements Function<Pair<int[], Set<Path>>, Pair<int[], Set<Path>>> {
    private final IntSet myProcessedNames = IntSets.newHashSet();
    private final Set<Path> myProcessedFiles = new HashSet<>();

    @Override
    public Pair<int[], Set<Path>> apply(Pair<int[], Set<Path>> deps) {
        IntSet currentDeps = IntSets.newHashSet(deps.getFirst());
        currentDeps.removeAll(myProcessedNames.toArray());
        myProcessedNames.addAll(deps.getFirst());

        Set<Path> depFiles = new HashSet<>(deps.getSecond());
        depFiles.removeAll(myProcessedFiles);
        myProcessedFiles.addAll(deps.getSecond());
        return Pair.create(currentDeps.toArray(), depFiles);
    }
}
