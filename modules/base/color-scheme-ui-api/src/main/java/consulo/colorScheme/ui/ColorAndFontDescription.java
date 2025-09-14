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

package consulo.colorScheme.ui;

import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptors;
import consulo.localize.LocalizeValue;
import consulo.ui.color.ColorValue;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public abstract class ColorAndFontDescription extends TextAttributes implements EditorSchemeAttributeDescriptor {
    private final LocalizeValue myName;
    private final LocalizeValue myGroup;
    private final String myType;
    private final Image myIcon;
    private final String myToolTip;
    private final EditorColorsScheme myScheme;
    private boolean isForegroundChecked;
    private boolean isBackgroundChecked;
    private boolean isEffectsColorChecked;
    private boolean isErrorStripeChecked;
    private boolean isInherited;

    public ColorAndFontDescription(LocalizeValue name, LocalizeValue group, String type, EditorColorsScheme scheme, Image icon, String toolTip) {
        myName = name;
        myGroup = group;
        myType = type;
        myScheme = scheme;
        myIcon = icon;
        myToolTip = toolTip;
    }

    @Override
    public String toString() {
        return myName.get();
    }

    @Nonnull
    public LocalizeValue getName() {
        return myName;
    }

    @Nonnull
    @Override
    public LocalizeValue getGroup() {
        return myGroup;
    }

    @Override
    public String getType() {
        return myType;
    }

    @Override
    public EditorColorsScheme getScheme() {
        return myScheme;
    }

    public Image getIcon() {
        return myIcon;
    }

    public String getToolTip() {
        return myToolTip;
    }

    protected void initCheckedStatus() {
        isForegroundChecked = getExternalForeground() != null;
        isBackgroundChecked = getExternalBackground() != null;
        isErrorStripeChecked = getExternalErrorStripe() != null;
        isEffectsColorChecked = getExternalEffectColor() != null;
        super.setForegroundColor(getExternalForeground());
        super.setBackgroundColor(getExternalBackground());
        super.setEffectColor(getExternalEffectColor());
        super.setEffectType(getExternalEffectType());
        super.setErrorStripeColor(getExternalErrorStripe());
    }

    public abstract ColorValue getExternalForeground();

    public abstract ColorValue getExternalBackground();

    public abstract ColorValue getExternalErrorStripe();

    public abstract ColorValue getExternalEffectColor();

    public abstract EffectType getExternalEffectType();

    public abstract void setExternalForeground(ColorValue col);

    public abstract void setExternalBackground(ColorValue col);

    public abstract void setExternalErrorStripe(ColorValue col);

    public abstract void setExternalEffectColor(ColorValue color);

    public abstract void setExternalEffectType(EffectType type);

    @Override
    public final void setForegroundColor(ColorValue col) {
        super.setForegroundColor(col);
        if (isForegroundChecked) {
            setExternalForeground(col);
        }
        else {
            setExternalForeground(null);
        }
    }

    @Override
    public final void setBackgroundColor(ColorValue col) {
        super.setBackgroundColor(col);
        if (isBackgroundChecked) {
            setExternalBackground(col);
        }
        else {
            setExternalBackground(null);
        }
    }

    @Override
    public void setErrorStripeColor(ColorValue color) {
        super.setErrorStripeColor(color);
        if (isErrorStripeChecked) {
            setExternalErrorStripe(color);
        }
        else {
            setExternalErrorStripe(null);
        }
    }

    @Override
    public final void setEffectColor(ColorValue col) {
        super.setEffectColor(col);
        if (isEffectsColorChecked) {
            setExternalEffectColor(col);
        }
        else {
            setExternalEffectColor(null);
        }
    }

    @Override
    public final void setEffectType(EffectType effectType) {
        super.setEffectType(effectType);
        setExternalEffectType(effectType);
    }

    public boolean isForegroundChecked() {
        return isForegroundChecked;
    }

    public boolean isBackgroundChecked() {
        return isBackgroundChecked;
    }

    public boolean isErrorStripeChecked() {
        return isErrorStripeChecked;
    }

    public boolean isEffectsColorChecked() {
        return isEffectsColorChecked;
    }

    public final void setForegroundChecked(boolean val) {
        isForegroundChecked = val;
        setForegroundColor(getForegroundColor());
    }

    public final void setBackgroundChecked(boolean val) {
        isBackgroundChecked = val;
        setBackgroundColor(getBackgroundColor());
    }

    public final void setErrorStripeChecked(boolean val) {
        isErrorStripeChecked = val;
        setErrorStripeColor(getErrorStripeColor());
    }

    public final void setEffectsColorChecked(boolean val) {
        isEffectsColorChecked = val;
        setEffectColor(getEffectColor());
        setEffectType(getEffectType());
    }

    @Override
    public abstract int getFontType();

    @Override
    public abstract void setFontType(int type);

    public boolean isFontEnabled() {
        return true;
    }

    public boolean isForegroundEnabled() {
        return true;
    }

    public boolean isBackgroundEnabled() {
        return true;
    }

    public boolean isErrorStripeEnabled() {
        return false;
    }

    public boolean isEffectsColorEnabled() {
        return true;
    }

    @Override
    public boolean isModified() {
        return false;
    }

    public boolean isInherited() {
        return isInherited;
    }

    public void setInherited(boolean isInherited) {
        this.isInherited = isInherited;
    }

    @Nullable
    public TextAttributes getBaseAttributes() {
        return null;
    }

    @Nullable
    public Pair<ColorAndFontDescriptors, AttributesDescriptor> getBaseAttributeDescriptor() {
        return null;
    }
}