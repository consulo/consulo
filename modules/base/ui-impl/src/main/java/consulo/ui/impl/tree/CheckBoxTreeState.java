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

import consulo.logging.Logger;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.CheckableTree;
import consulo.ui.TreeNode;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.TreeCheckEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.util.lang.ThreeState;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-10
 */
public final class CheckBoxTreeState<E> {
    private static final Logger LOG = Logger.getInstance(CheckBoxTreeState.class);

    private final CheckableTree<E> myTree;
    private final TreeNode<E> myRoot;
    private final Function<TreeNode<E>, @Nullable TreeNode<E>> myParent;
    private final Runnable myChangeListener;

    private volatile CheckBoxTreePolicy myPolicy = CheckBoxTreePolicy.CASCADE;
    private volatile Predicate<E> myCheckableFilter = value -> true;
    private volatile Predicate<E> myEnabledFilter = value -> true;

    private final Set<E> myChecked = new LinkedHashSet<>();

    private Set<E> myKnown = new HashSet<>();
    private @Nullable Map<TreeNode<E>, @Nullable ThreeState> myShownStates;

    public CheckBoxTreeState(CheckableTree<E> tree,
                             TreeNode<E> root,
                             Function<TreeNode<E>, @Nullable TreeNode<E>> parent,
                             Runnable changeListener) {
        myTree = tree;
        myRoot = root;
        myParent = parent;
        myChangeListener = changeListener;
    }

    public static <E> CheckBoxTreeState<E> attach(CheckableTree<E> tree, TreeController<E> controller, Runnable changeListener) {
        CheckBoxTreeState<E> state = new CheckBoxTreeState<>(tree, controller.getRoot(), CheckBoxTreeState::parentOf, changeListener);
        controller.setStructureListener(state::structureChanged);
        return state;
    }

    private static <E> @Nullable TreeNode<E> parentOf(TreeNode<E> node) {
        return node instanceof TreeNodeImpl<E> impl ? impl.getParent() : null;
    }

    public CheckBoxTreePolicy getPolicy() {
        return myPolicy;
    }

    @RequiredUIAccess
    public void setPolicy(CheckBoxTreePolicy policy) {
        myPolicy = policy;
        finish(Map.of(), ProgrammaticInputDetails.INSTANCE);
    }

    @RequiredUIAccess
    public void setCheckableFilter(Predicate<E> filter) {
        myCheckableFilter = filter;

        Map<E, Boolean> changes = new LinkedHashMap<>();
        for (E value : checkedSnapshot()) {
            if (!isCheckable(value)) {
                remove(value, changes);
            }
        }
        finish(changes, ProgrammaticInputDetails.INSTANCE);
    }

    @RequiredUIAccess
    public void setCheckEnabledFilter(Predicate<E> filter) {
        myEnabledFilter = filter;
        finish(Map.of(), ProgrammaticInputDetails.INSTANCE);
    }

    public boolean isCheckable(@Nullable E value) {
        return value != null && test(myCheckableFilter, value);
    }

    public boolean isCheckEnabled(@Nullable E value) {
        return isCheckable(value) && test(myEnabledFilter, value);
    }

    public boolean isChecked(@Nullable E value) {
        if (value == null) {
            return false;
        }

        synchronized (myChecked) {
            return myChecked.contains(value);
        }
    }

    public List<E> getCheckedValues() {
        Set<E> checked = new LinkedHashSet<>(checkedSnapshot());

        List<E> values = new ArrayList<>(checked.size());
        collectChecked(myRoot, checked, values);
        values.addAll(checked);
        return List.copyOf(values);
    }

    private void collectChecked(TreeNode<E> node, Set<E> checked, List<E> values) {
        for (TreeNode<E> child : node.getLoadedChildren()) {
            E value = child.getValue();
            if (value != null && checked.remove(value)) {
                values.add(value);
            }
            collectChecked(child, checked, values);
        }
    }

    public ThreeState getCheckState(E value) {
        if (!isCheckable(value)) {
            return ThreeState.NO;
        }

        TreeNode<E> node = find(myRoot, value);
        if (node == null) {
            return isChecked(value) ? ThreeState.YES : ThreeState.NO;
        }

        ThreeState state = shownState(node, new IdentityHashMap<>());
        return state == null ? ThreeState.NO : state;
    }

    @RequiredUIAccess
    public @Nullable ThreeState getShownState(TreeNode<E> node) {
        if (!isCheckable(node.getValue())) {
            return null;
        }

        Map<TreeNode<E>, @Nullable ThreeState> states = myShownStates;
        if (states == null) {
            states = new IdentityHashMap<>();
            myShownStates = states;
        }
        return shownState(node, states);
    }

    private @Nullable ThreeState shownState(TreeNode<E> node, Map<TreeNode<E>, @Nullable ThreeState> states) {
        if (states.containsKey(node)) {
            return states.get(node);
        }

        E value = node.getValue();
        ThreeState own = isCheckable(value) ? (isChecked(value) ? ThreeState.YES : ThreeState.NO) : null;

        ThreeState state = own;
        if (!myPolicy.isIndependent()) {
            ThreeState children = null;
            boolean mixed = false;
            for (TreeNode<E> child : node.getLoadedChildren()) {
                ThreeState childState = shownState(child, states);
                if (childState == null) {
                    continue;
                }

                if (childState == ThreeState.UNSURE || children != null && children != childState) {
                    mixed = true;
                    break;
                }
                children = childState;
            }

            if (mixed) {
                state = ThreeState.UNSURE;
            }
            else if (children != null) {
                state = own == null || own == children ? children : ThreeState.UNSURE;
            }
        }

        states.put(node, state);
        return state;
    }

    @RequiredUIAccess
    public void setChecked(E value, boolean checked, InputDetails details) {
        if (!isCheckable(value)) {
            return;
        }

        Map<E, Boolean> changes = new LinkedHashMap<>();
        change(value, checked, changes);

        TreeNode<E> node = find(myRoot, value);
        if (node != null) {
            spread(node, checked, changes);
        }
        finish(changes, details);
    }

    @RequiredUIAccess
    public boolean toggle(TreeNode<E> node, List<? extends TreeNode<E>> selection, InputDetails details) {
        E value = node.getValue();
        if (value == null || !isCheckEnabled(value)) {
            return false;
        }

        boolean checked = !isChecked(value);

        Map<E, Boolean> changes = new LinkedHashMap<>();
        change(value, checked, changes);
        spread(node, checked, changes);

        for (TreeNode<E> other : selection) {
            E otherValue = other.getValue();
            if (otherValue == null || otherValue.equals(value) || !isCheckEnabled(otherValue)) {
                continue;
            }

            change(otherValue, checked, changes);
            spread(other, checked, changes);
        }

        finish(changes, details);
        return true;
    }

    @RequiredUIAccess
    public void structureChanged() {
        Map<E, Boolean> changes = new LinkedHashMap<>();
        Set<E> present = new HashSet<>();
        inherit(myRoot, false, myPolicy.checkChildrenWithCheckedParent(), present, changes);

        for (E value : myKnown) {
            if (!present.contains(value)) {
                remove(value, changes);
            }
        }
        myKnown = present;

        finish(changes, ProgrammaticInputDetails.INSTANCE);
    }

    private void inherit(TreeNode<E> node, boolean parentChecked, boolean inherit, Set<E> present, Map<E, Boolean> changes) {
        for (TreeNode<E> child : node.getLoadedChildren()) {
            E value = child.getValue();

            boolean checked = parentChecked;
            if (value != null) {
                boolean fresh = !myKnown.contains(value) && !present.contains(value);
                present.add(value);

                if (isCheckable(value)) {
                    if (fresh && inherit && parentChecked) {
                        change(value, true, changes);
                    }
                    checked = isChecked(value);
                }
            }

            inherit(child, checked, inherit, present, changes);
        }
    }

    private void spread(TreeNode<E> node, boolean checked, Map<E, Boolean> changes) {
        CheckBoxTreePolicy policy = myPolicy;

        if (checked ? policy.checkChildrenWithCheckedParent() : policy.uncheckChildrenWithUncheckedParent()) {
            setBelow(node, checked, changes);
        }

        if (checked ? policy.checkParentWithCheckedChild() : policy.uncheckParentWithUncheckedChild()) {
            setAbove(node, checked, changes);
        }
    }

    private void setBelow(TreeNode<E> node, boolean checked, Map<E, Boolean> changes) {
        for (TreeNode<E> child : node.getLoadedChildren()) {
            E value = child.getValue();
            if (value != null) {
                change(value, checked, changes);
            }
            setBelow(child, checked, changes);
        }
    }

    private void setAbove(TreeNode<E> node, boolean checked, Map<E, Boolean> changes) {
        for (TreeNode<E> parent = myParent.apply(node); parent != null && parent != myRoot; parent = myParent.apply(parent)) {
            E value = parent.getValue();
            if (value != null) {
                change(value, checked, changes);
            }
        }
    }

    private void change(E value, boolean checked, Map<E, Boolean> changes) {
        if (!isCheckable(value)) {
            return;
        }

        boolean changed;
        synchronized (myChecked) {
            changed = checked ? myChecked.add(value) : myChecked.remove(value);
        }

        if (changed) {
            record(value, checked, changes);
        }
    }

    private void remove(E value, Map<E, Boolean> changes) {
        boolean changed;
        synchronized (myChecked) {
            changed = myChecked.remove(value);
        }

        if (changed) {
            record(value, false, changes);
        }
    }

    private static <E> void record(E value, boolean checked, Map<E, Boolean> changes) {
        if (changes.containsKey(value)) {
            changes.remove(value);
        }
        else {
            changes.put(value, checked);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void finish(Map<E, Boolean> changes, InputDetails details) {
        myShownStates = null;

        try {
            myChangeListener.run();
        }
        catch (RuntimeException e) {
            LOG.error(e);
        }

        for (Map.Entry<E, Boolean> change : changes.entrySet()) {
            myTree.getListenerDispatcher(TreeCheckEvent.class).onEvent(new TreeCheckEvent<>(myTree, change.getKey(), change.getValue(), details));
        }
    }

    private @Nullable TreeNode<E> find(TreeNode<E> node, E value) {
        for (TreeNode<E> child : node.getLoadedChildren()) {
            if (Objects.equals(child.getValue(), value)) {
                return child;
            }

            TreeNode<E> found = find(child, value);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    private List<E> checkedSnapshot() {
        synchronized (myChecked) {
            return List.copyOf(myChecked);
        }
    }

    private static <E> boolean test(Predicate<E> filter, E value) {
        try {
            return filter.test(value);
        }
        catch (RuntimeException e) {
            LOG.error(e);
            return false;
        }
    }
}
