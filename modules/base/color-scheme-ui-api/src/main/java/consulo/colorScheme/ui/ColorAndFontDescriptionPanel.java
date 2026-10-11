/*
 * Copyright 2000-2013 JetBrains s.r.o.
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

import consulo.application.localize.ApplicationLocalize;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.EffectType;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.setting.AttributesDescriptor;
import consulo.colorScheme.setting.ColorAndFontDescriptors;
import consulo.localize.LocalizeValue;
import consulo.proxy.EventDispatcher;
import consulo.ui.CheckBox;
import consulo.ui.ColorBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.color.ColorValue;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.StandardColors;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.awt.Font;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author cdr
 */
public class ColorAndFontDescriptionPanel implements ColorDescriptionPanel {
    private static final Map<EffectType, LocalizeValue> EFFECTS = createEffects();

    private final EventDispatcher<Listener> myListeners = EventDispatcher.create(Listener.class);

    private final VerticalLayout myPanel;

    private final ColorBox myBackgroundChooser;
    private final ColorBox myForegroundChooser;
    private final ColorBox myEffectsColorChooser;
    private final ColorBox myErrorStripeColorChooser;

    private final CheckBox myCbBackground;
    private final CheckBox myCbForeground;
    private final CheckBox myCbEffects;
    private final CheckBox myCbErrorStripe;

    private final ComboBox<EffectType> myEffectsCombo;

    private final CheckBox myCbBold;
    private final CheckBox myCbItalic;

    private final CheckBox myInheritAttributesBox;
    private final Hyperlink myInheritanceLink;
    private final Label myInheritanceLabel;

    private @Nullable String myInheritancePage;
    private @Nullable String myInheritanceAttribute;

    private boolean myResetting;

    @RequiredUIAccess
    public ColorAndFontDescriptionPanel() {
        myCbBold = CheckBox.create(ApplicationLocalize.checkboxFontBold());
        myCbItalic = CheckBox.create(ApplicationLocalize.checkboxFontItalic());

        myCbForeground = CheckBox.create(ApplicationLocalize.checkboxColorForeground());
        myForegroundChooser = ColorBox.create();
        myCbBackground = CheckBox.create(ApplicationLocalize.checkboxColorBackground());
        myBackgroundChooser = ColorBox.create();
        myCbErrorStripe = CheckBox.create(ApplicationLocalize.checkboxColorErrorStripeMark());
        myErrorStripeColorChooser = ColorBox.create();
        myCbEffects = CheckBox.create(ApplicationLocalize.checkboxColorEffects());
        myEffectsColorChooser = ColorBox.create();

        myEffectsCombo = ComboBox.create(EFFECTS.keySet());
        myEffectsCombo.setTextRenderer(effect -> effect == null ? LocalizeValue.empty() : EFFECTS.getOrDefault(effect, LocalizeValue.empty()));

        myInheritAttributesBox = CheckBox.create(ApplicationLocalize.labelInheritAttributes());
        myInheritanceLink = Hyperlink.create(LocalizeValue.empty(), event -> onHyperLinkClicked());
        myInheritanceLabel = Label.create();

        HorizontalLayout fontStyle = HorizontalLayout.create(Space.LARGE);
        fontStyle.add(myCbBold);
        fontStyle.add(myCbItalic);

        myPanel = VerticalLayout.create(Space.SMALL);
        myPanel.add(DockLayout.create(Space.NONE).right(fontStyle));
        myPanel.add(row(myCbForeground, myForegroundChooser));
        myPanel.add(row(myCbBackground, myBackgroundChooser));
        myPanel.add(row(myCbErrorStripe, myErrorStripeColorChooser));
        myPanel.add(row(myCbEffects, myEffectsColorChooser));
        myPanel.add(DockLayout.create(Space.NONE).right(myEffectsCombo));
        myPanel.add(myInheritAttributesBox);
        myPanel.add(DockLayout.create(Space.NONE).right(myInheritanceLink));
        myPanel.add(DockLayout.create(Space.NONE).right(myInheritanceLabel));

        for (CheckBox checkBox : new CheckBox[]{myCbBackground, myCbForeground, myCbEffects, myCbErrorStripe, myCbItalic, myCbBold, myInheritAttributesBox}) {
            checkBox.addValueListener(event -> onSettingsChangedByUser());
        }
        for (ColorBox colorBox : new ColorBox[]{myBackgroundChooser, myForegroundChooser, myEffectsColorChooser, myErrorStripeColorChooser}) {
            colorBox.addValueListener(event -> onSettingsChangedByUser());
        }
        myEffectsCombo.addValueListener(event -> onSettingsChangedByUser());

        myInheritanceLink.setVisible(false);
        myInheritanceLabel.setVisible(false);
    }

    private static Map<EffectType, LocalizeValue> createEffects() {
        Map<EffectType, LocalizeValue> map = new LinkedHashMap<>();
        map.put(EffectType.LINE_UNDERSCORE, ApplicationLocalize.comboboxEffectUnderscored());
        map.put(EffectType.BOLD_LINE_UNDERSCORE, ApplicationLocalize.comboboxEffectBoldunderscored());
        map.put(EffectType.WAVE_UNDERSCORE, ApplicationLocalize.comboboxEffectUnderwaved());
        map.put(EffectType.BOXED, ApplicationLocalize.comboboxEffectBordered());
        map.put(EffectType.STRIKEOUT, ApplicationLocalize.comboboxEffectStrikeout());
        map.put(EffectType.BOLD_DOTTED_LINE, ApplicationLocalize.comboboxEffectBoldDottedline());
        return map;
    }

    @RequiredUIAccess
    private static Component row(CheckBox checkBox, ColorBox colorBox) {
        return DockLayout.create(Space.MEDIUM).left(checkBox).right(colorBox);
    }

    protected void onHyperLinkClicked() {
        String page = myInheritancePage;
        String attribute = myInheritanceAttribute;
        if (page != null && attribute != null) {
            myListeners.getMulticaster().onHyperLinkClicked(page, attribute);
        }
    }

    @RequiredUIAccess
    private void onSettingsChangedByUser() {
        if (myResetting) {
            return;
        }
        onSettingsChanged();
    }

    @RequiredUIAccess
    protected void onSettingsChanged() {
        myErrorStripeColorChooser.setEnabled(isChecked(myCbErrorStripe));
        myForegroundChooser.setEnabled(isChecked(myCbForeground));
        myBackgroundChooser.setEnabled(isChecked(myCbBackground));
        myEffectsColorChooser.setEnabled(isChecked(myCbEffects));
        myEffectsCombo.setEnabled(isChecked(myCbEffects));

        myListeners.getMulticaster().onSettingsChanged();
    }

    private static boolean isChecked(CheckBox checkBox) {
        return Boolean.TRUE.equals(checkBox.getValue());
    }

    @Override
    public Component getUIComponent() {
        return myPanel;
    }

    @Override
    @RequiredUIAccess
    public void resetDefault() {
        myResetting = true;
        try {
            myCbBold.setValue(false, false);
            myCbBold.setEnabled(false);
            myCbItalic.setValue(false, false);
            myCbItalic.setEnabled(false);
            updateColorChooser(myCbForeground, myForegroundChooser, false, false, null);
            updateColorChooser(myCbBackground, myBackgroundChooser, false, false, null);
            updateColorChooser(myCbErrorStripe, myErrorStripeColorChooser, false, false, null);
            updateColorChooser(myCbEffects, myEffectsColorChooser, false, false, null);
            myEffectsCombo.setEnabled(false);
            myInheritanceLink.setVisible(false);
            myInheritanceLabel.setVisible(false);
            myInheritAttributesBox.setVisible(false);
        }
        finally {
            myResetting = false;
        }
    }

    @Override
    @RequiredUIAccess
    public void reset(EditorSchemeAttributeDescriptor d) {
        if (!(d instanceof ColorAndFontDescription description)) {
            return;
        }

        myResetting = true;
        try {
            if (description.isFontEnabled()) {
                myCbBold.setEnabled(true);
                myCbItalic.setEnabled(true);
                int fontType = description.getFontType();
                myCbBold.setValue((fontType & Font.BOLD) != 0, false);
                myCbItalic.setValue((fontType & Font.ITALIC) != 0, false);
            }
            else {
                myCbBold.setValue(false, false);
                myCbBold.setEnabled(false);
                myCbItalic.setValue(false, false);
                myCbItalic.setEnabled(false);
            }

            updateColorChooser(
                myCbForeground,
                myForegroundChooser,
                description.isForegroundEnabled(),
                description.isForegroundChecked(),
                description.getForegroundColor()
            );

            updateColorChooser(
                myCbBackground,
                myBackgroundChooser,
                description.isBackgroundEnabled(),
                description.isBackgroundChecked(),
                description.getBackgroundColor()
            );

            updateColorChooser(
                myCbErrorStripe,
                myErrorStripeColorChooser,
                description.isErrorStripeEnabled(),
                description.isErrorStripeChecked(),
                description.getErrorStripeColor()
            );

            updateColorChooser(
                myCbEffects,
                myEffectsColorChooser,
                description.isEffectsColorEnabled(),
                description.isEffectsColorChecked(),
                description.getEffectColor()
            );

            if (description.isEffectsColorEnabled() && description.isEffectsColorChecked()) {
                myEffectsCombo.setEnabled(true);
                EffectType effectType = description.getEffectType();
                if (EFFECTS.containsKey(effectType)) {
                    myEffectsCombo.setValue(effectType, false);
                }
            }
            else {
                myEffectsCombo.setEnabled(false);
            }
            setInheritanceInfo(description);
        }
        finally {
            myResetting = false;
        }
    }

    @Override
    @RequiredUIAccess
    public void apply(EditorSchemeAttributeDescriptor d, EditorColorsScheme scheme) {
        if (!(d instanceof ColorAndFontDescription description)) {
            return;
        }

        description.setInherited(isChecked(myInheritAttributesBox));
        if (description.isInherited()) {
            TextAttributes baseAttributes = description.getBaseAttributes();
            if (baseAttributes != null) {
                description.setFontType(baseAttributes.getFontType());
                description.setForegroundChecked(baseAttributes.getForegroundColor() != null);
                description.setForegroundColor(baseAttributes.getForegroundColor());
                description.setBackgroundChecked(baseAttributes.getBackgroundColor() != null);
                description.setBackgroundColor(baseAttributes.getBackgroundColor());
                description.setErrorStripeChecked(baseAttributes.getErrorStripeColor() != null);
                description.setErrorStripeColor(baseAttributes.getErrorStripeColor());
                description.setEffectColor(baseAttributes.getEffectColor());
                description.setEffectType(baseAttributes.getEffectType());
                description.setEffectsColorChecked(baseAttributes.getEffectColor() != null);
            }
            else {
                description.setInherited(false);
            }
            reset(description);
        }
        else {
            setInheritanceInfo(description);
            int fontType = Font.PLAIN;
            if (isChecked(myCbBold)) {
                fontType |= Font.BOLD;
            }
            if (isChecked(myCbItalic)) {
                fontType |= Font.ITALIC;
            }
            description.setFontType(fontType);
            description.setForegroundChecked(isChecked(myCbForeground));
            description.setForegroundColor(myForegroundChooser.getValueOrError());
            description.setBackgroundChecked(isChecked(myCbBackground));
            description.setBackgroundColor(myBackgroundChooser.getValueOrError());
            description.setErrorStripeChecked(isChecked(myCbErrorStripe));
            description.setErrorStripeColor(myErrorStripeColorChooser.getValueOrError());
            description.setEffectsColorChecked(isChecked(myCbEffects));
            description.setEffectColor(myEffectsColorChooser.getValueOrError());

            EffectType effectType = myEffectsCombo.getValue();
            if (myEffectsCombo.isEnabled() && effectType != null) {
                description.setEffectType(effectType);
            }
        }
        description.apply(scheme);
    }

    @Override
    public void addListener(Listener listener) {
        myListeners.addListener(listener);
    }

    @RequiredUIAccess
    private static void updateColorChooser(CheckBox checkBox, ColorBox colorBox, boolean isEnabled, boolean isChecked, @Nullable ColorValue color) {
        checkBox.setEnabled(isEnabled);
        checkBox.setValue(isChecked, false);
        colorBox.setValue(color != null ? color : StandardColors.WHITE, false);
        colorBox.setEnabled(isChecked);
    }

    @RequiredUIAccess
    private void setInheritanceInfo(ColorAndFontDescription description) {
        Pair<ColorAndFontDescriptors, AttributesDescriptor> baseDescriptor = description.getBaseAttributeDescriptor();
        if (baseDescriptor != null) {
            LocalizeValue attrName = baseDescriptor.second.getDisplayName();
            ColorAndFontDescriptors settingsPage = baseDescriptor.first;
            if (settingsPage != null) {
                myInheritancePage = settingsPage.getDisplayName().get();
                myInheritanceAttribute = attrName.get();
                myInheritanceLink.setText(ApplicationLocalize.labelInheritAttributesFrom(attrName, settingsPage.getDisplayName()));
                myInheritanceLink.setVisible(true);
                myInheritanceLabel.setVisible(false);
            }
            else {
                myInheritancePage = null;
                myInheritanceAttribute = null;
                myInheritanceLabel.setText(attrName);
                myInheritanceLabel.setVisible(true);
                myInheritanceLink.setVisible(false);
            }

            myInheritAttributesBox.setVisible(true);
            myInheritAttributesBox.setEnabled(true);
            myInheritAttributesBox.setValue(description.isInherited(), false);
            setEditEnabled(!description.isInherited(), description);
        }
        else {
            myInheritancePage = null;
            myInheritanceAttribute = null;
            myInheritanceLink.setVisible(false);
            myInheritanceLabel.setVisible(false);
            myInheritAttributesBox.setValue(false, false);
            myInheritAttributesBox.setVisible(false);
            setEditEnabled(true, description);
        }
    }

    @RequiredUIAccess
    private void setEditEnabled(boolean isEditEnabled, ColorAndFontDescription description) {
        myCbBackground.setEnabled(isEditEnabled && description.isBackgroundEnabled());
        myCbForeground.setEnabled(isEditEnabled && description.isForegroundEnabled());
        myCbBold.setEnabled(isEditEnabled && description.isFontEnabled());
        myCbItalic.setEnabled(isEditEnabled && description.isFontEnabled());
        myCbEffects.setEnabled(isEditEnabled && description.isEffectsColorEnabled());
        myCbErrorStripe.setEnabled(isEditEnabled && description.isErrorStripeEnabled());
        myErrorStripeColorChooser.setEditable(isEditEnabled);
        myEffectsColorChooser.setEditable(isEditEnabled);
        myForegroundChooser.setEditable(isEditEnabled);
        myBackgroundChooser.setEditable(isEditEnabled);
    }
}
