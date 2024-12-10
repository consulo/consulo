/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.debug.ui;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueNode;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2024-12-09
 */
public interface XValueTree {
    Key<XValueTree> KEY = Key.of(XValueTree.class);

    void expand(@Nonnull XValueNode node);

    @Nonnull
    List<XValueNode> getSelectedNodes();

    @Nullable
    XValueNode getSelectedNode();

    @Nullable
    XDebugSession getSession();

    @Nonnull
    Project getProject();

    @Nullable
    default XValue getSelectedValue() {
        XValueNode node = getSelectedNode();
        return node != null ? node.getValueContainer() : null;
    }
}
