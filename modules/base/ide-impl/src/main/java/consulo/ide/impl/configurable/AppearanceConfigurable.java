/*
 * Copyright 2013-2020 consulo.io
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
package consulo.ide.impl.configurable;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.localize.ApplicationLocalize;
import consulo.application.ui.UIFontManager;
import consulo.application.ui.UISettings;
import consulo.codeEditor.EditorFactory;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.configurable.ApplicationConfigurable;
import consulo.configurable.ConfigurationException;
import consulo.configurable.SimpleConfigurableByProperties;
import consulo.configurable.StandardConfigurableIds;
import consulo.configurable.search.AfterOptionChange;
import consulo.disposer.Disposable;
import consulo.ide.localize.IdeLocalize;
import consulo.localize.LocalizeValue;
import consulo.ui.AntialiasingType;
import consulo.ui.CheckBox;
import consulo.ui.ComboBox;
import consulo.ui.Component;
import consulo.ui.FontBox;
import consulo.ui.StaticPosition;
import consulo.ui.TextBoxWithHistory;
import consulo.ui.TextItemRenderer;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.IconLibrary;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.LabeledLayout;
import consulo.ui.layout.TableLayout;
import consulo.ui.layout.VerticalLayout;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.ObjectUtil;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author VISTALL
 * @since 2020-08-23
 */
@ExtensionImpl
public class AppearanceConfigurable extends SimpleConfigurableByProperties implements ApplicationConfigurable {
    private static final List<String> STANDARD_FONT_SIZES =
        List.of("8", "9", "10", "11", "12", "14", "16", "18", "20", "22", "24", "26", "28", "36", "48", "72");

    private final Provider<UISettings> myUISettings;
    private final Provider<UIFontManager> myUIFontManager;
    private final Provider<EditorFactory> myEditorFactory;
    private final Provider<EditorColorsManager> myEditorColorsManager;

    /**
     * The style when the page was opened or last applied - a style only previewed goes back to it.
     */
    private @Nullable Style myInitialStyle;
    /**
     * Above zero while the page sets the style box itself, which is not the user previewing a style.
     */
    private final AtomicInteger myEventBlocker = new AtomicInteger();
    /**
     * Set by the settings applied which need the whole UI painted again.
     */
    private boolean myRefreshUI;

    @Inject
    public AppearanceConfigurable(Provider<UISettings> uiSettings,
                                  Provider<UIFontManager> uiFontManager,
                                  Provider<EditorFactory> editorFactory,
                                  Provider<EditorColorsManager> editorColorsManager) {
        myUISettings = uiSettings;
        myUIFontManager = uiFontManager;
        myEditorFactory = editorFactory;
        myEditorColorsManager = editorColorsManager;
    }

    @Override
    public String getId() {
        return "appearance";
    }

    @Override
    public @Nullable String getParentId() {
        return StandardConfigurableIds.GENERAL_GROUP;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return IdeLocalize.titleAppearance();
    }

    @RequiredUIAccess
    @Override
    protected Component createLayout(PropertyBuilder propertyBuilder, Disposable uiDisposable) {
        UISettings settings = myUISettings.get();
        UIFontManager uiFontManager = myUIFontManager.get();
        StyleManager styleManager = StyleManager.get();

        VerticalLayout panel = VerticalLayout.create();

        VerticalLayout uiOptions = VerticalLayout.create();

        CheckBox cycleScrolling = CheckBox.create(IdeLocalize.checkbooxCyclicScrollingInLists());
        propertyBuilder.add(cycleScrolling, settings::getCycleScrolling, settings::setCycleScrolling);
        uiOptions.add(cycleScrolling);

        CheckBox iconsInQuickNavigation = CheckBox.create(IdeLocalize.checkboxShowIconsInQuickNavigation());
        propertyBuilder.add(iconsInQuickNavigation, settings::getShowIconInQuickNavigation, settings::setShowIconInQuickNavigation);
        uiOptions.add(iconsInQuickNavigation);

        CheckBox moveMouseOnDefaultButton = CheckBox.create(IdeLocalize.checkboxPositionCursorOnDefaultButton());
        propertyBuilder.add(moveMouseOnDefaultButton, settings::getMoveMouseOnDefaultButton, settings::setMoveMouseOnDefaultButton);
        uiOptions.add(moveMouseOnDefaultButton);

        CheckBox hideNavigationPopups = CheckBox.create(IdeLocalize.ideHideNavigationOnFocusLossDescription());
        propertyBuilder.add(hideNavigationPopups, settings::getHideNavigationOnFocusLoss, settings::setHideNavigationOnFocusLoss);
        uiOptions.add(hideNavigationPopups);

        CheckBox altDnd = CheckBox.create(IdeLocalize.dndWithAltPressedOnly());
        propertyBuilder.add(altDnd, settings::getDndWithPressedAltOnly, settings::setDndWithPressedAltOnly);
        uiOptions.add(altDnd);

        ComboBox<Style> styleBox = ComboBox.create(styleManager.getStyles());
        styleBox.setTextRenderer(style -> style == null ? LocalizeValue.empty() : LocalizeValue.of(style.getName()));
        // the style is previewed as soon as it is chosen
        styleBox.addValueListener(event -> {
            if (myEventBlocker.get() == 0 && event.getValue() != null) {
                styleManager.setCurrentStyle(event.getValue());
            }
        });
        propertyBuilder.add(
            styleBox::getValue,
            style -> {
                myEventBlocker.incrementAndGet();
                try {
                    styleBox.setValue(style);
                }
                finally {
                    myEventBlocker.decrementAndGet();
                }
            },
            styleManager::getCurrentStyle,
            style -> {
                if (style != null && style != styleManager.getCurrentStyle()) {
                    styleManager.setCurrentStyle(style);
                    myRefreshUI = true;
                }
            }
        );
        uiOptions.add(LabeledBuilder.simple(IdeLocalize.comboboxLookAndFeel(), styleBox));

        List<Object> iconThemes = new ArrayList<>();
        iconThemes.add(ObjectUtil.NULL);
        iconThemes.addAll(IconLibraryManager.get().getLibraries().values());
        ComboBox<Object> iconThemeBox = ComboBox.create(iconThemes);
        iconThemeBox.setRenderer((renderer, renderItem) -> {
            Object item = renderItem.getValue();
            if (item == ObjectUtil.NULL) {
                renderer.append(IdeLocalize.comboboxIconThemeUiDefault());
            }
            else if (item instanceof IconLibrary library) {
                renderer.append(library.getName());
            }
        });
        propertyBuilder.add(iconThemeBox, AppearanceConfigurable::getActiveIconLibraryOrNull, iconLibrary -> {
            if (!Objects.equals(iconLibrary, getActiveIconLibraryOrNull())) {
                IconLibraryManager.get().setActiveLibrary(iconLibrary instanceof IconLibrary library ? library.getId() : null);
                myRefreshUI = true;
            }
        });
        uiOptions.add(LabeledBuilder.simple(IdeLocalize.comboboxIconTheme(), iconThemeBox));

        CheckBox overrideFonts = CheckBox.create(IdeLocalize.checkboxOverrideDefaultLafFonts());
        FontBox fontBox = FontBox.create();
        TextBoxWithHistory fontSizeBox = TextBoxWithHistory.create().setHistory(STANDARD_FONT_SIZES);
        overrideFonts.addValueListener(event -> {
            fontBox.setEnabled(event.getValue());
            fontSizeBox.setEnabled(event.getValue());
        });
        fontBox.setEnabled(false);
        fontSizeBox.setEnabled(false);
        propertyBuilder.add(overrideFonts, uiFontManager::isOverrideFont, override -> {
            if (override != uiFontManager.isOverrideFont()) {
                uiFontManager.setOverrideFont(override);
                myRefreshUI = true;
            }
        });
        propertyBuilder.add(fontBox, uiFontManager::getFontName, fontName -> {
            if (fontName != null && !fontName.equals(uiFontManager.getFontName())) {
                uiFontManager.setFontName(fontName);
                myRefreshUI = true;
            }
        });
        propertyBuilder.add(fontSizeBox, () -> Integer.toString(uiFontManager.getFontSize()), text -> {
            int fontSize = getIntValue(text, uiFontManager.getFontSize());
            if (fontSize != uiFontManager.getFontSize()) {
                uiFontManager.setFontSize(fontSize);
                myRefreshUI = true;
            }
        });
        HorizontalLayout customFontLine = HorizontalLayout.create();
        customFontLine.add(overrideFonts);
        customFontLine.add(fontBox);
        customFontLine.add(LabeledBuilder.simple(IdeLocalize.labelFontSize(), fontSizeBox));
        uiOptions.add(customFontLine);

        panel.add(LabeledLayout.create(IdeLocalize.groupUiOptions(), uiOptions));

        TableLayout antialiasingPanel = TableLayout.create(StaticPosition.CENTER);

        ComboBox<AntialiasingType> antialiasingInIde = ComboBox.create(AntialiasingType.values());
        antialiasingInIde.setRenderer(buildItemRenderer(false));
        propertyBuilder.add(antialiasingInIde, () -> settings.IDE_AA_TYPE, type -> {
            if (type != null && type != settings.IDE_AA_TYPE) {
                settings.IDE_AA_TYPE = type;
                styleManager.refreshAntialiasingType(type);
                myRefreshUI = true;
            }
        });
        antialiasingPanel.add(
            LabeledBuilder.simple(IdeLocalize.labelTextAntialiasingScopeIde(), antialiasingInIde),
            TableLayout.cell(0, 0).fill()
        );

        ComboBox<AntialiasingType> antialiasingInEditor = ComboBox.create(AntialiasingType.values());
        antialiasingInEditor.setRenderer(buildItemRenderer(true));
        propertyBuilder.add(antialiasingInEditor, () -> settings.EDITOR_AA_TYPE, type -> {
            if (type != null && type != settings.EDITOR_AA_TYPE) {
                settings.EDITOR_AA_TYPE = type;
                myRefreshUI = true;
            }
        });
        antialiasingPanel.add(
            LabeledBuilder.simple(IdeLocalize.labelTextAntialiasingScopeEditor(), antialiasingInEditor),
            TableLayout.cell(0, 1).fill()
        );

        panel.add(LabeledLayout.create(IdeLocalize.groupAntialiasingMode(), antialiasingPanel));

        TableLayout windowOptions = TableLayout.create(StaticPosition.CENTER);
        VerticalLayout leftWindowOptions = VerticalLayout.create();
        VerticalLayout rightWindowOptions = VerticalLayout.create();
        windowOptions.add(leftWindowOptions, TableLayout.cell(0, 0).fill());
        windowOptions.add(rightWindowOptions, TableLayout.cell(0, 1).fill());

        CheckBox animateWindows = CheckBox.create(IdeLocalize.checkboxAnimateWindows());
        propertyBuilder.add(animateWindows, settings::getAnimateWindows, settings::setAnimateWindows);
        leftWindowOptions.add(animateWindows);

        CheckBox disableMnemonics = CheckBox.create(KeyMapLocalize.disableMnemonicInMenuCheckBox());
        propertyBuilder.add(disableMnemonics, settings::getDisableMnemonics, settings::setDisableMnemonics);
        leftWindowOptions.add(disableMnemonics);

        CheckBox disableMnemonicsInControls = CheckBox.create(KeyMapLocalize.disableMnemonicInControlsCheckBox());
        propertyBuilder.add(disableMnemonicsInControls, settings::getDisableMnemonicsInControls, settings::setDisableMnemonicsInControls);
        leftWindowOptions.add(disableMnemonicsInControls);

        CheckBox iconsInMenu = CheckBox.create(IdeLocalize.checkboxShowIconsInMenuItems());
        propertyBuilder.add(iconsInMenu, settings::getShowIconsInMenus, settings::setShowIconsInMenus);
        leftWindowOptions.add(iconsInMenu);

        CheckBox editorTooltip = CheckBox.create(IdeLocalize.checkboxShowEditorPreviewPopup());
        propertyBuilder.add(editorTooltip, settings::getShowEditorToolTip, settings::setShowEditorToolTip);
        leftWindowOptions.add(editorTooltip);

        CheckBox allowMergeButtons = CheckBox.create(IdeLocalize.allowMergingDialogButtons());
        propertyBuilder.add(allowMergeButtons, settings::getAllowMergeButtons, settings::setAllowMergeButtons);
        rightWindowOptions.add(allowMergeButtons);

        CheckBox smallLabelsOnTabs = CheckBox.create(IdeLocalize.smallLabelsInEditorTabs());
        propertyBuilder.add(smallLabelsOnTabs, settings::getUseSmallLabelsOnTabs, settings::setUseSmallLabelsOnTabs);
        rightWindowOptions.add(smallLabelsOnTabs);

        CheckBox smoothScrolling = CheckBox.create(ApplicationLocalize.checkboxSmoothScrolling());
        propertyBuilder.add(smoothScrolling, settings::getSmoothScrolling, settings::setSmoothScrolling);
        rightWindowOptions.add(smoothScrolling);

        panel.add(LabeledLayout.create(IdeLocalize.groupWindowOptions(), windowOptions));

        TextBoxWithHistory presentationFontSize = TextBoxWithHistory.create().setHistory(STANDARD_FONT_SIZES);
        propertyBuilder.add(presentationFontSize, () -> Integer.toString(settings.PRESENTATION_MODE_FONT_SIZE), text -> {
            int fontSize = getIntValue(text, settings.PRESENTATION_MODE_FONT_SIZE);
            if (fontSize != settings.PRESENTATION_MODE_FONT_SIZE) {
                settings.PRESENTATION_MODE_FONT_SIZE = fontSize;
                myRefreshUI = true;
            }
        });
        VerticalLayout presentationOptions = VerticalLayout.create();
        presentationOptions.add(LabeledBuilder.simple(IdeLocalize.labelFontSize(), presentationFontSize));
        panel.add(LabeledLayout.create(IdeLocalize.groupPresentationMode(), presentationOptions));

        return panel;
    }

    @RequiredUIAccess
    @Override
    protected boolean isModified(LayoutWrapper component) {
        // a previewed style is already current - it is a change while it is not the one the page started with
        return super.isModified(component) || myInitialStyle != null && myInitialStyle != StyleManager.get().getCurrentStyle();
    }

    @RequiredUIAccess
    @Override
    protected void reset(LayoutWrapper component) {
        StyleManager styleManager = StyleManager.get();
        if (myInitialStyle == null) {
            myInitialStyle = styleManager.getCurrentStyle();
        }
        else if (myInitialStyle != styleManager.getCurrentStyle()) {
            styleManager.setCurrentStyle(myInitialStyle);
        }

        super.reset(component);
    }

    @RequiredUIAccess
    @Override
    protected void apply(LayoutWrapper component) throws ConfigurationException {
        boolean modified = isModified(component);

        myRefreshUI = myInitialStyle != StyleManager.get().getCurrentStyle();
        super.apply(component);
        boolean refreshUI = myRefreshUI;
        myRefreshUI = false;
        myInitialStyle = StyleManager.get().getCurrentStyle();

        UISettings settings = myUISettings.get();
        UIAccess.current().give(() -> {
            if (refreshUI) {
                StyleManager.get().forceRepaintAll();
            }
            if (modified) {
                settings.fireUISettingsChanged();
            }
            myEditorFactory.get().refreshAllEditors();
        });
    }

    /**
     * An option switched from Find Action - the UI follows the settings.
     */
    @AfterOptionChange
    static void optionChanged(UISettings settings) {
        settings.fireUISettingsChanged();
    }

    @RequiredUIAccess
    @Override
    protected void disposeUIResources(LayoutWrapper component) {
        // a style previewed but not applied goes back
        StyleManager styleManager = StyleManager.get();
        if (myInitialStyle != null && myInitialStyle != styleManager.getCurrentStyle()) {
            styleManager.setCurrentStyle(myInitialStyle);
        }
        myInitialStyle = null;

        super.disposeUIResources(component);
    }

    private TextItemRenderer<AntialiasingType> buildItemRenderer(boolean editor) {
        return (renderer, renderItem) -> {
            AntialiasingType item = renderItem.getValue();
            if (item == null) {
                return;
            }

            renderer.withAntialiasingType(item);

            if (editor) {
                EditorColorsScheme scheme = myEditorColorsManager.get().getGlobalScheme();
                renderer.withFont(FontManager.get().createFont(scheme.getEditorFontName(), scheme.getEditorFontSize(), Font.PLAIN));
            }

            renderer.append(textForAntialiasingType(item));
        };
    }

    private static LocalizeValue textForAntialiasingType(AntialiasingType type) {
        return switch (type) {
            case SUBPIXEL -> LocalizeValue.localizeTODO("Subpixel");
            case GREYSCALE -> LocalizeValue.localizeTODO("Greyscale");
            case OFF -> LocalizeValue.localizeTODO("No antialiasing");
            default -> throw new IllegalArgumentException(type.toString());
        };
    }

    private static Object getActiveIconLibraryOrNull() {
        IconLibraryManager iconLibraryManager = IconLibraryManager.get();
        if (iconLibraryManager.isFromStyle()) {
            return ObjectUtil.NULL;
        }
        IconLibrary activeLibrary = iconLibraryManager.getActiveLibrary();
        return activeLibrary == null ? ObjectUtil.NULL : activeLibrary;
    }

    /**
     * The number in the text - the default for no text, or for anything else than a positive number.
     */
    private static int getIntValue(@Nullable String text, int defaultValue) {
        if (text == null || text.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(text.trim());
            return value > 0 ? value : defaultValue;
        }
        catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}
