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

import consulo.ui.TriStateCheckBoxTree;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.ThreeState;

/**
 * @author VISTALL
 * @since 2026-10-11
 */
public interface TriStateCheckBoxTreeStateHolder<E> extends CheckBoxTreeStateHolder<E>, TriStateCheckBoxTree<E> {
    @Override
    default ThreeState getOwnCheckState(E value) {
        return getCheckBoxState().getOwnCheckState(value);
    }

    @Override
    default void setCheckState(E value, ThreeState state) {
        getCheckBoxState().setCheckState(value, state, ProgrammaticInputDetails.INSTANCE);
    }

    @Override
    default boolean isUnsureEnabled() {
        return getCheckBoxState().isUnsureEnabled();
    }

    @Override
    default void setUnsureEnabled(boolean unsureEnabled) {
        getCheckBoxState().setUnsureEnabled(unsureEnabled);
    }
}
