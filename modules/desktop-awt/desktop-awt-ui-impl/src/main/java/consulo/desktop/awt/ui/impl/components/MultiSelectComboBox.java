// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.components;

import com.formdev.flatlaf.FlatClientProperties;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.ListCellRenderer;
import javax.swing.MutableComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
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
    private static final int MAX_WIDTH = 250;
    private static final String ELLIPSIS = "\u2026";
    private static final String CELL_BORDER_KEY = "MultiSelectComboBox.cellBorder";

    private boolean myActionEventIsFiring = false;
    private List<T> myItems;
    private final Set<T> mySelectedItems = new LinkedHashSet<>();
    private final CheckComboBox myComboBox;
    private final JPanel mySelectedPanel;
    private final JPanel myChipsPanel;
    private final TextRenderer myTextRenderer = new TextRenderer();
    private final JComboBox<String> myTextSizer = new JComboBox<>();
    private final JComboBox<String> myMinimumSizer = new JComboBox<>();
    private final MultiComboBoxModel<T> myComboBoxModel;
    private final @Nullable Function<T, String> myItemToTextConverter;
    private @Nullable String myPlaceholder;
    private @Nullable Function<List<T>, String> mySummary;
    private boolean myKeepPopupOpen;
    private boolean myRequestFocusOnClick = true;
    private String myClosedText = "";
    private boolean myClosedTextIsPlaceholder;

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

        myChipsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        myChipsPanel.setOpaque(false);

        mySelectedPanel = new JPanel(new GridBagLayout());
        mySelectedPanel.setOpaque(false);
        mySelectedPanel.add(
            myChipsPanel,
            new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0)
        );

        myTextSizer.setRenderer(myTextRenderer);
        myMinimumSizer.setRenderer(myTextRenderer);
        myMinimumSizer.putClientProperty(FlatClientProperties.MINIMUM_WIDTH, 0);

        myComboBox = new CheckComboBox(myComboBoxModel);
        myComboBox.setEditor(new BasicComboBoxEditor() {
            @Override
            public Component getEditorComponent() {
                return mySelectedPanel;
            }
        });

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
        return myRequestFocusOnClick;
    }

    public void setRequestFocusOnClick(boolean value) {
        myRequestFocusOnClick = value;
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

    public void setSummary(@Nullable Function<List<T>, String> summary) {
        mySummary = summary;
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
        for (T item : myItems) {
            if (wanted.contains(item)) {
                existingSelectedItems.add(item);
            }
        }

        mySelectedItems.clear();
        mySelectedItems.addAll(existingSelectedItems);

        if (!myComboBoxModel.hasObjects(myItems)) {
            myComboBoxModel.setObjects(myItems);
        }
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

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }

        Dimension size = getPreferredSize();
        Insets insets = getInsets();
        size.width = Math.min(size.width, myComboBox.textWidth(myMinimumSizer, ELLIPSIS) + insets.left + insets.right);
        return size;
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
        if (myComboBoxModel.contains(selectedItem) && !isNavigationEvent(EventQueue.getCurrentEvent())) {
            if (myComboBox.isPopupVisible()) {
                myKeepPopupOpen = true;
                SwingUtilities.invokeLater(() -> myKeepPopupOpen = false);
            }

            if (mySelectedItems.contains(selectedItem)) {
                deselectItem(selectedItem);
            }
            else {
                selectItem(selectedItem);
            }
        }

        int index = myItems.indexOf(selectedItem);
        myComboBox.setSelectedItem(null);
        restorePopupSelection(index);
    }

    private void restorePopupSelection(int index) {
        if (index < 0 || !myComboBox.isPopupVisible()) {
            return;
        }

        ComboPopup popup = myComboBox.getPopup();
        JList<?> list = popup == null ? null : popup.getList();
        if (list != null && index < list.getModel().getSize()) {
            list.setSelectedIndex(index);
        }
    }

    private static boolean isNavigationEvent(@Nullable AWTEvent event) {
        if (!(event instanceof KeyEvent keyEvent)) {
            return false;
        }

        int keyCode = keyEvent.getKeyCode();
        return keyCode != KeyEvent.VK_ENTER && keyCode != KeyEvent.VK_SPACE;
    }

    private void selectItem(T item) {
        Set<T> selection = new LinkedHashSet<>(mySelectedItems);
        selection.add(item);
        applyOrderedSelection(selection);
        updateSelectedDisplay();
        fireActionEvent();
    }

    private void deselectItem(T item) {
        mySelectedItems.remove(item);
        updateSelectedDisplay();
        fireActionEvent();
    }

    private void applyOrderedSelection(Set<T> selection) {
        mySelectedItems.clear();
        for (T item : myItems) {
            if (selection.contains(item)) {
                mySelectedItems.add(item);
            }
        }
    }

    private void repaintPopupList() {
        ComboPopup popup = myComboBox.getPopup();
        JList<?> list = popup == null ? null : popup.getList();
        if (list != null) {
            list.repaint();
        }
    }

    public void updateSelectedDisplay() {
        repaintPopupList();

        Function<List<T>, String> summary = mySummary;
        boolean textMode = mySelectedItems.isEmpty() || summary != null;
        if (mySelectedItems.isEmpty()) {
            myClosedText = Objects.toString(myPlaceholder, "");
            myClosedTextIsPlaceholder = true;
        }
        else if (summary != null) {
            myClosedText = summary.apply(new ArrayList<>(mySelectedItems));
            myClosedTextIsPlaceholder = false;
        }
        else {
            myClosedText = "";
            myClosedTextIsPlaceholder = false;
        }

        if (myComboBox.isEditable() == textMode) {
            myComboBox.setEditable(!textMode);
            myComboBox.setRequestFocusEnabled(myRequestFocusOnClick);
        }

        rebuildChips(textMode);

        myComboBox.revalidate();
        myComboBox.repaint();
    }

    private void rebuildChips(boolean textMode) {
        myChipsPanel.removeAll();

        if (!textMode) {
            int index = 0;
            for (T item : mySelectedItems) {
                if (index > 0) {
                    myChipsPanel.add(Box.createRigidArea(new Dimension(JBUI.scale(5), 0)));
                }

                JPanel itemPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, JBUI.scale(2), 0));
                itemPanel.setOpaque(false);
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

                myChipsPanel.add(itemPanel);
                index++;
            }
        }

        mySelectedPanel.setBorder(BorderFactory.createEmptyBorder(0, chipsLeftInset(), 0, 0));
        mySelectedPanel.revalidate();
        mySelectedPanel.repaint();
    }

    private static int chipsLeftInset() {
        Insets padding = UIManager.getInsets("ComboBox.padding");
        int paddingLeft = padding == null ? 0 : JBUI.scale(padding.left);
        Insets cellInsets = JBCurrentTheme.listCellBorderFull().getBorderInsets(null);
        return Math.max(paddingLeft, cellInsets.left);
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

        boolean hasObjects(List<T> objects) {
            return myObjects.equals(objects);
        }
    }

    private final class CheckComboBox extends ComboBox<T> {
        CheckComboBox(MultiComboBoxModel<T> model) {
            super(model);
        }

        @Override
        public void setRenderer(@Nullable ListCellRenderer<? super T> renderer) {
            super.setRenderer(renderer == null ? null : new CheckRenderer(renderer));
        }

        @Override
        public void setPopupVisible(boolean visible) {
            if (!visible && myKeepPopupOpen) {
                myKeepPopupOpen = false;
                return;
            }
            super.setPopupVisible(visible);
        }

        @Override
        public void updateUI() {
            super.updateUI();
            SwingUtilities.updateComponentTreeUI(myTextRenderer);
            myTextSizer.updateUI();
            myMinimumSizer.updateUI();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            if (!isEditable()) {
                size.width = textWidth(myTextSizer, myClosedText);
            }
            size.width = Math.min(size.width, JBUI.scale(MAX_WIDTH));
            return size;
        }

        private int textWidth(JComboBox<String> sizer, String text) {
            sizer.setFont(getFont());
            sizer.setPrototypeDisplayValue(text);

            Insets sizerInsets = sizer.getInsets();
            Insets insets = getInsets();
            return sizer.getPreferredSize().width - sizerInsets.left - sizerInsets.right + insets.left + insets.right;
        }
    }

    private final class CheckRenderer implements ListCellRenderer<T> {
        private final ListCellRenderer<? super T> myDelegate;
        private final JPanel myPanel = new JPanel(new BorderLayout());
        private final JLabel myCheck = new JLabel();

        CheckRenderer(ListCellRenderer<? super T> delegate) {
            myDelegate = delegate;
            myCheck.setBorder(JBUI.Borders.emptyRight(4));
            myCheck.setOpaque(false);
        }

        @Override
        public Component getListCellRendererComponent(
            JList<? extends T> list,
            @Nullable T value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            if (index < 0 && value == null) {
                return myTextRenderer.getListCellRendererComponent(list, myClosedText, index, false, false);
            }

            Component component = myDelegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            myPanel.setBorder(takeCellBorder(component));

            Image checked = PlatformIconGroup.actionsChecked();
            Image mark = value != null && mySelectedItems.contains(value) ? checked : Image.empty(checked.getWidth(), checked.getHeight());
            myCheck.setIcon(TargetAWT.to(mark));

            myPanel.removeAll();
            myPanel.add(myCheck, BorderLayout.WEST);
            myPanel.add(component, BorderLayout.CENTER);
            myPanel.setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            myPanel.setOpaque(index >= 0);
            return myPanel;
        }

        private static @Nullable Border takeCellBorder(Component component) {
            if (!(component instanceof JComponent jComponent)) {
                return null;
            }

            Border border = jComponent.getBorder();
            if (border == null) {
                return (Border) jComponent.getClientProperty(CELL_BORDER_KEY);
            }

            jComponent.putClientProperty(CELL_BORDER_KEY, border);
            jComponent.setBorder(null);
            return border;
        }
    }

    private final class TextRenderer extends ColoredListCellRenderer<Object> {
        private String myText = "";
        private SimpleTextAttributes myTextAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;

        @Override
        public Component getListCellRendererComponent(JList<?> list, @Nullable Object value, int index, boolean selected, boolean hasFocus) {
            setEnabled(myComboBox.isEnabled());
            return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
        }

        @Override
        protected void customizeCellRenderer(JList<?> list, @Nullable Object value, int index, boolean selected, boolean hasFocus) {
            setBorder(JBCurrentTheme.listCellBorderFull());

            myText = Objects.toString(value, "");
            myTextAttributes = myClosedTextIsPlaceholder ? SimpleTextAttributes.GRAYED_ATTRIBUTES : SimpleTextAttributes.REGULAR_ATTRIBUTES;
            append(myText, myTextAttributes);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            size.width -= getIpad().right + getInsets().left;
            return size;
        }

        @Override
        protected void doPaint(Graphics2D g) {
            fitText();
            super.doPaint(g);
        }

        private void fitText() {
            if (myText.isEmpty()) {
                return;
            }

            FontMetrics metrics = getFontMetrics(getFont());
            int available = getWidth() - getIpad().left - getInsets().right;
            if (metrics.stringWidth(myText) <= available) {
                return;
            }

            String fitted = ellipsize(myText, metrics, available);
            change(() -> {
                clear();
                append(fitted, myTextAttributes);
            }, false);
        }

        private static String ellipsize(String text, FontMetrics metrics, int available) {
            for (int end = text.length() - 1; end > 0; end--) {
                if (Character.isLowSurrogate(text.charAt(end))) {
                    continue;
                }

                String candidate = text.substring(0, end).stripTrailing() + ELLIPSIS;
                if (metrics.stringWidth(candidate) <= available) {
                    return candidate;
                }
            }
            return ELLIPSIS;
        }
    }
}
