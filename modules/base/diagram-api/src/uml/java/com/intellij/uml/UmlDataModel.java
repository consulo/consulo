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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolder;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @author Konstantin Bulenkov
 */
public abstract class UmlDataModel<T> implements UserDataHolder, Disposable {
  public static Key<String> ORIGINAL_ELEMENT_FQN = Key.create("ORIGINAL_ELEMENT_FQN");
  private final THashMap myUserData = new THashMap();
  @NotNull
  public abstract Collection<UmlNode<T>> getNodes();

  @NotNull
  public abstract Collection<UmlEdge<T>> getEdges();

  @NotNull
  public abstract UmlNode<T> getSourceNode(UmlEdge<T> e);

  @NotNull
  public abstract UmlNode<T> getTargetNode(UmlEdge<T> e);

  @NotNull
  public abstract String getNodeName(UmlNode<T> n);

  @NotNull
  public abstract String getEdgeName(UmlEdge<T> e);

  @Nullable
  public abstract UmlEdge<T> createEdge(@NotNull UmlNode<T> from, @NotNull UmlNode<T> to);

  @SuppressWarnings({"unchecked"})
  public <Type> Type getUserData(@NotNull Key<Type> key) {
    return (Type) myUserData.get(key);
  }

  @SuppressWarnings({"unchecked"})
  public <Type> void putUserData(@NotNull Key<Type> key, @Nullable Type value) {
    myUserData.put(key, value);
  }

  public abstract void removeNode(UmlNode<T> node);
  public abstract UmlNode<T> addElement(T element);
  public abstract void removeEdge(UmlEdge<T> edge);
  public abstract boolean hasElement(T element);

  public void collapseNode(UmlNode<T> node) {}
  public void expandNode(UmlNode<T> node) {}
}
