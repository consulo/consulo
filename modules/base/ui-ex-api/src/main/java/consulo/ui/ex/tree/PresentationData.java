/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.ui.ex.tree;

import consulo.colorScheme.TextAttributesKey;
import consulo.component.util.ComparableObject;
import consulo.component.util.ComparableObjectCheck;
import consulo.localize.LocalizeValue;
import consulo.navigation.ItemPresentation;
import consulo.navigation.ItemPresentationWithSeparator;
import consulo.navigation.LocationPresentation;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.ColoredItemPresentation;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.image.Image;
import consulo.util.collection.Lists;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.*;
import java.util.List;

/**
 * Default implementation of the {@link ItemPresentation} interface.
 */
public class PresentationData implements ColoredItemPresentation, ComparableObject, LocationPresentation {
    protected final List<PresentableNodeDescriptor.ColoredFragment> myColoredText = Lists.newLockFreeCopyOnWriteList();

    private Image myIcon;

    private String myLocationString;
    private String myPresentableText;

    private String myTooltip;
    private TextAttributesKey myAttributesKey;

    private ColorValue myForcedTextForeground;

    private Font myFont;

    private boolean mySeparatorAbove = false;

    private boolean myChanged;
    private String myLocationPrefix;
    private String myLocationSuffix;

    /**
     * Creates an instance with the specified parameters.
     *
     * @param presentableText the name of the object to be presented in most renderers across the program.
     * @param locationString  the location of the object (for example, the package of a class). The location
     *                        string is used by some renderers and usually displayed as grayed text next to
     *                        the item name.
     * @param icon            the icon shown for the node when it is collapsed in a tree, or when it is displayed
     *                        in a non-tree view.
     * @param attributesKey   the attributes for rendering the item text.
     */
    public PresentationData(
        @Nonnull LocalizeValue presentableText,
        String locationString,
        Image icon,
        @Nullable TextAttributesKey attributesKey
    ) {
        myIcon = icon;
        myLocationString = locationString;
        myPresentableText = presentableText.get();
        myAttributesKey = attributesKey;
    }

    /**
     * Creates an instance with the specified parameters.
     *
     * @param presentableText the name of the object to be presented in most renderers across the program.
     * @param locationString  the location of the object (for example, the package of a class). The location
     *                        string is used by some renderers and usually displayed as grayed text next to
     *                        the item name.
     * @param icon            the icon shown for the node when it is collapsed in a tree, or when it is displayed
     *                        in a non-tree view.
     * @param attributesKey   the attributes for rendering the item text.
     */
    public PresentationData(String presentableText, String locationString, Image icon, @Nullable TextAttributesKey attributesKey) {
        myIcon = icon;
        myLocationString = locationString;
        myPresentableText = presentableText;
        myAttributesKey = attributesKey;
    }

    /**
     * Creates an instance with no parameters specified.
     */
    public PresentationData() {
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Nullable
    public ColorValue getForcedTextForeground() {
        return myForcedTextForeground;
    }

    public void setForcedTextForeground(@Nullable ColorValue forcedTextForeground) {
        myForcedTextForeground = forcedTextForeground;
    }

    @Override
    public String getLocationString() {
        return myLocationString;
    }

    @Override
    public String getPresentableText() {
        return myPresentableText;
    }

    public void setIcon(Image icon) {
        myIcon = icon;
    }

    /**
     * Sets the location of the object (for example, the package of a class). The location
     * string is used by some renderers and usually displayed as grayed text next to the item name.
     *
     * @param locationString the location of the object.
     */
    public void setLocationString(@Nonnull LocalizeValue locationString) {
        myLocationString = locationString.get();
    }

    /**
     * Sets the location of the object (for example, the package of a class). The location
     * string is used by some renderers and usually displayed as grayed text next to the item name.
     *
     * @param locationString the location of the object.
     */
    public void setLocationString(String locationString) {
        myLocationString = locationString;
    }

    /**
     * Sets the name of the object to be presented in most renderers across the program.
     *
     * @param presentableText the name of the object.
     */
    public void setPresentableText(@Nonnull LocalizeValue presentableText) {
        myPresentableText = presentableText.get();
    }

    /**
     * Sets the name of the object to be presented in most renderers across the program.
     *
     * @param presentableText the name of the object.
     */
    public void setPresentableText(String presentableText) {
        myPresentableText = presentableText;
    }

    /**
     * Copies the presentation parameters from the specified presentation instance.
     *
     * @param presentation the instance to copy the parameters from.
     */
    public void updateFrom(ItemPresentation presentation) {
        setIcon(presentation.getIcon());
        setPresentableText(presentation.getPresentableText());
        setLocationString(presentation.getLocationString());
        if (presentation instanceof ColoredItemPresentation coloredItemPresentation) {
            setAttributesKey(coloredItemPresentation.getTextAttributesKey());
        }
        setSeparatorAbove(presentation instanceof ItemPresentationWithSeparator);
        if (presentation instanceof LocationPresentation locationPresentation) {
            myLocationPrefix = locationPresentation.getLocationPrefix();
            myLocationSuffix = locationPresentation.getLocationSuffix();
        }
    }

    public boolean hasSeparatorAbove() {
        return mySeparatorAbove;
    }

    public void setSeparatorAbove(boolean b) {
        mySeparatorAbove = b;
    }

    @Override
    public TextAttributesKey getTextAttributesKey() {
        return myAttributesKey;
    }

    /**
     * Sets the attributes for rendering the item text.
     *
     * @param attributesKey the attributes for rendering the item text.
     */
    public void setAttributesKey(TextAttributesKey attributesKey) {
        myAttributesKey = attributesKey;
    }

    public String getTooltip() {
        return myTooltip;
    }

    public void setTooltip(@Nullable String tooltip) {
        myTooltip = tooltip;
    }

    public boolean isChanged() {
        return myChanged;
    }

    public void setChanged(boolean changed) {
        myChanged = changed;
    }

    @Nonnull
    public List<PresentableNodeDescriptor.ColoredFragment> getColoredText() {
        return myColoredText;
    }

    public void addText(PresentableNodeDescriptor.ColoredFragment coloredFragment) {
        myColoredText.add(coloredFragment);
    }

    public void addText(String text, SimpleTextAttributes attributes) {
        myColoredText.add(new PresentableNodeDescriptor.ColoredFragment(text, attributes));
    }

    public void clearText() {
        myColoredText.clear();
    }

    public void clear() {
        myIcon = null;
        clearText();
        myAttributesKey = null;
        myFont = null;
        myForcedTextForeground = null;
        myLocationString = null;
        myPresentableText = null;
        myTooltip = null;
        myChanged = false;
        mySeparatorAbove = false;
        myLocationSuffix = null;
        myLocationPrefix = null;
    }

    @Override
    @Nonnull
    public Object[] getEqualityObjects() {
        return new Object[]{myIcon, myColoredText, myAttributesKey, myFont, myForcedTextForeground, myPresentableText, myLocationString, mySeparatorAbove, myLocationPrefix, myLocationSuffix};
    }

    @Override
    public int hashCode() {
        return ComparableObjectCheck.hashCode(this, super.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        return ComparableObjectCheck.equals(this, obj);
    }

    public void copyFrom(PresentationData from) {
        if (from == this) {
            return;
        }
        myAttributesKey = from.myAttributesKey;
        myIcon = from.myIcon;
        clearText();
        myColoredText.addAll(from.myColoredText);
        myFont = from.myFont;
        myForcedTextForeground = from.myForcedTextForeground;
        myLocationString = from.myLocationString;
        myPresentableText = from.myPresentableText;
        myTooltip = from.myTooltip;
        mySeparatorAbove = from.mySeparatorAbove;
        myLocationPrefix = from.myLocationPrefix;
        myLocationSuffix = from.myLocationSuffix;
    }

    @Override
    public PresentationData clone() {
        PresentationData clone = new PresentationData();
        clone.copyFrom(this);
        return clone;
    }

    public void applyFrom(PresentationData from) {
        myAttributesKey = getValue(myAttributesKey, from.myAttributesKey);
        myIcon = getValue(myIcon, from.myIcon);

        if (myColoredText.isEmpty()) {
            myColoredText.addAll(from.myColoredText);
        }

        myFont = getValue(myFont, from.myFont);
        myForcedTextForeground = getValue(myForcedTextForeground, from.myForcedTextForeground);
        myLocationString = getValue(myLocationString, from.myLocationString);
        myPresentableText = getValue(myPresentableText, from.myPresentableText);
        myTooltip = getValue(myTooltip, from.myTooltip);
        mySeparatorAbove = mySeparatorAbove || from.mySeparatorAbove;
        myLocationPrefix = getValue(myLocationPrefix, from.myLocationPrefix);
        myLocationSuffix = getValue(myLocationSuffix, from.myLocationSuffix);
    }

    private static <T> T getValue(T ownValue, T fromValue) {
        return ownValue != null ? ownValue : fromValue;
    }

    @Override
    public String getLocationPrefix() {
        return myLocationPrefix == null ? LocationPresentation.DEFAULT_LOCATION_PREFIX : myLocationPrefix;
    }

    @Override
    public String getLocationSuffix() {
        return myLocationSuffix == null ? LocationPresentation.DEFAULT_LOCATION_SUFFIX : myLocationSuffix;
    }
}
