/*
 * Copyright 2000-2009 JetBrains s.r.o.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.intellij.uml;

import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public abstract class UmlNodeBase<T> implements UmlNode<T> {
  private String qualifiedName = null;
  private int hashCode = 0;
  private final UmlProvider<T> myProvider;
  public UmlNodeBase(@NotNull UmlProvider<T> provider) {
    myProvider = provider;
  }

  protected @NotNull final UmlProvider<T> getUmlProvider() {
    return myProvider;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    final UmlNodeBase<?> that = (UmlNodeBase<?>)o;
    final String thatFQN = that.getFQN();
    final String fqn = getFQN();
    return fqn != null && fqn.length() > 0 && fqn.equals(thatFQN);
  }

  @Override
  public int hashCode() {
    return hashCode == 0 ? hashCode = getFQN().hashCode() : hashCode;
  }

  protected String getFQN() {
    if (qualifiedName == null) {
      qualifiedName = getUmlProvider().getVfsResolver().getQualifiedName(getIdentifyingElement());
    }
    return qualifiedName;
  }
}
