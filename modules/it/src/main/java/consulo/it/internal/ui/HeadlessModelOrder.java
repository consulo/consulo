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
package consulo.it.internal.ui;

import consulo.ui.model.FlatDataModel;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The value of a headless multi-select component: the given items that are in the model, in model order.
 *
 * @author VISTALL
 */
public final class HeadlessModelOrder {
    private HeadlessModelOrder() {
    }

    public static <E> List<E> retain(FlatDataModel<E> model, @Nullable List<E> value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }

        Set<E> wanted = new HashSet<>(value);
        List<E> selected = new ArrayList<>();
        for (int i = 0; i < model.getSize(); i++) {
            E item = model.get(i);
            if (wanted.contains(item)) {
                selected.add(item);
            }
        }
        return selected;
    }
}
