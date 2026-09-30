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

package com.intellij.diagram;

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
public abstract class DiagramDataModel<T> implements UserDataHolder, Disposable {
  public static Key<String> ORIGINAL_ELEMENT_FQN = Key.create("ORIGINAL_ELEMENT_FQN");
  private final THashMap myUserData = new THashMap();
  @NotNull
  public abstract Collection<DiagramNode<T>> getNodes();

  @NotNull
  public abstract Collection<DiagramEdge<T>> getEdges();

  @NotNull
  public abstract DiagramNode<T> getSourceNode(DiagramEdge<T> e);

  @NotNull
  public abstract DiagramNode<T> getTargetNode(DiagramEdge<T> e);

  @NotNull
  public abstract String getNodeName(DiagramNode<T> n);

  @NotNull
  public abstract String getEdgeName(DiagramEdge<T> e);

  @Nullable
  public abstract DiagramEdge<T> createEdge(@NotNull DiagramNode<T> from, @NotNull DiagramNode<T> to);

  @SuppressWarnings({"unchecked"})
  public <Type> Type getUserData(@NotNull Key<Type> key) {
    return (Type) myUserData.get(key);
  }

  @SuppressWarnings({"unchecked"})
  public <Type> void putUserData(@NotNull Key<Type> key, @Nullable Type value) {
    myUserData.put(key, value);
  }

  public abstract void removeNode(DiagramNode<T> node);
  public abstract DiagramNode<T> addElement(T element);
  public abstract void removeEdge(DiagramEdge<T> edge);
  public abstract boolean hasElement(T element);

  public void collapseNode(DiagramNode<T> node) {}
  public void expandNode(DiagramNode<T> node) {}
}
