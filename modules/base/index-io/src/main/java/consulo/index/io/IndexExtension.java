/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.index.io;

import consulo.index.io.DataIndexer;
import consulo.index.io.IndexId;
import consulo.index.io.KeyDescriptor;
import consulo.index.io.data.DataExternalizer;

import jakarta.annotation.Nonnull;

/**
 * @author Eugene Zhuravlev
 * V class MUST have equals / hashcode properly defined!!!
 */
public abstract class IndexExtension<K, V, I> {
  @Nonnull
  public abstract IndexId<K, V> getName();

  @Nonnull
  public abstract DataIndexer<K, V, I> getIndexer();

  @Nonnull
  public abstract KeyDescriptor<K> getKeyDescriptor();

  @Nonnull
  public abstract DataExternalizer<V> getValueExternalizer();

  public abstract int getVersion();
}
