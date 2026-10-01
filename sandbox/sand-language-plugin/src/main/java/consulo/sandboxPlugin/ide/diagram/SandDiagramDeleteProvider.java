/*
 * Copyright 2013-2026 consulo.io
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
package consulo.sandboxPlugin.ide.diagram;

import consulo.diagram.DiagramDeleteProvider;
import consulo.diagram.DiagramEdge;
import consulo.diagram.DiagramNode;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public class SandDiagramDeleteProvider extends DiagramDeleteProvider<String> {
    private final Set<String> myDeleted = ConcurrentHashMap.newKeySet();

    public boolean isDeleted(String className) {
        return myDeleted.contains(className);
    }

    @Override
    public boolean canDeleteNode(DiagramNode<String> node) {
        return !"Object".equals(node.getIdentifyingElement());
    }

    @Override
    public boolean canDeleteEdge(DiagramEdge<String> edge) {
        return false;
    }

    @Override
    public boolean deleteNode(DiagramNode<String> node) {
        return myDeleted.add(node.getIdentifyingElement());
    }

    @Override
    public boolean deleteEdge(DiagramEdge<String> edge) {
        return false;
    }
}
