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
package consulo.ui.impl.model;

import consulo.ui.model.LazyFlatDataModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Holds its items the way the plain model does and only says that they may be read a page at a time, which is what a
 * frontend keys the kind of list it builds off.
 * <p/>
 * Being lazy is a property of the model a caller asks for rather than of the implementation - a list of a handful of
 * items is better off eager, and only the caller knows how many there will be.
 *
 * @author VISTALL
 */
public class LazyFlatDataModelImpl<E> extends FlatDataModelImpl<E> implements LazyFlatDataModel<E> {
    public LazyFlatDataModelImpl(Collection<? extends E> items) {
        super(items);
    }

    @Override
    public List<E> fetch(int offset, int limit) {
        int size = getSize();
        if (offset >= size || limit <= 0) {
            return List.of();
        }

        List<E> page = new ArrayList<>(Math.min(limit, size - offset));
        for (int i = offset; i < Math.min(offset + limit, size); i++) {
            page.add(get(i));
        }
        return page;
    }
}
