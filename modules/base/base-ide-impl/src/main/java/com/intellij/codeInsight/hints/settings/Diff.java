/*
 * Copyright 2013-2017 consulo.io
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
package com.intellij.codeInsight.hints.settings;

import javax.annotation.Nonnull;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * from kotlin
 */
public class Diff {
  @Nonnull
  public static Diff build(Set<String> base, Set<String> updated) {
    Set<String> removed = new LinkedHashSet<>(base);
    removed.removeAll(updated);

    Set<String> added = new LinkedHashSet<>(updated);
    added.removeAll(base);

    return new Diff(added, removed);
  }

  private final Set<String> myAdded;
  private final Set<String> myRemoved;

  public Diff(@Nonnull Set<String> added, @Nonnull Set<String> removed) {
    this.myAdded = added;
    this.myRemoved = removed;
  }

  public Set<String> getAdded() {
    return myAdded;
  }

  public Set<String> getRemoved() {
    return myRemoved;
  }

  @Nonnull
  public Set<String> applyOn(@Nonnull Set<String> base) {
    Set<String> baseSet = new LinkedHashSet<>(base);
    baseSet.addAll(myAdded);
    baseSet.removeAll(myRemoved);
    return baseSet;
  }
}
