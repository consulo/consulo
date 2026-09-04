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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.desktop.awt.ui.impl.event.DesktopAWTInputDetails;
import consulo.desktop.awt.ui.impl.facade.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.FontBox;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.ValueComponentEvent;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.ex.awt.ComboBox;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.font.FontManager;
import consulo.ui.font.Typeface;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Each row is drawn in the family it names, which is the whole point of choosing a font by eye.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
class DesktopFontBoxImpl extends SwingComponentDelegate<DesktopFontBoxImpl.MyComboBox> implements FontBox {
    private static final Logger LOG = Logger.getInstance(DesktopFontBoxImpl.class);

    class MyComboBox extends ComboBox<Object> implements FromSwingComponentWrapper {
        @Override
        public Component toUIComponent() {
            return DesktopFontBoxImpl.this;
        }
    }

    /**
     * Stands in for the families until they are known, so the chooser reads as busy rather than as
     * empty.
     */
    private static class LoadingItem {
        @Override
        public String toString() {
            return UILocalize.fontBoxLoading().get();
        }
    }

    /**
     * Draws the family name in its own typeface, falling back to the list font when the family
     * cannot render its own name - a CJK-only family under a latin name would come out as boxes.
     * The collapsed row keeps the list font whatever is chosen, so that picking a family with tall
     * metrics does not resize the chooser.
     */
    private static class FontRenderer extends DefaultListCellRenderer {
        @Override
        public java.awt.Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean selected,
            boolean focused
        ) {
            super.getListCellRendererComponent(list, value, index, selected, focused);

            if (index != -1 && value instanceof Typeface typeface) {
                String name = typeface.getName();
                Font font = new Font(name, Font.PLAIN, getFont().getSize());
                if (font.canDisplayUpTo(name) == -1) {
                    setFont(font);
                }
            }
            return this;
        }

        /**
         * Only the height is answered honestly. Swing sizes a list by asking every row how wide it
         * wants to be, and a row here answers by laying its name out in its own typeface, which with
         * a few hundred families installed is the slow half of opening the chooser. The combo pins
         * its own width instead, so nothing reads the width returned here.
         */
        @Override
        public Dimension getPreferredSize() {
            Font font = getFont();
            if (font == null) {
                return super.getPreferredSize();
            }

            Insets insets = getInsets();
            return new Dimension(1, getFontMetrics(font).getHeight() + insets.top + insets.bottom);
        }
    }

    private volatile List<Typeface> myTypefaces = List.of();

    private boolean myMonospacedOnly;

    /**
     * Held while the families are still being enumerated, and dropped when they arrive.
     */
    private @Nullable LoadingItem myLoadingItem = new LoadingItem();

    /**
     * Refilling the model makes swing clear and reset the selection, and every one of those steps
     * raises an action event - without this the page would read a rebuild as the user picking a font.
     */
    private boolean myFireListeners = true;

    /**
     * What the caller asked for, which can arrive before the families do. The list is filled off the
     * ui thread, so a reset writing the stored family back has nothing to select yet.
     */
    private @Nullable String myRequestedValue;

    @Override
    protected MyComboBox createComponent() {
        MyComboBox comboBox = new MyComboBox();
        comboBox.setRenderer(new FontRenderer());
        comboBox.addItem(myLoadingItem);
        comboBox.setMinimumAndPreferredWidth(comboBox.getPreferredSize().height * 8);

        comboBox.addActionListener(e -> {
            String name = nameOf(comboBox.getSelectedItem());
            if (!myFireListeners || name == null) {
                return;
            }

            myRequestedValue = name;

            fireListeners();
        });

        loadFonts();

        return comboBox;
    }

    /**
     * Enumerating the installed families is the first thing that touches the font subsystem and can take
     * seconds on a machine with many of them, which is what the font manager keeps off the ui thread.
     */
    @RequiredUIAccess
    private void loadFonts() {
        UIAccess uiAccess = UIAccess.current();

        FontManager.get().getAvailableTypefacesAsync(uiAccess)
            .whenCompleteAsync((typefaces, e) -> {
                if (e != null) {
                    // the box would otherwise sit on the loading item with nothing said anywhere
                    LOG.error("Failed to read the installed font families", e);
                }
                else {
                    myTypefaces = typefaces;
                }

                myLoadingItem = null;
                fillItems();
            }, uiAccess);
    }

    @RequiredUIAccess
    private void fillItems() {
        MyComboBox comboBox = toAWTComponent();

        myFireListeners = false;
        try {
            comboBox.removeAllItems();

            if (myLoadingItem != null) {
                comboBox.addItem(myLoadingItem);
            }

            for (Typeface typeface : myTypefaces) {
                if (!myMonospacedOnly || typeface.isMonospaced()) {
                    comboBox.addItem(typeface);
                }
            }

            applyRequestedValue();
        }
        finally {
            myFireListeners = true;
        }
    }

    /**
     * A family the environment does not have, or one the monospaced filter left out, is still the stored value
     * and is shown rather than dropped - the user sees what the scheme asks for instead of a silent
     * substitution. It has to be put in the model to be shown at all: swing rejects a selection a non editable
     * combo does not carry, and rejects it silently, which would leave the box reading a family nobody chose.
     */
    @RequiredUIAccess
    private void applyRequestedValue() {
        String requested = myRequestedValue;
        if (requested == null || myLoadingItem != null) {
            return;
        }

        MyComboBox comboBox = toAWTComponent();

        Object item = findItem(comboBox, requested);
        if (item == null) {
            item = requested;
            comboBox.addItem(item);
        }

        comboBox.setSelectedItem(item);
    }

    private static @Nullable Object findItem(MyComboBox comboBox, String family) {
        for (int i = 0; i < comboBox.getItemCount(); i++) {
            Object item = comboBox.getItemAt(i);
            if (family.equals(nameOf(item))) {
                return item;
            }
        }

        return null;
    }

    /**
     * The family an item stands for, or null for the one item which stands for no family at all.
     */
    private static @Nullable String nameOf(@Nullable Object item) {
        if (item instanceof Typeface typeface) {
            return typeface.getName();
        }

        return item instanceof String family ? family : null;
    }

    @Override
    public @Nullable String getValue() {
        if (!isInitialized()) {
            return myRequestedValue;
        }

        String name = nameOf(toAWTComponent().getSelectedItem());
        return name == null ? myRequestedValue : name;
    }

    @RequiredUIAccess
    @Override
    public void setValue(@Nullable String value, boolean fireListeners) {
        myRequestedValue = value;

        if (isInitialized()) {
            myFireListeners = false;
            try {
                applyRequestedValue();
            }
            finally {
                myFireListeners = true;
            }
        }

        if (fireListeners) {
            fireListeners();
        }
    }

    @Override
    public void setMonospacedOnly(boolean monospacedOnly) {
        if (myMonospacedOnly == monospacedOnly) {
            return;
        }

        myMonospacedOnly = monospacedOnly;

        if (isInitialized()) {
            fillItems();
        }
    }

    @Override
    public boolean isMonospacedOnly() {
        return myMonospacedOnly;
    }

    @RequiredUIAccess
    @SuppressWarnings("unchecked")
    private void fireListeners() {
        getListenerDispatcher(ValueComponentEvent.class)
            .onEvent(new ValueComponentEvent(this, getValue(), DesktopAWTInputDetails.currentEvent(toAWTComponent())));
    }
}
