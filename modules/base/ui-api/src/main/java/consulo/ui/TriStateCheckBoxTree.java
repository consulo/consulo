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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

/**
 * A {@link Tree} whose rows carry a {@link TriStateCheckBox} in front of the node presentation - see
 * {@link CheckableTree} for how the check boxes behave.
 * <p>
 * Besides checked and unchecked, the own state of a value can be {@link ThreeState#UNSURE}, for a value whose
 * data is partly on and partly off. Such a value spreads nothing to the values around it. A click moves an
 * {@code UNSURE} value to {@code YES} and toggles between {@code YES} and {@code NO} otherwise; only when
 * {@link #setUnsureEnabled(boolean) the third state is enabled} does a click move {@code NO} to {@code UNSURE},
 * the same way a {@link TriStateCheckBox} does.
 *
 * @author VISTALL
 * @since 2026-10-11
 */
public interface TriStateCheckBoxTree<E> extends CheckableTree<E> {
    static <E> TriStateCheckBoxTree<E> create(TreeModel<E> model) {
        return create(null, model);
    }

    static <E> TriStateCheckBoxTree<E> create(@Nullable E rootValue, TreeModel<E> model) {
        return create(rootValue, model, TreeExecutor.uiThread());
    }

    static <E> TriStateCheckBoxTree<E> create(TreeModel<E> model, TreeExecutor executor) {
        return create(null, model, executor);
    }

    /**
     * @param executor where the model runs - {@link TreeExecutor#uiThread()} is only right for a model that
     *                 computes nothing, see {@link TreeExecutor}
     */
    static <E> TriStateCheckBoxTree<E> create(@Nullable E rootValue, TreeModel<E> model, TreeExecutor executor) {
        return UIInternal.get()._Components_triStateCheckBoxTree(rootValue, model, executor);
    }

    /**
     * The own state of the value, whatever the values below it are.
     */
    ThreeState getOwnCheckState(E value);

    /**
     * Changes the own state of the value. {@code YES} and {@code NO} spread as the policy says, the same as
     * {@link #setChecked(Object, boolean)}; {@code UNSURE} stays on the value alone. A value which carries no check
     * box is left alone.
     */
    @RequiredUIAccess
    void setCheckState(E value, ThreeState state);

    /**
     * Whether a click can move a value to {@link ThreeState#UNSURE}. Off unless set - the third state is then
     * only ever set by a call.
     */
    boolean isUnsureEnabled();

    @RequiredUIAccess
    void setUnsureEnabled(boolean unsureEnabled);
}
