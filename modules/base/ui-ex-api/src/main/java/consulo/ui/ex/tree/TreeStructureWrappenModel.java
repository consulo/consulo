/*
 * Copyright 2013-2017 consulo.io
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
package consulo.ui.ex.tree;

import consulo.application.ReadAction;
import consulo.application.util.function.ThrowableComputable;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Function;

/**
 * @author VISTALL
 * @since 16-Sep-17
 */
public class TreeStructureWrappenModel<T> implements TreeModel<T> {
    private AbstractTreeStructure myStructure;

    public TreeStructureWrappenModel(AbstractTreeStructure structure) {
        myStructure = structure;
    }

    public @Nullable T getRootElement() {
        return (T) myStructure.getRootElement();
    }

    @Override
    public boolean isNeedBuildChildrenBeforeOpen(TreeNode<T> node) {
        return myStructure.isToBuildChildrenInBackground(node.getValue());
    }

    @Override
    @SuppressWarnings("unchecked")
    public void buildChildren(Function<T, TreeNode<T>> nodeFactory, @Nullable T parentValue) {
        ThrowableComputable<Object[], RuntimeException> action = () -> myStructure.getChildElements(parentValue);

        for (Object o : ReadAction.compute(action)) {
            T element = (T) o;
            TreeNode<T> apply = nodeFactory.apply(element);

            apply.setLeaf(o instanceof consulo.ui.ex.tree.TreeNode && !((consulo.ui.ex.tree.TreeNode) o).isAlwaysShowPlus());

            apply.setRenderer((fileElement, itemPresentation) -> {
                NodeDescriptor descriptor = myStructure.createDescriptor(element, null);

                descriptor.update();

                if (descriptor instanceof PresentableNodeDescriptor<?> presentable) {
                    renderPresentation(itemPresentation, presentable, descriptor);
                }
                else {
                    itemPresentation.append(descriptor.toString());
                }

                try {
                    ReadAction.compute(() -> itemPresentation.withIcon(descriptor.getIcon()));
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    /**
     * The descriptor carries everything the awt renderer paints - coloured fragments and the file status
     * foreground - and flattening it to {@code toString()} lost all of it. The forced colour wins only where
     * a fragment has no colour of its own, so the grey location text stays grey while the name takes the
     * status colour.
     */
    private static void renderPresentation(
        TextItemPresentation itemPresentation,
        PresentableNodeDescriptor<?> presentable,
        NodeDescriptor descriptor
    ) {
        PresentationData presentation = presentable.getPresentation();
        ColorValue forced = presentation.getForcedTextForeground();

        List<PresentableNodeDescriptor.ColoredFragment> fragments = presentation.getColoredText();
        if (fragments.isEmpty()) {
            itemPresentation.append(LocalizeValue.ofNullable(descriptor.toString()), toTextAttribute(null, forced));
            return;
        }

        for (PresentableNodeDescriptor.ColoredFragment fragment : fragments) {
            itemPresentation.append(fragment.getText(), toTextAttribute(fragment.getAttributes(), forced));
        }
    }

    private static TextAttribute toTextAttribute(@Nullable SimpleTextAttributes attributes, @Nullable ColorValue forced) {
        int style = 0;
        ColorValue foreground = null;

        if (attributes != null) {
            if ((attributes.getStyle() & SimpleTextAttributes.STYLE_BOLD) != 0) {
                style |= TextAttribute.STYLE_BOLD;
            }
            if ((attributes.getStyle() & SimpleTextAttributes.STYLE_ITALIC) != 0) {
                style |= TextAttribute.STYLE_ITALIC;
            }
            if (attributes.getFgColor() != null) {
                foreground = TargetAWT.from(attributes.getFgColor());
            }
        }

        if (foreground == null) {
            foreground = forced;
        }

        return new TextAttribute(style, foreground);
    }
}
