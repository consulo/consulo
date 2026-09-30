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

import org.jspecify.annotations.Nullable;

/**
 * @author Konstantin Bulenkov
 */
public abstract class DiagramNodeBase<T> implements DiagramNode<T> {
    private @Nullable String myQualifiedName;
    private int myHashCode;
    private final DiagramProvider<T> myProvider;

    public DiagramNodeBase(DiagramProvider<T> provider) {
        myProvider = provider;
    }

    protected final DiagramProvider<T> getDiagramProvider() {
        return myProvider;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DiagramNodeBase<?> that = (DiagramNodeBase<?>) o;
        String fqn = getFQN();
        return !fqn.isEmpty() && fqn.equals(that.getFQN());
    }

    @Override
    public int hashCode() {
        if (myHashCode == 0) {
            myHashCode = getFQN().hashCode();
        }
        return myHashCode;
    }

    protected String getFQN() {
        String qualifiedName = myQualifiedName;
        if (qualifiedName == null) {
            qualifiedName = getDiagramProvider().getVfsResolver().getQualifiedName(getIdentifyingElement());
            myQualifiedName = qualifiedName;
        }
        return qualifiedName;
    }
}
