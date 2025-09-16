/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ide.errorTreeView;

import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.ClickableTreeCellRenderer;
import consulo.ide.impl.idea.openapi.vcs.changes.issueLinks.TreeNodePartListener;
import consulo.ide.impl.idea.ui.CustomizeColoredTreeCellRenderer;
import consulo.ide.impl.idea.ui.MultilineTreeCellRenderer;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;

public class NewErrorTreeRenderer extends MultilineTreeCellRenderer {
    private final MyWrapperRenderer myWrapperRenderer;
    private final CallingBackColoredTreeCellRenderer myColoredTreeCellRenderer;
    private final MyNotSelectedColoredTreeCellRenderer myRightCellRenderer;

    private NewErrorTreeRenderer() {
        myColoredTreeCellRenderer = new CallingBackColoredTreeCellRenderer();
        myRightCellRenderer = new MyNotSelectedColoredTreeCellRenderer();
        myWrapperRenderer = new MyWrapperRenderer(myColoredTreeCellRenderer, myRightCellRenderer);
    }

    public static JScrollPane install(JTree tree) {
        NewErrorTreeRenderer renderer = new NewErrorTreeRenderer();
        //new TreeLinkMouseListener(renderer.myColoredTreeCellRenderer).install(tree);
        new TreeNodePartListener(renderer.myRightCellRenderer).installOn(tree);
        return MultilineTreeCellRenderer.installRenderer(tree, renderer);
    }

    @Override
    public Component getTreeCellRendererComponent(
        JTree tree,
        Object value,
        boolean selected,
        boolean expanded,
        boolean leaf,
        int row,
        boolean hasFocus
    ) {
        ErrorTreeElement element = getElement(value);
        if (element != null) {
            CustomizeColoredTreeCellRenderer leftSelfRenderer = element.getLeftSelfRenderer();
            CustomizeColoredTreeCellRenderer rightSelfRenderer = element.getRightSelfRenderer();
            if (leftSelfRenderer != null || rightSelfRenderer != null) {
                myColoredTreeCellRenderer.setCurrentCallback(leftSelfRenderer);
                myRightCellRenderer.setCurrentCallback(rightSelfRenderer);
                return myWrapperRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }
        }
        return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
    }

    private static class MyNotSelectedColoredTreeCellRenderer extends SimpleColoredComponent implements ClickableTreeCellRenderer {
        private CustomizeColoredTreeCellRenderer myCurrentCallback;

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            if (myCurrentCallback instanceof CustomizeColoredTreeCellRendererReplacement cellRendererReplacement) {
                return cellRendererReplacement.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            }

            clear();
            setBackground(UIUtil.getBgFillColor(tree));

            if (myCurrentCallback != null) {
                myCurrentCallback.customizeCellRenderer(this, tree, value, selected, expanded, leaf, row, hasFocus);
            }

            if (getFont() == null) {
                setFont(tree.getFont());
            }
            return this;
        }

        @Nullable
        @Override
        public Object getTag() {
            return myCurrentCallback == null ? null : myCurrentCallback.getTag();
        }

        public void setCurrentCallback(CustomizeColoredTreeCellRenderer currentCallback) {
            myCurrentCallback = currentCallback;
        }
    }

    private static class MyWrapperRenderer implements TreeCellRenderer {
        private final TreeCellRenderer myLeft;
        private final TreeCellRenderer myRight;
        private final JPanel myPanel;

        public TreeCellRenderer getLeft() {
            return myLeft;
        }

        public TreeCellRenderer getRight() {
            return myRight;
        }

        public MyWrapperRenderer(TreeCellRenderer left, TreeCellRenderer right) {
            myLeft = left;
            myRight = right;

            myPanel = new JPanel(new BorderLayout());
        }

        @Override
        public Component getTreeCellRendererComponent(
            JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus
        ) {
            myPanel.removeAll();
            myPanel.setBackground(tree.getBackground());
            myPanel.add(myLeft.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus), BorderLayout.WEST);
            myPanel.add(myRight.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus), BorderLayout.EAST);
            return myPanel;
        }
    }

    @Nonnull
    public static String calcPrefix(@Nullable ErrorTreeElement element) {
        if (element instanceof SimpleMessageElement || element instanceof NavigatableMessageElement) {
            String prefix = element.getKind().getPresentableText();

            if (element instanceof NavigatableMessageElement navigatableMessageElement) {
                String rendPrefix = navigatableMessageElement.getRendererTextPrefix();
                if (!StringUtil.isEmpty(rendPrefix)) {
                    prefix += rendPrefix + " ";
                }
            }

            return prefix;
        }
        return "";
    }

    @Override
    protected void initComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        ErrorTreeElement element = getElement(value);
        if (element instanceof GroupingElement) {
            setFont(getFont().deriveFont(Font.BOLD));
        }

        String prefix = calcPrefix(element);
        if (element != null) {
            String[] text = element.getText();
            if (text == null) {
                text = ArrayUtil.EMPTY_STRING_ARRAY;
            }
            if (text.length > 0 && text[0] == null) {
                text[0] = "";
            }
            setText(text, prefix);
        }

        Image icon = null;

        if (element instanceof GroupingElement groupingElement) {
            icon = groupingElement.getFile() != null
                ? groupingElement.getFile().getFileType().getIcon()
                : PlatformIconGroup.filetypesText();
        }
        else if (element instanceof SimpleMessageElement || element instanceof NavigatableMessageElement) {
            icon = switch (element.getKind()) {
                case ERROR -> PlatformIconGroup.generalError();
                case WARNING, NOTE -> PlatformIconGroup.generalWarning();
                case INFO -> PlatformIconGroup.generalInformation();
                default -> null;
            };
        }

        setIcon(TargetAWT.to(icon));
    }

    private static ErrorTreeElement getElement(Object value) {
        return value instanceof DefaultMutableTreeNode node
            && node.getUserObject() instanceof ErrorTreeNodeDescriptor errorTreeNodeDescr ? errorTreeNodeDescr.getElement() : null;
    }
}

