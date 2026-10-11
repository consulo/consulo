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
package consulo.ui.impl.tree;

import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckableTree;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.ThreeState;

import java.util.List;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public interface CheckBoxTreeStateHolder<E> extends CheckableTree<E> {
    CheckBoxTreeState<E> getCheckBoxState();

    @Override
    default CheckBoxTreePolicy getCheckPolicy() {
        return getCheckBoxState().getPolicy();
    }

    @Override
    default void setCheckPolicy(CheckBoxTreePolicy policy) {
        getCheckBoxState().setPolicy(policy);
    }

    @Override
    default void setCheckableFilter(Predicate<E> filter) {
        getCheckBoxState().setCheckableFilter(filter);
    }

    @Override
    default void setCheckEnabledFilter(Predicate<E> filter) {
        getCheckBoxState().setCheckEnabledFilter(filter);
    }

    @Override
    default boolean isCheckable(E value) {
        return getCheckBoxState().isCheckable(value);
    }

    @Override
    default boolean isCheckEnabled(E value) {
        return getCheckBoxState().isCheckEnabled(value);
    }

    @Override
    default boolean isChecked(E value) {
        return getCheckBoxState().isChecked(value);
    }

    @Override
    default ThreeState getCheckState(E value) {
        return getCheckBoxState().getCheckState(value);
    }

    @Override
    default void setChecked(E value, boolean checked) {
        getCheckBoxState().setChecked(value, checked, ProgrammaticInputDetails.INSTANCE);
    }

    @Override
    default List<E> getCheckedValues() {
        return getCheckBoxState().getCheckedValues();
    }
}
