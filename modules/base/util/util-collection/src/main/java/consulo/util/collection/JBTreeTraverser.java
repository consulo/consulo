/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.util.collection;

import consulo.util.lang.function.Functions;

import jakarta.annotation.Nonnull;
import java.util.function.Function;

public class JBTreeTraverser<T> extends FilteredTraverserBase<T, JBTreeTraverser<T>> {

  @Nonnull
  public static <T> JBTreeTraverser<T> from(@Nonnull Function<T, ? extends Iterable<? extends T>> treeStructure) {
    return new JBTreeTraverser<T>(treeStructure);
  }

  @Nonnull
  public static <T> JBTreeTraverser<T> of(@Nonnull Function<T, T[]> treeStructure) {
    return new JBTreeTraverser<T>(Functions.compose(treeStructure, Functions.<T>wrapArray()));
  }

  public JBTreeTraverser(Function<T, ? extends Iterable<? extends T>> treeStructure) {
    super(null, treeStructure);
  }

  protected JBTreeTraverser(Meta<T> meta, Function<T, ? extends Iterable<? extends T>> treeStructure) {
    super(meta, treeStructure);
  }

  @Nonnull
  @Override
  protected JBTreeTraverser<T> newInstance(Meta<T> meta) {
    return new JBTreeTraverser<T>(meta, getTree());
  }
}