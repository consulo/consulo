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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.facade.DesktopAWTTargetAWTImpl;
import consulo.localize.LocalizeValue;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
class DesktopTreeNodeDescriptor<K> extends PresentableNodeDescriptor<Object> {
    private final DesktopTreeStructure<K> myStructure;
    private final Object myElement;

    private volatile @Nullable Object @Nullable [] myColumnValues;

    @SuppressWarnings("rawtypes")
    DesktopTreeNodeDescriptor(DesktopTreeStructure<K> structure, Object element, @Nullable NodeDescriptor parentDescriptor) {
        super(parentDescriptor);
        myStructure = structure;
        myElement = element;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void update(PresentationData presentation) {
        if (myElement == myStructure.getRootNode()) {
            return;
        }

        DesktopTreeNodeImpl<K> node = (DesktopTreeNodeImpl<K>) myElement;

        BiConsumer<K, TextItemPresentation> render = node.getRenderer();

        render.accept(node.getValue(), new TextItemPresentation() {
            @Override
            public void clearText() {
                presentation.clearText();
            }

            @Override
            public TextItemPresentation withIcon(@Nullable Image image) {
                presentation.setIcon(image);
                return this;
            }

            @Override
            public void append(LocalizeValue text, TextAttribute textAttribute) {
                presentation.addText(text, DesktopAWTTargetAWTImpl.from(textAttribute));
            }
        });

        Object[] values = myStructure.computeColumnValues(node.getValue());
        if (!Arrays.equals(values, myColumnValues)) {
            myColumnValues = values;
            presentation.setChanged(true);
        }
    }

    @Nullable Object getColumnValue(int index) {
        Object[] values = myColumnValues;
        return values == null || index < 0 || index >= values.length ? null : values[index];
    }

    @Override
    public Object getElement() {
        return myElement;
    }
}
