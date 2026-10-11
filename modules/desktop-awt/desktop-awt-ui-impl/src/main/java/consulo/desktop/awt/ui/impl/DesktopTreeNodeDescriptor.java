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

import consulo.ui.TextItemPresentation;
import consulo.ui.ex.tree.NodeDescriptor;
import consulo.ui.ex.tree.PresentableNodeDescriptor;
import consulo.ui.ex.tree.PresentationData;
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
    private volatile DesktopTreeRowDecoration myDecoration = DesktopTreeRowDecoration.EMPTY;

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

        DesktopTreeNodePresentation itemPresentation = new DesktopTreeNodePresentation(presentation);
        render.accept(node.getValue(), itemPresentation);

        DesktopTreeRowDecoration decoration = itemPresentation.toDecoration();
        if (!decoration.equals(myDecoration)) {
            myDecoration = decoration;
            presentation.setChanged(true);
        }

        Object[] values = myStructure.computeColumnValues(node.getValue());
        if (!Arrays.equals(values, myColumnValues)) {
            myColumnValues = values;
            presentation.setChanged(true);
        }
    }

    DesktopTreeRowDecoration getDecoration() {
        return myDecoration;
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
