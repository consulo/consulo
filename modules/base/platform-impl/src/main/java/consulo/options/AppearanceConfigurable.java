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
package consulo.options;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.ex.util.EditorUtil;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.util.NotNullComputable;
import com.intellij.util.ui.UIUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.IdeLocalize;
import consulo.platform.base.localize.KeyMapLocalize;
import consulo.ui.*;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.font.Font;
import consulo.ui.font.FontManager;
import consulo.ui.image.IconLibrary;
import consulo.ui.image.IconLibraryManager;
import consulo.ui.layout.*;
import consulo.ui.style.Style;
import consulo.ui.style.StyleManager;
import consulo.ui.util.LabeledBuilder;
import consulo.util.lang.Comparing;
import consulo.util.lang.ObjectUtil;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2020-08-23
 */
public class AppearanceConfigurable extends SimpleConfigurable<AppearanceConfigurable.LayoutImpl> implements Configurable {
  public static class LayoutImpl implements NotNullComputable<Layout> {
    private VerticalLayout myPanel;

    private ComboBox<String> myFontCombo;
    private TextBoxWithHistory myFontSizeCombo;
    private CheckBox myAnimateWindowsCheckBox;
    private CheckBox myWindowShortcutsCheckBox;
    private CheckBox myShowToolStripesCheckBox;
    private ComboBox<Style> myLafComboBox;
    private ComboBox<Object> myIconThemeComboBox;
    private CheckBox myCycleScrollingCheckBox;

    private CheckBox myMoveMouseOnDefaultButtonCheckBox;
    private CheckBox myOverrideLAFFonts;

    private CheckBox myHideIconsInQuickNavigation;
    private CheckBox myCbDisplayIconsInMenu;
    private CheckBox myDisableMnemonics;
    private CheckBox myDisableMnemonicInControlsCheckBox;
    private CheckBox myHideNavigationPopupsCheckBox;
    private CheckBox myAltDNDCheckBox;
    private CheckBox myAllowMergeButtons;
    private CheckBox myUseSmallLabelsOnTabs;
    private CheckBox myWidescreenLayoutCheckBox;
    private CheckBox myLeftLayoutCheckBox;
    private CheckBox myRightLayoutCheckBox;
    private TextBoxWithHistory myPresentationModeFontSize;
    private CheckBox myEditorTooltipCheckBox;
    private ComboBox<AntialiasingType> myAntialiasingInIDE;
    private ComboBox<AntialiasingType> myAntialiasingInEditor;
    private CheckBox mySmoothScrollingBox;

    @RequiredUIAccess
    private LayoutImpl() {
      myPanel = VerticalLayout.create();

      VerticalLayout uiOptions = VerticalLayout.create();
      uiOptions.add(myCycleScrollingCheckBox = CheckBox.create(IdeLocalize.checkbooxCyclicScrollingInLists()));
      uiOptions.add(myHideIconsInQuickNavigation = CheckBox.create(IdeLocalize.checkboxShowIconsInQuickNavigation()));
      uiOptions.add(myMoveMouseOnDefaultButtonCheckBox = CheckBox.create(IdeLocalize.checkboxPositionCursorOnDefaultButton()));
      uiOptions.add(myHideNavigationPopupsCheckBox = CheckBox.create("Hide navigation popups on focus loss"));
      uiOptions.add(myAltDNDCheckBox = CheckBox.create("Drag-n-Drop with ALT pressed only"));

      myLafComboBox = ComboBox.create(StyleManager.get().getStyles());
      myLafComboBox.setTextRender(style -> style == null ? LocalizeValue.empty() : LocalizeValue.of(style.getName()));
      uiOptions.add(LabeledBuilder.simple(IdeLocalize.comboboxLookAndFeel(), myLafComboBox));

      List<Object> iconThemes = new ArrayList<>();
      iconThemes.add(ObjectUtil.NULL);
      Map<String, IconLibrary> libraries = IconLibraryManager.get().getLibraries();
      iconThemes.addAll(libraries.values());
      myIconThemeComboBox = ComboBox.create(iconThemes);
      myIconThemeComboBox.setRender((render, index, item) -> {
        if (item == ObjectUtil.NULL) {
          render.append(IdeLocalize.comboboxIconThemeUiDefault());
        }
        else {
          render.append(((IconLibrary)item).getName());
        }
      });
      uiOptions.add(LabeledBuilder.simple(IdeLocalize.comboboxIconTheme(), myIconThemeComboBox));

      HorizontalLayout useCustomFontLine = HorizontalLayout.create();
      useCustomFontLine.add(myOverrideLAFFonts = CheckBox.create(IdeLocalize.checkboxOverrideDefaultLafFonts()));
      Set<String> avaliableFontNames = FontManager.get().getAvaliableFontNames();
      useCustomFontLine.add(myFontCombo = ComboBox.create(avaliableFontNames));
      useCustomFontLine.add(LabeledBuilder.simple(IdeLocalize.labelFontSize(), myFontSizeCombo = TextBoxWithHistory.create().setHistory(UIUtil.getStandardFontSizes())));
      uiOptions.add(useCustomFontLine);

      myOverrideLAFFonts.addValueListener(event -> {
        myFontCombo.setEnabled(event.getValue());
        myFontSizeCombo.setEnabled(event.getValue());
      });

      myFontCombo.setEnabled(false);
      myFontSizeCombo.setEnabled(false);

      myPanel.add(LabeledLayout.create(IdeLocalize.groupUiOptions(), uiOptions));

      TableLayout aaPanel = TableLayout.create(StaticPosition.CENTER);

      myAntialiasingInIDE = ComboBox.create(AntialiasingType.values());
      myAntialiasingInIDE.setRender(buildItemRender(false));

      aaPanel.add(LabeledBuilder.simple(IdeLocalize.labelTextAntialiasingScopeIde(), myAntialiasingInIDE), TableLayout.cell(0, 0).fill());

      myAntialiasingInEditor = ComboBox.create(AntialiasingType.values());
      myAntialiasingInEditor.setRender(buildItemRender(true));
      aaPanel.add(LabeledBuilder.simple(IdeLocalize.labelTextAntialiasingScopeEditor(), myAntialiasingInEditor), TableLayout.cell(0, 1).fill());

      myPanel.add(LabeledLayout.create(IdeLocalize.groupAntialiasingMode(), aaPanel));

      TableLayout windowOptions = TableLayout.create(StaticPosition.CENTER);

      VerticalLayout leftWindowOption = VerticalLayout.create();
      VerticalLayout rightWindowOption = VerticalLayout.create();

      windowOptions.add(leftWindowOption, TableLayout.cell(0, 0).fill());
      windowOptions.add(rightWindowOption, TableLayout.cell(0, 1).fill());

      leftWindowOption.add(myAnimateWindowsCheckBox = CheckBox.create(IdeLocalize.checkboxAnimateWindows()));
      leftWindowOption.add(myDisableMnemonics = CheckBox.create(KeyMapLocalize.disableMnemonicInMenuCheckBox()));
      leftWindowOption.add(myDisableMnemonicInControlsCheckBox = CheckBox.create(KeyMapLocalize.disableMnemonicInControlsCheckBox()));
      leftWindowOption.add(myCbDisplayIconsInMenu = CheckBox.create(IdeLocalize.checkboxShowIconsInMenuItems()));
      leftWindowOption.add(myLeftLayoutCheckBox = CheckBox.create(IdeLocalize.checkboxLeftToolwindowLayout()));
      leftWindowOption.add(myEditorTooltipCheckBox = CheckBox.create(IdeLocalize.checkboxShowEditorPreviewPopup()));

      rightWindowOption.add(myShowToolStripesCheckBox = CheckBox.create(IdeLocalize.checkboxShowToolWindowBars()));
      rightWindowOption.add(myWindowShortcutsCheckBox = CheckBox.create(IdeLocalize.checkboxShowToolWindowNumbers()));
      rightWindowOption.add(myAllowMergeButtons = CheckBox.create("Allow merging buttons on dialogs"));
      rightWindowOption.add(myUseSmallLabelsOnTabs = CheckBox.create("Small labels in editor tabs"));
      rightWindowOption.add(myWidescreenLayoutCheckBox = CheckBox.create(IdeLocalize.checkboxWidescreenToolWindowLayout()));
      rightWindowOption.add(myRightLayoutCheckBox = CheckBox.create(IdeLocalize.checkboxRightToolwindowLayout()));
      rightWindowOption.add(mySmoothScrollingBox = CheckBox.create("Smooth scrolling"));

      myPanel.add(LabeledLayout.create(IdeLocalize.groupWindowOptions(), windowOptions));

      VerticalLayout presentationOptions = VerticalLayout.create();
      presentationOptions.add(LabeledBuilder.simple(IdeLocalize.labelFontSize(), myPresentationModeFontSize = TextBoxWithHistory.create().setHistory(UIUtil.getStandardFontSizes())));
      myPanel.add(LabeledLayout.create(IdeLocalize.groupPresentationMode(), presentationOptions));
    }

    private TextItemRender<AntialiasingType> buildItemRender(boolean editor) {
      return (render, index, item) -> {
        if (item == null) {
          return;
        }

        render.withAntialiasingType(item);

        if (editor) {
          EditorColorsScheme scheme = EditorColorsManager.getInstance().getGlobalScheme();
          render.withFont(FontManager.get().createFont(scheme.getEditorFontName(), scheme.getEditorFontSize(), Font.STYLE_PLAIN));
        }

        render.append(textForAntialiasingType(item));
      };
    }

    private LocalizeValue textForAntialiasingType(@Nonnull AntialiasingType type) {
      switch (type) {
        case SUBPIXEL:
          return LocalizeValue.localizeTODO("Subpixel");
        case GREYSCALE:
          return LocalizeValue.localizeTODO("Greyscale");
        case OFF:
          return LocalizeValue.localizeTODO("No antialiasing");
        default:
          throw new IllegalArgumentException(type.toString());
      }
    }

    @Nonnull
    @Override
    public Layout compute() {
      return myPanel;
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  protected LayoutImpl createPanel(Disposable uiDisposable) {
    return new LayoutImpl();
  }

  @RequiredUIAccess
  @Override
  protected boolean isModified(@Nonnull LayoutImpl component) {
    UISettings settings = UISettings.getInstance();

    boolean isModified = false;
    isModified |= !Comparing.equal(component.myFontCombo.getValue(), settings.FONT_FACE);
    isModified |= !Comparing.equal(component.myFontSizeCombo.getValue(), Integer.toString(settings.FONT_SIZE));
    isModified |= component.myAntialiasingInIDE.getValue() != settings.IDE_AA_TYPE;
    isModified |= component.myAntialiasingInEditor.getValue() != settings.EDITOR_AA_TYPE;
    isModified |= component.myAnimateWindowsCheckBox.getValue() != settings.ANIMATE_WINDOWS;
    isModified |= component.myWindowShortcutsCheckBox.getValue() != settings.SHOW_TOOL_WINDOW_NUMBERS;
    isModified |= component.myShowToolStripesCheckBox.getValue() == settings.HIDE_TOOL_STRIPES;
    isModified |= component.myCbDisplayIconsInMenu.getValue() != settings.SHOW_ICONS_IN_MENUS;
    isModified |= component.myAllowMergeButtons.getValue() != settings.ALLOW_MERGE_BUTTONS;
    isModified |= component.myCycleScrollingCheckBox.getValue() != settings.CYCLE_SCROLLING;

    isModified |= component.myOverrideLAFFonts.getValue() != settings.OVERRIDE_NONIDEA_LAF_FONTS;

    isModified |= component.myDisableMnemonics.getValue() != settings.DISABLE_MNEMONICS;
    isModified |= component.myDisableMnemonicInControlsCheckBox.getValue() != settings.DISABLE_MNEMONICS_IN_CONTROLS;

    isModified |= component.myUseSmallLabelsOnTabs.getValue() != settings.USE_SMALL_LABELS_ON_TABS;
    isModified |= component.myWidescreenLayoutCheckBox.getValue() != settings.WIDESCREEN_SUPPORT;
    isModified |= component.myLeftLayoutCheckBox.getValue() != settings.LEFT_HORIZONTAL_SPLIT;
    isModified |= component.myRightLayoutCheckBox.getValue() != settings.RIGHT_HORIZONTAL_SPLIT;
    isModified |= component.myEditorTooltipCheckBox.getValue() != settings.SHOW_EDITOR_TOOLTIP;

    isModified |= component.myHideIconsInQuickNavigation.getValue() != settings.SHOW_ICONS_IN_QUICK_NAVIGATION;

    isModified |= !Comparing.equal(component.myPresentationModeFontSize.getValue(), Integer.toString(settings.PRESENTATION_MODE_FONT_SIZE));

    isModified |= component.myMoveMouseOnDefaultButtonCheckBox.getValue() != settings.MOVE_MOUSE_ON_DEFAULT_BUTTON;
    isModified |= component.myHideNavigationPopupsCheckBox.getValue() != settings.HIDE_NAVIGATION_ON_FOCUS_LOSS;
    isModified |= component.myAltDNDCheckBox.getValue() != settings.DND_WITH_PRESSED_ALT_ONLY;
    isModified |= component.mySmoothScrollingBox.getValue() != settings.SMOOTH_SCROLLING;
    isModified |= !Comparing.equal(component.myLafComboBox.getValue(), StyleManager.get().getCurrentStyle());
    isModified |= !Comparing.equal(component.myIconThemeComboBox.getValue(), getActiveIconLibraryOrNull());

    return isModified;
  }

  @RequiredUIAccess
  @Override
  protected void reset(@Nonnull LayoutImpl component) {
    UISettings settings = UISettings.getInstance();

    component.myFontCombo.setValue(settings.FONT_FACE);
    component.myAntialiasingInIDE.setValue(settings.IDE_AA_TYPE);
    component.myAntialiasingInEditor.setValue(settings.EDITOR_AA_TYPE);
    component.myFontSizeCombo.setValue(Integer.toString(settings.FONT_SIZE));
    component.myPresentationModeFontSize.setValue(Integer.toString(settings.PRESENTATION_MODE_FONT_SIZE));
    component.myAnimateWindowsCheckBox.setValue(settings.ANIMATE_WINDOWS);
    component.myWindowShortcutsCheckBox.setValue(settings.SHOW_TOOL_WINDOW_NUMBERS);
    component.myShowToolStripesCheckBox.setValue(!settings.HIDE_TOOL_STRIPES);
    component.myCbDisplayIconsInMenu.setValue(settings.SHOW_ICONS_IN_MENUS);
    component.myAllowMergeButtons.setValue(settings.ALLOW_MERGE_BUTTONS);
    component.myCycleScrollingCheckBox.setValue(settings.CYCLE_SCROLLING);

    component.myHideIconsInQuickNavigation.setValue(settings.SHOW_ICONS_IN_QUICK_NAVIGATION);
    component.myMoveMouseOnDefaultButtonCheckBox.setValue(settings.MOVE_MOUSE_ON_DEFAULT_BUTTON);
    component.myHideNavigationPopupsCheckBox.setValue(settings.HIDE_NAVIGATION_ON_FOCUS_LOSS);
    component.myAltDNDCheckBox.setValue(settings.DND_WITH_PRESSED_ALT_ONLY);
    component.myLafComboBox.setValue(StyleManager.get().getCurrentStyle());
    component.myOverrideLAFFonts.setValue(settings.OVERRIDE_NONIDEA_LAF_FONTS);
    component.myDisableMnemonics.setValue(settings.DISABLE_MNEMONICS);
    component.myUseSmallLabelsOnTabs.setValue(settings.USE_SMALL_LABELS_ON_TABS);
    component.myWidescreenLayoutCheckBox.setValue(settings.WIDESCREEN_SUPPORT);
    component.myLeftLayoutCheckBox.setValue(settings.LEFT_HORIZONTAL_SPLIT);
    component.myRightLayoutCheckBox.setValue(settings.RIGHT_HORIZONTAL_SPLIT);
    component.myEditorTooltipCheckBox.setValue(settings.SHOW_EDITOR_TOOLTIP);
    component.myDisableMnemonicInControlsCheckBox.setValue(settings.DISABLE_MNEMONICS_IN_CONTROLS);
    component.mySmoothScrollingBox.setValue(settings.SMOOTH_SCROLLING);
    component.myIconThemeComboBox.setValue(getActiveIconLibraryOrNull());
  }

  @RequiredUIAccess
  @Override
  protected void apply(@Nonnull LayoutImpl component) throws ConfigurationException {
    UISettings settings = UISettings.getInstance();
    int _fontSize = getIntValue(component.myFontSizeCombo, settings.FONT_SIZE);
    int _presentationFontSize = getIntValue(component.myPresentationModeFontSize, settings.PRESENTATION_MODE_FONT_SIZE);
    boolean shouldUpdateUI = false;
    String _fontFace = component.myFontCombo.getValue();

    StyleManager styleManager = StyleManager.get();

    if (_fontSize != settings.FONT_SIZE || !settings.FONT_FACE.equals(_fontFace)) {
      settings.FONT_SIZE = _fontSize;
      settings.FONT_FACE = _fontFace;
      shouldUpdateUI = true;
    }

    if (_presentationFontSize != settings.PRESENTATION_MODE_FONT_SIZE) {
      settings.PRESENTATION_MODE_FONT_SIZE = _presentationFontSize;
      shouldUpdateUI = true;
    }

    if (component.myAntialiasingInIDE.getValue() != settings.IDE_AA_TYPE) {
      settings.IDE_AA_TYPE = component.myAntialiasingInIDE.getValue();
      styleManager.refreshAntialiasingType(settings.IDE_AA_TYPE);
      shouldUpdateUI = true;
    }

    if (component.myAntialiasingInEditor.getValue() != settings.EDITOR_AA_TYPE) {
      settings.EDITOR_AA_TYPE = component.myAntialiasingInEditor.getValue();
      shouldUpdateUI = true;
    }

    settings.ANIMATE_WINDOWS = component.myAnimateWindowsCheckBox.getValue();
    boolean update = settings.SHOW_TOOL_WINDOW_NUMBERS != component.myWindowShortcutsCheckBox.getValue();
    settings.SHOW_TOOL_WINDOW_NUMBERS = component.myWindowShortcutsCheckBox.getValue();
    update |= settings.HIDE_TOOL_STRIPES != !component.myShowToolStripesCheckBox.getValue();
    settings.HIDE_TOOL_STRIPES = !component.myShowToolStripesCheckBox.getValue();
    update |= settings.SHOW_ICONS_IN_MENUS != component.myCbDisplayIconsInMenu.getValue();
    settings.SHOW_ICONS_IN_MENUS = component.myCbDisplayIconsInMenu.getValue();
    update |= settings.ALLOW_MERGE_BUTTONS != component.myAllowMergeButtons.getValue();
    settings.ALLOW_MERGE_BUTTONS = component.myAllowMergeButtons.getValue();
    update |= settings.CYCLE_SCROLLING != component.myCycleScrollingCheckBox.getValue();
    settings.CYCLE_SCROLLING = component.myCycleScrollingCheckBox.getValue();
    if (settings.OVERRIDE_NONIDEA_LAF_FONTS != component.myOverrideLAFFonts.getValue()) {
      shouldUpdateUI = true;
    }
    settings.OVERRIDE_NONIDEA_LAF_FONTS = component.myOverrideLAFFonts.getValue();
    settings.MOVE_MOUSE_ON_DEFAULT_BUTTON = component.myMoveMouseOnDefaultButtonCheckBox.getValue();
    settings.HIDE_NAVIGATION_ON_FOCUS_LOSS = component.myHideNavigationPopupsCheckBox.getValue();
    settings.DND_WITH_PRESSED_ALT_ONLY = component.myAltDNDCheckBox.getValue();

    update |= settings.DISABLE_MNEMONICS != component.myDisableMnemonics.getValue();
    settings.DISABLE_MNEMONICS = component.myDisableMnemonics.getValue();

    update |= settings.USE_SMALL_LABELS_ON_TABS != component.myUseSmallLabelsOnTabs.getValue();
    settings.USE_SMALL_LABELS_ON_TABS = component.myUseSmallLabelsOnTabs.getValue();

    update |= settings.WIDESCREEN_SUPPORT != component.myWidescreenLayoutCheckBox.getValue();
    settings.WIDESCREEN_SUPPORT = component.myWidescreenLayoutCheckBox.getValue();

    update |= settings.LEFT_HORIZONTAL_SPLIT != component.myLeftLayoutCheckBox.getValue();
    settings.LEFT_HORIZONTAL_SPLIT = component.myLeftLayoutCheckBox.getValue();

    update |= settings.RIGHT_HORIZONTAL_SPLIT != component.myRightLayoutCheckBox.getValue();
    settings.RIGHT_HORIZONTAL_SPLIT = component.myRightLayoutCheckBox.getValue();

    update |= settings.SHOW_EDITOR_TOOLTIP != component.myEditorTooltipCheckBox.getValue();
    settings.SHOW_EDITOR_TOOLTIP = component.myEditorTooltipCheckBox.getValue();

    update |= settings.DISABLE_MNEMONICS_IN_CONTROLS != component.myDisableMnemonicInControlsCheckBox.getValue();
    settings.DISABLE_MNEMONICS_IN_CONTROLS = component.myDisableMnemonicInControlsCheckBox.getValue();

    update |= settings.SHOW_ICONS_IN_QUICK_NAVIGATION != component.myHideIconsInQuickNavigation.getValue();
    settings.SHOW_ICONS_IN_QUICK_NAVIGATION = component.myHideIconsInQuickNavigation.getValue();

    update |= settings.SMOOTH_SCROLLING != component.mySmoothScrollingBox.getValue();
    settings.SMOOTH_SCROLLING = component.mySmoothScrollingBox.getValue();

    UIAccess uiAccess = UIAccess.current();

    final boolean finalUpdate = update;
    final boolean finalShouldUpdateUI = shouldUpdateUI;

    uiAccess.give(() -> {
      boolean refreshUI = finalShouldUpdateUI;
      if (!Comparing.equal(component.myLafComboBox.getValue(), styleManager.getCurrentStyle())) {
        final Style newStyle = component.myLafComboBox.getValue();
        assert newStyle != null;
        styleManager.setCurrentStyle(newStyle);
        refreshUI = true;
      }

      if (!Comparing.equal(component.myIconThemeComboBox.getValue(), getActiveIconLibraryOrNull())) {
        Object iconLib = component.myIconThemeComboBox.getValue();

        if (iconLib == ObjectUtil.NULL) {
          IconLibraryManager.get().setActiveLibrary(null);
        }
        else {
          IconLibraryManager.get().setActiveLibrary(((IconLibrary)iconLib).getId());
        }

        refreshUI = true;
      }

      if (refreshUI) {
        styleManager.refreshUI();
      }

      if (finalUpdate) {
        settings.fireUISettingsChanged();
      }

      EditorUtil.reinitSettings();
    });
  }

  @Nonnull
  private static Object getActiveIconLibraryOrNull() {
    IconLibraryManager iconLibraryManager = IconLibraryManager.get();

    if (iconLibraryManager.isFromStyle()) {
      return ObjectUtil.NULL;
    }

    return iconLibraryManager.getActiveLibrary();
  }

  private static int getIntValue(TextBox combo, int defaultValue) {
    String temp = combo.getValue();
    int value = -1;
    if (temp != null && temp.trim().length() > 0) {
      try {
        value = Integer.parseInt(temp);
      }
      catch (NumberFormatException ignore) {
      }
      if (value <= 0) {
        value = defaultValue;
      }
    }
    else {
      value = defaultValue;
    }
    return value;
  }
}
