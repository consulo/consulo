/*
 * Copyright 2013-2024 consulo.io
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
package consulo.execution.impl.internal.console;

import consulo.execution.ExecutionBundle;
import consulo.localize.LocalizeManager;
import consulo.ui.ex.awt.CollectionComboBoxModel;
import consulo.ui.ex.awt.ColoredListCellRenderer;
import consulo.ui.ex.awt.ComboBox;
import consulo.util.io.CharsetToolkit;
import consulo.virtualFileSystem.encoding.ApplicationEncodingManager;
import consulo.virtualFileSystem.encoding.EncodingReference;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author VISTALL
 * @since 2024-11-17
 */
public class ConsoleEncodingComboBox extends ComboBox<ConsoleEncodingComboBox.EncodingItem> {
    public static abstract class EncodingItem {
        public abstract String getDisplayName();
    }

    public static class CharsetItem extends EncodingItem {
        private final EncodingReference myReference;

        public CharsetItem(EncodingReference reference) {
            myReference = reference;
        }

        public CharsetItem(Charset charset) {
            this(new EncodingReference(charset));
        }

        @Override
        public String getDisplayName() {
            Locale locale = LocalizeManager.get().getLocale();
            
            Charset charset = myReference.getCharset();
            if (charset == null) {
                return ExecutionBundle.message("encoding.name.system.default", CharsetToolkit.getDefaultSystemCharset().displayName(locale));
            } else {
                return charset.displayName(locale);
            }
        }

        @Override
        public String toString() {
            return getDisplayName();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CharsetItem that = (CharsetItem) o;
            return Objects.equals(myReference, that.myReference);
        }

        @Override
        public int hashCode() {
            return Objects.hash(myReference);
        }
    }

    public static class SeparatorItem extends EncodingItem {
        private final String myText;

        public SeparatorItem(String text) {
            myText = text;
        }

        @Override
        public String getDisplayName() {
            return myText;
        }

        @Override
        public String toString() {
            return myText;
        }
    }

    private static final SeparatorItem FAVORITES = new SeparatorItem(ExecutionBundle.message("combobox.console.favorites.separator.label"));
    private static final SeparatorItem MORE = new SeparatorItem(ExecutionBundle.message("combobox.console.more.separator.label"));

    private static final CharsetItem DEFAULT = new CharsetItem(EncodingReference.DEFAULT);

    public ConsoleEncodingComboBox() {
        setRenderer(new ColoredListCellRenderer<EncodingItem>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList list, EncodingItem value, int index, boolean selected, boolean hasFocus) {
                if (value == null) {
                    append("");
                }
                else if (value instanceof SeparatorItem separatorItem) {
                    setSeparator(separatorItem.getDisplayName());
                }
                else if (value instanceof CharsetItem charsetItem) {
                    append(charsetItem.getDisplayName());
                }
            }
        });
    }

    public void reset(ApplicationEncodingManager manager, EncodingReference reference) {
        List<EncodingItem> items = new ArrayList<>();
        items.add(DEFAULT);

        Collection<Charset> favorites = manager.getFavorites();
        if (!favorites.isEmpty()) {
            items.add(FAVORITES);
            favorites.forEach(charset -> items.add(new CharsetItem(charset)));
        }

        SortedMap<String, Charset> map = Charset.availableCharsets();
        if (!map.isEmpty()) {
            items.add(MORE);
            map.values().forEach(charset -> items.add(new CharsetItem(charset)));
        }

        setModel(new CollectionComboBoxModel<>(items));

        setSelectedItem(new CharsetItem(reference));
    }

    @Nonnull
    public EncodingReference getSelectedEncodingReference() {
        Object selectedItem = getSelectedItem();
        if (selectedItem instanceof CharsetItem charsetItem) {
            return charsetItem.myReference;
        }

        return EncodingReference.DEFAULT;
    }

    @Override
    public void setSelectedItem(Object anObject) {
        if (!(anObject instanceof SeparatorItem)) {
            super.setSelectedItem(anObject);
        }
    }

    @Override
    public void setSelectedIndex(final int anIndex) {
        Object item = getItemAt(anIndex);
        if (!(item instanceof SeparatorItem)) {
            super.setSelectedIndex(anIndex);
        }
    }
}
