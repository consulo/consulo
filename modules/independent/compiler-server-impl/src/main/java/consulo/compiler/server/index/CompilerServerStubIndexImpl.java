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
package consulo.compiler.server.index;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.StubIndex;
import com.intellij.psi.stubs.StubIndexKey;
import com.intellij.util.Processor;
import com.intellij.util.indexing.IdIterator;
import javax.annotation.Nonnull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author VISTALL
 * @since 12:14/14.08.13
 */
public class CompilerServerStubIndexImpl extends StubIndex {
  @Nonnull
  @Override
  public <Key, Psi extends PsiElement> Collection<Psi> get(@Nonnull StubIndexKey<Key, Psi> indexKey, @Nonnull Key key, @Nonnull Project project, GlobalSearchScope scope) {
    return Collections.emptyList();
  }

  @Override
  public <Key, Psi extends PsiElement> boolean processElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                            @Nonnull Key key,
                                            @Nonnull Project project,
                                            GlobalSearchScope scope,
                                            Class<Psi> requiredClass,
                                            @Nonnull Processor<? super Psi> processor) {
    return true;
  }

  @Nonnull
  @Override
  public <Key> Collection<Key> getAllKeys(@Nonnull StubIndexKey<Key, ?> indexKey, @Nonnull Project project) {
    return Collections.emptyList();
  }

  @Override
  public <K> boolean processAllKeys(@Nonnull StubIndexKey<K, ?> indexKey, @Nonnull Project project, Processor<K> processor) {
    return true;
  }

  @Nonnull
  @Override
  public <Key> IdIterator getContainingIds(@Nonnull StubIndexKey<Key, ?> indexKey,
                                           @Nonnull Key dataKey,
                                           @Nonnull Project project,
                                           @Nonnull GlobalSearchScope scope) {
    return new IdIterator() {
      @Override
      public boolean hasNext() {
        return false;
      }

      @Override
      public int next() {
        return 0;
      }

      @Override
      public int size() {
        return 0;
      }
    };
  }

  @Override
  public void forceRebuild(@Nonnull Throwable e) {

  }
}
