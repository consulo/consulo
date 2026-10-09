/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ui.ex.util;

import consulo.annotation.DeprecationInfo;
import consulo.colorScheme.TextAttributes;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.ColoredTextContainer;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.UIAccess;
import consulo.ui.font.Font;
import consulo.ui.image.Image;

import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

// todo: move to lang-impl ?
public class CompositeAppearance implements ModifiableCellAppearanceEx {
    private static final Logger LOG = Logger.getInstance(CompositeAppearance.class);

    private Image myIcon;
    private final List<TextSection> mySections = new ArrayList<>();
    private int myInsertionIndex = 0;

    @Override
    public void customize(ColoredTextContainer component) {
        synchronized (mySections) {
            for (TextSection section : mySections) {
                TextAttributes attributes = section.getTextAttributes();
                component.append(section.getText(), TextAttributesUtil.fromTextAttributes(attributes));
            }
            component.setIcon(myIcon);
        }
    }

    @Override
    public Image getIcon() {
        synchronized (mySections) {
            return myIcon;
        }
    }

    @Override
    public void setIcon(@Nullable Image icon) {
        synchronized (mySections) {
            myIcon = icon;
        }
    }

    @Override
    public String getText() {
        synchronized (mySections) {
            StringBuilder buffer = new StringBuilder();
            for (TextSection section : mySections) {
                buffer.append(section.myText.get());
            }
            return buffer.toString();
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        synchronized (mySections) {
            if (!(obj instanceof CompositeAppearance that)) {
                return false;
            }
            if (UIAccess.isUIThread()) {
                return that.mySections.equals(mySections);
            }
            else {
                return new ArrayList<>(mySections).equals(new ArrayList<>(that.mySections));
            }
        }
    }

    @Override
    public int hashCode() {
        return getText().hashCode();
    }

    protected void addSectionAt(int index, TextSection section) {
        synchronized (mySections) {
            mySections.add(index, section);
            for (Iterator<TextSection> iterator = mySections.iterator(); iterator.hasNext(); ) {
                TextSection textSection = iterator.next();
                if (textSection == null) {
                    LOG.error("index: " + index + " size: " + mySections.size());
                    iterator.remove();
                }
            }
        }
    }

    public DequeEnd getBeginning() {
        return new DequeBeginning();
    }

    public DequeEnd getEnding() {
        return new DequeEnding();
    }

    public DequeEnd getSuffix() {
        return new DequeSuffix();
    }

    public static CompositeAppearance textComment(LocalizeValue text, LocalizeValue comment) {
        DequeEnd ending = new CompositeAppearance().getEnding();
        ending.addText(text);
        ending.addComment(comment);
        return ending.getAppearance();
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public static CompositeAppearance textComment(String text, String comment) {
        return textComment(LocalizeValue.ofNullable(text), LocalizeValue.ofNullable(comment));
    }

    public static CompositeAppearance single(LocalizeValue text, SimpleTextAttributes textAttributes) {
        CompositeAppearance result = new CompositeAppearance();
        result.getEnding().addText(text, textAttributes);
        return result;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public static CompositeAppearance single(@Nullable String text, SimpleTextAttributes textAttributes) {
        return single(LocalizeValue.ofNullable(text), textAttributes);
    }

    public static CompositeAppearance single(LocalizeValue text) {
        return single(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public static CompositeAppearance single(@Nullable String text) {
        return single(LocalizeValue.ofNullable(text));
    }

    public static CompositeAppearance invalid(LocalizeValue absolutePath) {
        CompositeAppearance appearance = new CompositeAppearance();
        appearance.setIcon(PlatformIconGroup.nodesPpinvalid());
        appearance.getEnding().addText(absolutePath, SimpleTextAttributes.ERROR_ATTRIBUTES);
        return appearance;
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    public static CompositeAppearance invalid(@Nullable String absolutePath) {
        return invalid(LocalizeValue.ofNullable(absolutePath));
    }

    public Iterator<TextSection> getSectionsIterator() {
        return mySections.iterator();
    }

    public static class TextSection {
        private static final TextAttributes DEFAULT_TEXT_ATTRIBUTES = new TextAttributes(null, null, null, null, Font.PLAIN);
        private LocalizeValue myText;
        private TextAttributes myAttributes;

        public TextSection(LocalizeValue text, TextAttributes attributes) {
            myAttributes = attributes == null ? DEFAULT_TEXT_ATTRIBUTES : attributes;
            myText = text;
        }

        public LocalizeValue getText() {
            return myText;
        }

        public TextAttributes getTextAttributes() {
            return myAttributes;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj == this
                || obj instanceof TextSection that && that.myAttributes.equals(myAttributes) && that.myText.equals(myText);
        }

        @Override
        public int hashCode() {
            return myText.hashCode();
        }
    }

    public abstract class DequeEnd {
        public void addText(LocalizeValue text, SimpleTextAttributes textAttributes) {
            addText(text, TextAttributesUtil.toTextAttributes(textAttributes));
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @SuppressWarnings("deprecation")
        public void addText(@Nullable String text, SimpleTextAttributes textAttributes) {
            addText(text, TextAttributesUtil.toTextAttributes(textAttributes));
        }

        public void addText(LocalizeValue text) {
            addText(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @SuppressWarnings("deprecation")
        public void addText(@Nullable String text) {
            addText(text, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        public abstract void addSection(TextSection section);

        public void addText(LocalizeValue text, TextAttributes attributes) {
            addSection(new TextSection(text, attributes));
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public void addText(@Nullable String text, TextAttributes attributes) {
            addSection(new TextSection(LocalizeValue.ofNullable(text), attributes));
        }

        public void addSurrounded(LocalizeValue text, LocalizeValue prefix, LocalizeValue suffix, SimpleTextAttributes textAttributes) {
            if (text.isNotEmpty()) {
                addText(LocalizeValue.join(prefix, text, suffix), textAttributes);
            }
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @SuppressWarnings("deprecation")
        public void addSurrounded(@Nullable String text, String prefix, String suffix, SimpleTextAttributes textAttributes) {
            if (!StringUtil.isEmptyOrSpaces(text)) {
                addText(prefix + text + suffix, textAttributes);
            }
        }

        public CompositeAppearance getAppearance() {
            return CompositeAppearance.this;
        }

        public void addComment(LocalizeValue comment, SimpleTextAttributes commentAttributes) {
            addSurrounded(comment, LocalizeValue.of(" ("), LocalizeValue.of(")"), commentAttributes);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        @SuppressWarnings("deprecation")
        public void addComment(@Nullable String comment, SimpleTextAttributes commentAttributes) {
            addSurrounded(comment, " (", ")", commentAttributes);
        }

        public void addComment(LocalizeValue comment) {
            addComment(comment, SimpleTextAttributes.GRAY_ATTRIBUTES);
        }

        @Deprecated
        @DeprecationInfo("Use variant with LocalizeValue")
        public void addComment(@Nullable String comment) {
            addComment(LocalizeValue.ofNullable(comment));
        }
    }

    private class DequeBeginning extends DequeEnd {
        @Override
        public void addSection(TextSection section) {
            synchronized (mySections) {
                addSectionAt(0, section);
                myInsertionIndex++;
            }
        }
    }

    private class DequeEnding extends DequeEnd {
        @Override
        public void addSection(TextSection section) {
            synchronized (mySections) {
                addSectionAt(myInsertionIndex, section);
                myInsertionIndex++;
            }
        }
    }

    private class DequeSuffix extends DequeEnd {
        @Override
        public void addSection(TextSection section) {
            synchronized (mySections) {
                addSectionAt(mySections.size(), section);
            }
        }
    }
}
