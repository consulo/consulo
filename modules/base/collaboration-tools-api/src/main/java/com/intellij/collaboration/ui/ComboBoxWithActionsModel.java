// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui;

import consulo.proxy.EventDispatcher;
import consulo.ui.ex.awt.MutableCollectionComboBoxModel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

final class ComboBoxWithActionsModel<T> implements ComboBoxModel<ComboBoxWithActionsModel.Item<T>> {
    private final MutableCollectionComboBoxModel<Item.Wrapper<T>> itemsModel = new MutableCollectionComboBoxModel<>();
    private final EventDispatcher<ListDataListener> listEventDispatcher = EventDispatcher.create(ListDataListener.class);

    private List<Action> actions = List.of();

    ComboBoxWithActionsModel() {
        itemsModel.addListDataListener(new ListDataListener() {
            @Override
            public void intervalAdded(ListDataEvent e) {
                listEventDispatcher.getMulticaster().intervalAdded(e);
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                listEventDispatcher.getMulticaster().intervalRemoved(e);
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                listEventDispatcher.getMulticaster().contentsChanged(e);
            }
        });
    }

    @Nonnull
    List<T> getItems() {
        return itemsModel.getItems().stream()
            .map(w -> w.wrappee)
            .collect(Collectors.toList());
    }

    void setItems(@Nonnull List<T> value) {
        List<Item.Wrapper<T>> newItems = value.stream().map(Item.Wrapper::new).collect(Collectors.toList());
        List<Item.Wrapper<T>> currentItems = new ArrayList<>(itemsModel.getItems());

        // Remove items no longer present
        Set<Item.Wrapper<T>> newSet = new HashSet<>(newItems);
        for (Item.Wrapper<T> item : currentItems) {
            if (!newSet.contains(item)) {
                itemsModel.removeElement(item);
            }
        }

        // Add new items
        Set<Item.Wrapper<T>> currentSet = new HashSet<>(itemsModel.getItems());
        List<Item.Wrapper<T>> toAdd = new ArrayList<>();
        for (Item.Wrapper<T> item : newItems) {
            if (!currentSet.contains(item)) {
                toAdd.add(item);
            }
        }
        if (!toAdd.isEmpty()) {
            itemsModel.add(toAdd);
        }
    }

    @Nonnull
    List<Action> getActions() {
        return actions;
    }

    void setActions(@Nonnull List<Action> newActions) {
        List<Action> oldValue = this.actions;
        this.actions = newActions;
        if (!oldValue.isEmpty()) {
            listEventDispatcher.getMulticaster().intervalRemoved(
                new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, itemsModel.getSize(), itemsModel.getSize() + oldValue.size() - 1));
        }
        if (!newActions.isEmpty()) {
            listEventDispatcher.getMulticaster().intervalAdded(
                new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, itemsModel.getSize(), itemsModel.getSize() + newActions.size() - 1));
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public Item.@Nullable Wrapper<T> getSelectedItem() {
        Object selected = itemsModel.getSelectedItem();
        if (selected instanceof Item.Wrapper<?>) {
            return (Item.Wrapper<T>) selected;
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setSelectedItem(Object item) {
        if (item instanceof Item.Action<?> actionItem) {
            Action action = actionItem.action;
            if (action.isEnabled()) {
                action.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
            return;
        }
        if (item instanceof Item.Wrapper<?>) {
            itemsModel.setSelectedItem(item);
        }
        else {
            itemsModel.setSelectedItem(null);
        }
    }

    @Override
    public int getSize() {
        return itemsModel.getSize() + actions.size();
    }

    @Override
    public Item<T> getElementAt(int index) {
        if (index >= 0 && index < itemsModel.getSize()) {
            return itemsModel.getElementAt(index);
        }
        int actionIndex = index - itemsModel.getSize();
        if (actionIndex >= 0 && actionIndex < actions.size()) {
            return new Item.Action<>(actions.get(actionIndex), actionIndex == 0 && itemsModel.getSize() != 0);
        }
        throw new IndexOutOfBoundsException("Invalid index " + index);
    }

    @Override
    public void addListDataListener(ListDataListener l) {
        listEventDispatcher.addListener(l);
    }

    @Override
    public void removeListDataListener(ListDataListener l) {
        listEventDispatcher.removeListener(l);
    }

    public sealed interface Item<T> {
        record Wrapper<T>(T wrappee) implements Item<T> {
        }

        final class Action<T> implements Item<T> {
            final javax.swing.Action action;
            final boolean needSeparatorAbove;

            Action(@Nonnull javax.swing.Action action, boolean needSeparatorAbove) {
                this.action = action;
                this.needSeparatorAbove = needSeparatorAbove;
            }

            public @Nonnull javax.swing.Action getAction() {
                return action;
            }

            public boolean getNeedSeparatorAbove() {
                return needSeparatorAbove;
            }
        }
    }
}
