// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.components;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractListModel;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboBoxUI;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Combo-box with an ability to pick several items.
 * <p>
 * Use {@link #addActionListener(ActionListener)} to subscribe on change events.
 * Use {@link #getSelectedItems()} to get selected items explicitly.
 *
 * @author Anton Kozub
 */
public class MultiSelectComboBox<T> extends JComponent {
    private boolean myActionEventIsFiring = false;
    private List<T> myItems;
    private final Set<T> mySelectedItems = new LinkedHashSet<>();
    private final ComboBox<T> myComboBox;
    private final JPanel mySelectedPanel;
    private final MultiComboBoxModel<T> myComboBoxModel;
    private final @Nullable Function<T, String> myItemToTextConverter;
    private @Nullable String myPlaceholder;

    /**
     * @param items               The list of available items.
     * @param itemToTextConverter The function for extracting an item's presentable name.
     */
    @SuppressWarnings("unchecked")
    public MultiSelectComboBox(List<T> items, @Nullable Function<T, String> itemToTextConverter) {
        myItems = new ArrayList<>(items);
        myItemToTextConverter = itemToTextConverter;
        myComboBoxModel = new MultiComboBoxModel<>(items);

        setLayout(new BorderLayout());

        mySelectedPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        mySelectedPanel.setOpaque(false);

        myComboBox = new ComboBox<>(myComboBoxModel);
        myComboBox.setEditable(true);
        myComboBox.setEditor(new BasicComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return mySelectedPanel;
            }
        });

        if (myComboBox.getUI() instanceof BasicComboBoxUI ui) {
            ui.addEditor();
        }

        myComboBox.setRenderer((list, item, index, isSelected, cellHasFocus) -> new JLabel(getItemText(item)));

        myComboBox.addActionListener(e -> handleSelectionInPopupMenu());

        mySelectedPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (myComboBox.isEnabled() && SwingUtilities.isLeftMouseButton(e)) {
                    myComboBox.setPopupVisible(!myComboBox.isPopupVisible());
                }
            }
        });

        add(myComboBox);

        updateSelectedDisplay();
    }

    public MultiSelectComboBox(List<T> items) {
        this(items, null);
    }

    public MultiSelectComboBox(T[] items, @Nullable Function<T, String> itemToTextConverter) {
        this(Arrays.asList(items), itemToTextConverter);
    }

    public MultiSelectComboBox(T[] items) {
        this(Arrays.asList(items), null);
    }

    /**
     * The list of available items.
     */
    public List<T> getItems() {
        return Collections.unmodifiableList(myItems);
    }

    /**
     * The list of selected values.
     */
    public Set<T> getSelectedItems() {
        return Collections.unmodifiableSet(mySelectedItems);
    }

    public boolean isRequestFocusOnClick() {
        return myComboBox.isRequestFocusEnabled();
    }

    public void setRequestFocusOnClick(boolean value) {
        myComboBox.setRequestFocusEnabled(value);
    }

    public ComboBox<T> getComboBox() {
        return myComboBox;
    }

    public void setRenderer(ListCellRenderer<? super T> renderer) {
        myComboBox.setRenderer(renderer);
    }

    public void setPlaceholder(@Nullable String placeholder) {
        myPlaceholder = placeholder;
        updateSelectedDisplay();
    }

    public void setItems(List<T> newItems) {
        myItems = new ArrayList<>(newItems);
        setSelectedItems(new ArrayList<>(mySelectedItems));
    }

    public void setItems(T[] newItems) {
        setItems(Arrays.asList(newItems));
    }

    public @Nullable T getItemAt(int index) {
        return index >= 0 && index < myItems.size() ? myItems.get(index) : null;
    }

    public void insertItemAt(T item, int index) {
        myItems.add(index, item);
        setSelectedItems(new ArrayList<>(mySelectedItems));
    }

    public void removeItem(T item) {
        myItems.remove(item);
        setSelectedItems(new ArrayList<>(mySelectedItems));
    }

    public void setSelectedItems(@Nullable Collection<T> selectedItems) {
        Set<T> wanted = selectedItems == null ? Set.of() : new LinkedHashSet<>(selectedItems);

        Set<T> existingSelectedItems = new LinkedHashSet<>();
        List<T> unselectedItems = new ArrayList<>();
        for (T item : myItems) {
            if (wanted.contains(item)) {
                existingSelectedItems.add(item);
            }
            else {
                unselectedItems.add(item);
            }
        }

        mySelectedItems.clear();
        mySelectedItems.addAll(existingSelectedItems);

        myComboBoxModel.setObjects(unselectedItems);
        updateSelectedDisplay();
    }

    public void setSelectedItems(T @Nullable [] selectedItems) {
        setSelectedItems(selectedItems == null ? null : Arrays.asList(selectedItems));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        myComboBox.setEnabled(enabled);
        updateSelectedDisplay();
    }

    public void addActionListener(ActionListener listener) {
        listenerList.add(ActionListener.class, listener);
    }

    public void removeActionListener(ActionListener listener) {
        listenerList.remove(ActionListener.class, listener);
    }

    private void fireActionEvent() {
        if (myActionEventIsFiring) {
            return;
        }

        myActionEventIsFiring = true;

        try {
            ActionListener[] listeners = getListeners(ActionListener.class);
            if (listeners.length == 0) {
                return;
            }

            long mostRecentEventTime = EventQueue.getMostRecentEventTime();
            AWTEvent currentEvent = EventQueue.getCurrentEvent();

            int modifiers;
            if (currentEvent instanceof InputEvent inputEvent) {
                modifiers = inputEvent.getModifiersEx();
            }
            else if (currentEvent instanceof ActionEvent actionEvent) {
                modifiers = actionEvent.getModifiers();
            }
            else {
                modifiers = 0;
            }

            ActionEvent event = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "comboBoxChanged", mostRecentEventTime, modifiers);

            for (ActionListener listener : listeners) {
                listener.actionPerformed(event);
            }
        }
        finally {
            myActionEventIsFiring = false;
        }
    }

    @SuppressWarnings("unchecked")
    private void handleSelectionInPopupMenu() {
        Object selected = myComboBox.getSelectedItem();
        if (selected == null) {
            return;
        }

        T selectedItem = (T) selected;
        if (!mySelectedItems.contains(selectedItem) && myComboBoxModel.contains(selectedItem)) {
            selectItem(selectedItem);
        }

        myComboBox.setSelectedItem(null);
    }

    private void selectItem(T item) {
        mySelectedItems.add(item);
        myComboBoxModel.removeElement(item);
        updateSelectedDisplay();
        fireActionEvent();
    }

    private void deselectItem(T item) {
        mySelectedItems.remove(item);
        myComboBoxModel.insertElementAt(item, unselectedIndexOf(item));
        updateSelectedDisplay();
        fireActionEvent();
    }

    private int unselectedIndexOf(T item) {
        int index = 0;
        for (T candidate : myItems) {
            if (Objects.equals(candidate, item)) {
                break;
            }
            if (!mySelectedItems.contains(candidate)) {
                index++;
            }
        }
        return Math.min(index, myComboBoxModel.getSize());
    }

    public void updateSelectedDisplay() {
        mySelectedPanel.removeAll();

        int index = 0;
        for (T item : mySelectedItems) {
            if (index > 0) {
                mySelectedPanel.add(Box.createRigidArea(new Dimension(5, 0)));
            }

            JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 2, 0));
            itemPanel.setOpaque(true);
            itemPanel.setBorder(JBUI.Borders.emptyTop(2));
            itemPanel.setEnabled(myComboBox.isEnabled());

            Component textLabel = createItemComponent(item);
            textLabel.setEnabled(myComboBox.isEnabled());
            itemPanel.add(textLabel);

            JLabel closeLabel = new JLabel();
            closeLabel.setIcon(TargetAWT.to(PlatformIconGroup.actionsClose()));
            closeLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (!myComboBox.isEnabled()) {
                        return;
                    }
                    deselectItem(item);
                }
            });
            itemPanel.add(closeLabel);

            mySelectedPanel.add(itemPanel);
            index++;
        }

        if (mySelectedItems.isEmpty() && myPlaceholder != null && !myPlaceholder.isEmpty()) {
            JLabel placeholderLabel = new JLabel(myPlaceholder);
            placeholderLabel.setForeground(UIUtil.getInactiveTextColor());
            mySelectedPanel.add(placeholderLabel);
        }

        mySelectedPanel.revalidate();
        mySelectedPanel.repaint();
    }

    protected Component createItemComponent(T item) {
        return new JLabel(getItemText(item));
    }

    protected String getItemText(@Nullable T item) {
        if (myItemToTextConverter != null && item != null) {
            return myItemToTextConverter.apply(item);
        }
        return Objects.toString(item, "");
    }

    private static class MultiComboBoxModel<T> extends AbstractListModel<T> implements MutableComboBoxModel<T>, Serializable {
        private final List<T> myObjects;
        private @Nullable Object mySelectedObject;

        MultiComboBoxModel(Collection<T> items) {
            myObjects = new ArrayList<>(items);
        }

        void setObjects(List<T> objects) {
            int oldSize = myObjects.size();
            myObjects.clear();
            if (oldSize > 0) {
                fireIntervalRemoved(this, 0, oldSize - 1);
            }

            myObjects.addAll(objects);
            if (!objects.isEmpty()) {
                fireIntervalAdded(this, 0, objects.size() - 1);
            }
        }

        @Override
        public void setSelectedItem(@Nullable Object item) {
            if ((mySelectedObject != null && !mySelectedObject.equals(item)) || mySelectedObject == null && item != null) {
                mySelectedObject = item;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public @Nullable Object getSelectedItem() {
            return mySelectedObject;
        }

        @Override
        public int getSize() {
            return myObjects.size();
        }

        @Override
        public @Nullable T getElementAt(int index) {
            return index >= 0 && index < myObjects.size() ? myObjects.get(index) : null;
        }

        @Override
        public void addElement(T element) {
            myObjects.add(element);
            fireIntervalAdded(this, myObjects.size() - 1, myObjects.size() - 1);
        }

        @Override
        public void insertElementAt(T element, int index) {
            myObjects.add(index, element);
            fireIntervalAdded(this, index, index);
        }

        @Override
        public void removeElementAt(int index) {
            myObjects.remove(index);
            fireIntervalRemoved(this, index, index);
        }

        @Override
        public void removeElement(@Nullable Object element) {
            int index = myObjects.indexOf(element);
            if (index != -1) {
                removeElementAt(index);
            }
        }

        boolean contains(T element) {
            return myObjects.contains(element);
        }
    }
}
