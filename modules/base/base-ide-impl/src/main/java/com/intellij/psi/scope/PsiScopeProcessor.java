/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package com.intellij.psi.scope;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface PsiScopeProcessor {
  interface Event {
    Event SET_DECLARATION_HOLDER = new Event() {
    };
  }

  /**
   * @param element candidate element.
   * @param state   current state of resolver.
   * @return false to stop processing.
   */
  boolean execute(@Nonnull PsiElement element, ResolveState state);

  @Nullable
  default <T> T getHint(@Nonnull Key<T> hintKey) {
    return null;
  }

  default void handleEvent(Event event, @Nullable Object associated) {
  }
}
