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
package consulo.execution.debug.impl.internal.ui.tree.node;

import consulo.execution.debug.frame.XValueGroup;
import consulo.execution.debug.impl.internal.frame.UnifiedColoredTextContainer;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.ui.ex.SimpleTextAttributes;
import org.jspecify.annotations.Nullable;

/**
 * A group of values - the counterpart of the swing {@link XValueGroupNodeImpl}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedXValueGroupNodeImpl extends UnifiedXValueContainerNode<XValueGroup> {
    public UnifiedXValueGroupNodeImpl(UnifiedXDebuggerTree tree, UnifiedXDebuggerTreeNode parent, XValueGroup group) {
        super(tree, parent, group);
        setLeaf(false);
        setIcon(group.getIcon());

        UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
        text.append(group.getName().get(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        String comment = group.getComment();
        if (comment != null) {
            text.append(" " + comment, SimpleTextAttributes.GRAY_ATTRIBUTES);
        }
        setText(text);
    }

    @Override
    public @Nullable String getRestorableName() {
        return myValueContainer.getName().get();
    }

    @Override
    public String toString() {
        return myValueContainer.getName().get();
    }
}
