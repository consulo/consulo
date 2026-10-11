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
package consulo.ui.event;

import consulo.ui.CheckableTree;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.ThreeState;

/**
 * Sent once for every value whose own check state changed - the one the user or a call changed, and every one the
 * {@link consulo.ui.CheckBoxTreePolicy} carried the change to. The details of the value the user clicked are
 * shared by all of them.
 *
 * @author VISTALL
 * @since 2026-10-10
 */
public final class TreeCheckEvent<V> extends ComponentEvent<CheckableTree<V>> {
    private final V myValue;
    private final ThreeState myState;

    public TreeCheckEvent(CheckableTree<V> component, V value, boolean checked) {
        this(component, value, checked, ProgrammaticInputDetails.INSTANCE);
    }

    public TreeCheckEvent(CheckableTree<V> component, V value, boolean checked, InputDetails inputDetails) {
        this(component, value, checked ? ThreeState.YES : ThreeState.NO, inputDetails);
    }

    public TreeCheckEvent(CheckableTree<V> component, V value, ThreeState state, InputDetails inputDetails) {
        super(component, inputDetails);
        myValue = value;
        myState = state;
    }

    public V getValue() {
        return myValue;
    }

    public boolean isChecked() {
        return myState == ThreeState.YES;
    }

    /**
     * The new own state of the value - {@link ThreeState#UNSURE} only ever comes from a
     * {@link consulo.ui.TriStateCheckBoxTree}.
     */
    public ThreeState getState() {
        return myState;
    }
}
