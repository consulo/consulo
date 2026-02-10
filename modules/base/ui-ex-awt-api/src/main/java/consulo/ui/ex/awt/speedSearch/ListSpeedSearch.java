/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.awt.speedSearch;

import consulo.ui.ex.awt.ScrollingUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.util.function.Function;

public class ListSpeedSearch<T> extends SpeedSearchBase<JList<T>> {
    @Nonnull
    public static <T> ListSpeedSearch<T> installOn(@Nonnull JList<T> list) {
        return installOn(list, null);
    }

    @Nonnull
    public static <T> ListSpeedSearch<T> installOn(@Nonnull JList<T> list, @Nullable Function<? super T, String> convertor) {
        ListSpeedSearch<T> search = new ListSpeedSearch<>(list, null, convertor);
        search.setupListeners();
        return search;
    }

    private final Function<? super T, String> myToStringConverter;

    public ListSpeedSearch(JList<T> component, @SuppressWarnings("unused") Void sig, Function<? super T, String> convertor) {
        super(component, sig);
        myToStringConverter = convertor;
    }

    @Deprecated
    public ListSpeedSearch(JList<T> list) {
        super(list);
        myToStringConverter = null;
    }

    @Deprecated
    public ListSpeedSearch(JList<T> component, Function<? super T, String> convertor) {
        super(component);
        myToStringConverter = convertor;
    }

    @Override
    protected void selectElement(Object element, String selectedText) {
        if (element != null) {
            //noinspection unchecked
            ScrollingUtil.selectItem(myComponent, (T) element);
        }
        else {
            myComponent.clearSelection();
        }
    }

    @Override
    protected int getSelectedIndex() {
        return myComponent.getSelectedIndex();
    }

    @Nonnull
    @Override
    protected Object[] getAllElements() {
        return getAllListElements(myComponent);
    }

    public static Object[] getAllListElements(JList list) {
        ListModel model = list.getModel();
        if (model instanceof DefaultListModel) { // optimization
            return ((DefaultListModel) model).toArray();
        }
        else {
            Object[] elements = new Object[model.getSize()];
            for (int i = 0; i < elements.length; i++) {
                elements[i] = model.getElementAt(i);
            }
            return elements;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected String getElementText(Object element) {
        if (myToStringConverter != null) {
            return myToStringConverter.apply((T) element);
        }
        return element == null ? null : element.toString();
    }
}