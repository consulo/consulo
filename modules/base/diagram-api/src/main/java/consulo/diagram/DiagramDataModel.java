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

import consulo.disposer.Disposable;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import java.util.Collection;

/**
 * @author Konstantin Bulenkov
 */
public abstract class DiagramDataModel<T> extends UserDataHolderBase implements Disposable {
    public static final Key<String> ORIGINAL_ELEMENT_FQN = Key.create("ORIGINAL_ELEMENT_FQN");

    public abstract Collection<DiagramNode<T>> getNodes();

    public abstract Collection<DiagramEdge<T>> getEdges();

    public abstract DiagramNode<T> getSourceNode(DiagramEdge<T> edge);

    public abstract DiagramNode<T> getTargetNode(DiagramEdge<T> edge);

    public abstract String getNodeName(DiagramNode<T> node);

    public abstract String getEdgeName(DiagramEdge<T> edge);

    public abstract @Nullable DiagramEdge<T> createEdge(DiagramNode<T> from, DiagramNode<T> to);

    public abstract void removeNode(DiagramNode<T> node);

    public abstract @Nullable DiagramNode<T> addElement(T element);

    public abstract void removeEdge(DiagramEdge<T> edge);

    public abstract boolean hasElement(T element);

    public void collapseNode(DiagramNode<T> node) {
    }

    public void expandNode(DiagramNode<T> node) {
    }
}
