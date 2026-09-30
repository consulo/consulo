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

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

/**
 * @author Konstantin Bulenkov
 */
public class PsiUmlEdge<T extends PsiElement>  implements UmlEdge<T> {
  private UmlNode<T> mySource;
  private UmlNode<T> myTarget;
  private String myName;
  private T myIdentifyingElement;
  private UmlRelationshipInfo myRelationship;

  public PsiUmlEdge(@NotNull UmlNode<T> source, @NotNull UmlNode<T> target, String name,
                           @NotNull T identifyingElement,
                           @NotNull UmlRelationshipInfo relationship) {
    mySource = source;
    myTarget = target;
    myName = name;
    myIdentifyingElement = identifyingElement;
    myRelationship = relationship;
  }

  public PsiUmlEdge(UmlNode<T> from, UmlNode<T> to, @NotNull UmlRelationshipInfo relationship) {
    this(from, to, "", from.getIdentifyingElement(), relationship);
  }

  public PsiUmlEdge(final UmlNode<T> source, final UmlNode<T> target,
                    final String name, final @NotNull T identifyingElement) {
    this(source, target, name, identifyingElement, UmlRelationshipInfo.NO_RELATIONSHIP);
  }

  public UmlNode<T> getSource() {
    return mySource;
  }

  public UmlNode<T> getTarget() {
    return myTarget;
  }

  public String getName() {
    return myName;
  }

  @NotNull
  public T getIdentifyingElement() {
    return myIdentifyingElement;
  }

  public void setRelationship(UmlRelationshipInfo relationship) {
    myRelationship = relationship;
  }

  @NotNull
  public UmlRelationshipInfo getRelationship() {
    return myRelationship;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PsiUmlEdge that = (PsiUmlEdge)o;

    if (!myIdentifyingElement.equals(that.myIdentifyingElement)) return false;
    if (!myName.equals(that.myName)) return false;
    if (!myRelationship.equals(that.myRelationship)) return false;
    if (!mySource.equals(that.mySource)) return false;
    if (!myTarget.equals(that.myTarget)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = mySource.hashCode();
    result = 31 * result + myTarget.hashCode();
    result = 31 * result + myName.hashCode();
    result = 31 * result + myIdentifyingElement.hashCode();
    result = 31 * result + myRelationship.hashCode();
    return result;
  }

}

