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
package consulo.diagram;

import consulo.language.psi.PsiElement;

/**
 * @author Konstantin Bulenkov
 */
public class PsiDiagramEdge<T extends PsiElement> implements DiagramEdge<T> {
    private final DiagramNode<T> mySource;
    private final DiagramNode<T> myTarget;
    private final String myName;
    private final T myIdentifyingElement;
    private DiagramRelationshipInfo myRelationship;

    public PsiDiagramEdge(DiagramNode<T> source,
                          DiagramNode<T> target,
                          String name,
                          T identifyingElement,
                          DiagramRelationshipInfo relationship) {
        mySource = source;
        myTarget = target;
        myName = name;
        myIdentifyingElement = identifyingElement;
        myRelationship = relationship;
    }

    public PsiDiagramEdge(DiagramNode<T> from, DiagramNode<T> to, DiagramRelationshipInfo relationship) {
        this(from, to, "", from.getIdentifyingElement(), relationship);
    }

    public PsiDiagramEdge(DiagramNode<T> source, DiagramNode<T> target, String name, T identifyingElement) {
        this(source, target, name, identifyingElement, DiagramRelationshipInfo.NO_RELATIONSHIP);
    }

    @Override
    public DiagramNode<T> getSource() {
        return mySource;
    }

    @Override
    public DiagramNode<T> getTarget() {
        return myTarget;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public T getIdentifyingElement() {
        return myIdentifyingElement;
    }

    public void setRelationship(DiagramRelationshipInfo relationship) {
        myRelationship = relationship;
    }

    @Override
    public DiagramRelationshipInfo getRelationship() {
        return myRelationship;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PsiDiagramEdge<?> that = (PsiDiagramEdge<?>) o;
        return myIdentifyingElement.equals(that.myIdentifyingElement)
            && myName.equals(that.myName)
            && myRelationship.equals(that.myRelationship)
            && mySource.equals(that.mySource)
            && myTarget.equals(that.myTarget);
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
