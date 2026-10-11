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

import consulo.disposer.Disposable;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ComponentEventListener;
import consulo.ui.event.TreeCheckEvent;
import consulo.util.lang.ThreeState;

import java.util.List;
import java.util.function.Predicate;

/**
 * A {@link Tree} whose rows carry a check box in front of the node presentation - in the tree column, when the tree
 * has columns. The user toggles a check box by clicking it, and the check boxes of the selected rows with the space
 * key.
 * <p/>
 * The check state belongs to the value of a node, not to the node: it stays with the value while the tree rebuilds
 * its levels, and a value which is checked before its level is built shows checked once it is. A value the tree
 * stops holding - its level was built again without it - forgets its state.
 * <p/>
 * How a change spreads to the values above and below the changed one is the {@link CheckBoxTreePolicy} of the
 * tree, {@link CheckBoxTreePolicy#CASCADE} unless set. A change only reaches the nodes the tree has built, except
 * that the children of a checked node are checked as they are built when the policy checks children with their
 * parent.
 * <p/>
 * The filters decide which values carry a check box and which of those the user may change. They are asked on the
 * UI thread, so they answer from the value alone and never read a model. The check state may be read on any
 * thread, and is changed on the UI thread.
 *
 * @author VISTALL
 * @since 2026-10-10
 * @see CheckBoxTree
 * @see CheckBoxTreeTable
 */
public interface CheckableTree<E> extends Tree<E> {
    CheckBoxTreePolicy getCheckPolicy();

    /**
     * Applies to the changes made from now on - the values already checked are left as they are.
     */
    @RequiredUIAccess
    void setCheckPolicy(CheckBoxTreePolicy policy);

    /**
     * Values the filter rejects show no check box, are never checked, and are passed through when a change spreads.
     * A value which is checked and is rejected by a new filter is unchecked. Every value carries a check box unless
     * set.
     */
    @RequiredUIAccess
    void setCheckableFilter(Predicate<E> filter);

    /**
     * Values the filter rejects show a disabled check box: the user cannot change it, while a call or a change
     * spreading from another value still can. Every check box is enabled unless set.
     */
    @RequiredUIAccess
    void setCheckEnabledFilter(Predicate<E> filter);

    boolean isCheckable(E value);

    boolean isCheckEnabled(E value);

    /**
     * The own state of the value, whatever the values below it are.
     */
    boolean isChecked(E value);

    /**
     * The state the check box of the value shows: {@link ThreeState#UNSURE} for a value the policy links to the
     * values below it when those differ among themselves or from it, the own state of the value otherwise.
     * {@link ThreeState#NO} for a value which carries no check box.
     */
    ThreeState getCheckState(E value);

    /**
     * Changes the own state of the value and spreads the change as the policy says. A value which carries no check
     * box is left alone.
     */
    @RequiredUIAccess
    void setChecked(E value, boolean checked);

    /**
     * Every checked value - parents as well as leaves - those the tree holds first, in the order they are shown,
     * then those it has not built yet, in the order they were checked.
     */
    List<E> getCheckedValues();

    @SuppressWarnings("unchecked")
    default Disposable addCheckListener(ComponentEventListener<CheckableTree<E>, TreeCheckEvent<E>> listener) {
        return addListener((Class) TreeCheckEvent.class, listener);
    }
}
