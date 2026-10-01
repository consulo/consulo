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
package consulo.diagram.impl.internal.editor;

import consulo.diagram.DiagramNodesGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.graph.GraphGroup;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public final class DiagramGraphGroup implements GraphGroup {
    private final DiagramNodesGroup myGroup;
    private final LocalizeValue myName;

    public DiagramGraphGroup(DiagramNodesGroup group) {
        myGroup = group;
        myName = LocalizeValue.of(group.getGroupName());
    }

    @Override
    public LocalizeValue getName() {
        return myName;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof DiagramGraphGroup that && myGroup == that.myGroup;
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(myGroup);
    }
}
