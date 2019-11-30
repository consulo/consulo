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

package com.intellij.util.diff;

import com.intellij.openapi.util.Ref;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author max
 */
public interface FlyweightCapableTreeStructure<T> {
  @Nonnull
  T getRoot();

  @Nullable
  T getParent(@Nonnull T node);

  @Nonnull
  T prepareForGetChildren(@Nonnull T node);

  int getChildren(@Nonnull T parent, @Nonnull Ref<T[]> into);

  void disposeChildren(T[] nodes, int count);

  @Nonnull
  CharSequence toString(@Nonnull T node);

  int getStartOffset(@Nonnull T node);
  int getEndOffset(@Nonnull T node);
}
