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
package consulo.ui;

import consulo.ui.internal.UIInternal;
import consulo.ui.model.FlatDataModel;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author VISTALL
 * @since 2017-09-12
 */
public interface ListBox<E> extends ValueComponent<E>, HasSpeedSearch<E>, HasItemSize<E> {
    @SafeVarargs
    static <E> ListBox<E> create(E... elements) {
        return UIInternal.get()._Components_listBox(FlatDataModel.of(Arrays.asList(elements)));
    }

    static <E> ListBox<E> create(Collection<E> elements) {
        return UIInternal.get()._Components_listBox(FlatDataModel.of(elements));
    }

    static <E> ListBox<E> create(FlatDataModel<E> model) {
        return UIInternal.get()._Components_listBox(model);
    }

    FlatDataModel<E> getDataModel();

    void setRender(TextItemRender<E> render);

    void setRender(ComponentItemRender<E> render);

    void setValueByIndex(int index);
}
