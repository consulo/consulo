/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.versionControlSystem.distributed.impl.internal.push;

import consulo.ui.ex.awt.LinkMouseListenerBase;
import consulo.ui.ex.awt.tree.CheckboxTree;
import consulo.logging.Logger;

import consulo.versionControlSystem.distributed.ui.awt.PushLogTreeUtil;
import consulo.versionControlSystem.distributed.ui.awt.VcsLinkedTextComponent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseEvent;

public class VcsBranchEditorListener extends LinkMouseListenerBase {
    private static final Logger LOG = Logger.getInstance(VcsBranchEditorListener.class);
    private final CheckboxTree.CheckboxTreeCellRenderer myRenderer;
    private VcsLinkedTextComponent myUnderlined;

    public VcsBranchEditorListener(CheckboxTree.CheckboxTreeCellRenderer renderer) {
        myRenderer = renderer;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Component component = (Component)e.getSource();
        Object tag = getTagAt(e);
        boolean shouldRepaint = false;
        if (myUnderlined != null) {
            myUnderlined.setUnderlined(false);
            myUnderlined = null;
            shouldRepaint = true;
        }
        if (tag instanceof VcsLinkedTextComponent linkedText) {
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            linkedText.setUnderlined(true);
            myUnderlined = linkedText;
            shouldRepaint = true;
        }
        else {
            super.mouseMoved(e);
        }
        if (shouldRepaint) {
            myRenderer.getTextRenderer().getTree().repaint();
        }
    }

    @Nullable
    @Override
    protected Object getTagAt(@Nonnull MouseEvent e) {
        return PushLogTreeImplUtil.getTagAtForRenderer(myRenderer, e);
    }

    @Override
    protected void handleTagClick(@Nullable Object tag, @Nonnull MouseEvent event) {
        if (tag instanceof VcsLinkedTextComponent linkedText) {
            TreePath path = myRenderer.getTextRenderer().getTree().getPathForLocation(event.getX(), event.getY());
            if (path == null) {
                return; //path could not be null if tag not null; see consulo.versionControlSystem.distributed.ui.awt.PushLogTreeUtil.getTagAtForRenderer
            }
            Object node = path.getLastPathComponent();
            if (node == null || (!(node instanceof DefaultMutableTreeNode))) {
                LOG.warn("Incorrect last path component: " + node);
                return;
            }
            linkedText.fireOnClick((DefaultMutableTreeNode)node, event);
        }
        if (tag instanceof Runnable runnable) {
            runnable.run();
        }
    }
}
