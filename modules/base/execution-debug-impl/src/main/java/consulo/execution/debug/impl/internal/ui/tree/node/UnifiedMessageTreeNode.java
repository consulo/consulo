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

import consulo.execution.debug.impl.internal.frame.UnifiedColoredTextContainer;
import consulo.execution.debug.impl.internal.ui.tree.UnifiedXDebuggerTree;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.ui.XDebuggerUIConstants;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * A message in place of children - the counterpart of the swing {@link MessageTreeNode}.
 *
 * @author VISTALL
 * @since 2026-09-24
 */
public class UnifiedMessageTreeNode extends UnifiedXDebuggerTreeNode {
    private UnifiedMessageTreeNode(
        UnifiedXDebuggerTree tree,
        @Nullable UnifiedXDebuggerTreeNode parent,
        LocalizeValue message,
        SimpleTextAttributes attributes,
        @Nullable Image icon
    ) {
        super(tree, parent, true);
        setIcon(icon);

        UnifiedColoredTextContainer text = new UnifiedColoredTextContainer();
        text.append(message.get(), attributes);
        setText(text);
    }

    public static UnifiedMessageTreeNode createEllipsisNode(UnifiedXDebuggerTree tree, UnifiedXDebuggerTreeNode parent, int remaining) {
        LocalizeValue message = remaining == -1
            ? XDebuggerLocalize.nodeTextEllipsis0UnknownMoreNodesDoubleClickToShow()
            : XDebuggerLocalize.nodeTextEllipsis0MoreNodesDoubleClickToShow(remaining);
        return new UnifiedMessageTreeNode(tree, parent, message, SimpleTextAttributes.GRAYED_ATTRIBUTES, null);
    }

    public static UnifiedMessageTreeNode createMessageNode(
        UnifiedXDebuggerTree tree,
        UnifiedXDebuggerTreeNode parent,
        LocalizeValue message,
        @Nullable Image icon,
        SimpleTextAttributes attributes
    ) {
        return new UnifiedMessageTreeNode(tree, parent, message, attributes, icon);
    }

    public static UnifiedMessageTreeNode createLoadingMessage(UnifiedXDebuggerTree tree, UnifiedXDebuggerTreeNode parent) {
        return new UnifiedMessageTreeNode(
            tree,
            parent,
            XDebuggerUIConstants.COLLECTING_DATA_MESSAGE,
            XDebuggerUIConstants.COLLECTING_DATA_HIGHLIGHT_ATTRIBUTES,
            null
        );
    }

    public static UnifiedMessageTreeNode createInfoMessage(UnifiedXDebuggerTree tree, UnifiedXDebuggerTreeNode parent, LocalizeValue message) {
        return new UnifiedMessageTreeNode(tree, parent, message, SimpleTextAttributes.REGULAR_ATTRIBUTES, XDebuggerUIConstants.INFORMATION_MESSAGE_ICON);
    }
}
