/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.application.options.colors;

import com.intellij.Patches;
import com.intellij.application.options.OptionsConstants;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.FontPreferences;
import com.intellij.openapi.editor.colors.ModifiableFontPreferences;
import com.intellij.openapi.ui.LabeledComponent;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.FontComboBox;
import com.intellij.ui.FontInfoRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.ui.style.StandardColors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import java.awt.event.*;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class FontOptions implements OptionsPanel {
  private static final FontInfoRenderer RENDERER = new FontInfoRenderer() {
    @Override
    protected boolean isEditorFont() {
      return true;
    }
  };
  private static final String HELP_URL = "https://confluence.jetbrains.com/display/IDEADEV/Support+for+Ligatures+in+Editor";

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  @Nonnull
  private final ColorAndFontOptions myOptions;

  @Nonnull
  private final JTextField myEditorFontSizeField = new JTextField(4);
  @Nonnull
  private final JTextField myLineSpacingField = new JTextField(4);
  private final FontComboBox myPrimaryCombo = new FontComboBox();
  private final JCheckBox myUseSecondaryFontCheckbox = new JCheckBox(ApplicationBundle.message("secondary.font"));
  private final JCheckBox myEnableLigaturesCheckbox = new JCheckBox(ApplicationBundle.message("use.ligatures"));
  private final JLabel myLigaturesInfoLinkLabel;
  private final FontComboBox mySecondaryCombo = new FontComboBox();

  @Nonnull
  private final JBCheckBox myOnlyMonospacedCheckBox = new JBCheckBox(ApplicationBundle.message("checkbox.show.only.monospaced.fonts"));

  private boolean myIsInSchemeChange;

  private final JPanel myRootPanel;

  public FontOptions(ColorAndFontOptions options) {
    this(options, ApplicationBundle.message("group.editor.font"));
  }

  protected FontOptions(@Nonnull ColorAndFontOptions options, final String title) {
    JPanel mainPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, true, false));
    mainPanel.setBorder(JBUI.Borders.empty(5));

    myOptions = options;
    mainPanel.add(myOnlyMonospacedCheckBox);

    JPanel primaryFontPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    primaryFontPanel.setBorder(JBUI.Borders.emptyLeft(20));
    mainPanel.add(primaryFontPanel);

    primaryFontPanel.add(LabeledComponent.left(myPrimaryCombo, ApplicationBundle.message("primary.font")));
    primaryFontPanel.add(LabeledComponent.left(myEditorFontSizeField, ApplicationBundle.message("editbox.font.size")));
    primaryFontPanel.add(LabeledComponent.left(myLineSpacingField, ApplicationBundle.message("editbox.line.spacing")));

    JLabel infoLabel = new JLabel(ApplicationBundle.message("label.fallback.fonts.list.description"));
    infoLabel.setFont(JBUI.Fonts.smallFont());
    infoLabel.setForeground(TargetAWT.to(StandardColors.GRAY));
    mainPanel.add(infoLabel);

    JPanel secondFontPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    mainPanel.add(secondFontPanel);

    secondFontPanel.add(myUseSecondaryFontCheckbox);
    secondFontPanel.add(mySecondaryCombo);

    JPanel ligaturesPanel = new JPanel(new HorizontalLayout(JBUI.scale(5)));
    ligaturesPanel.setEnabled(!Patches.TEXT_LAYOUT_IS_SLOW);
    myEnableLigaturesCheckbox.setBorder(null);
    ligaturesPanel.add(myEnableLigaturesCheckbox);
    myLigaturesInfoLinkLabel = new LinkLabel<>(ApplicationBundle.message("ligatures.more.info"), null, new LinkListener<String>() {
      @Override
      public void linkSelected(LinkLabel aSource, String aLinkData) {
        BrowserUtil.browse(HELP_URL);
      }
    });
    myLigaturesInfoLinkLabel.setBorder(JBUI.Borders.emptyLeft(5));
    ligaturesPanel.add(myLigaturesInfoLinkLabel);
    mainPanel.add(ligaturesPanel);

    myOnlyMonospacedCheckBox.setBorder(null);
    myUseSecondaryFontCheckbox.setBorder(null);
    mySecondaryCombo.setEnabled(false);

    myOnlyMonospacedCheckBox.setSelected(EditorColorsManager.getInstance().isUseOnlyMonospacedFonts());
    myOnlyMonospacedCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        EditorColorsManager.getInstance().setUseOnlyMonospacedFonts(myOnlyMonospacedCheckBox.isSelected());
        myPrimaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
        mySecondaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
      }
    });
    myPrimaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
    myPrimaryCombo.setRenderer(RENDERER);

    mySecondaryCombo.setMonospacedOnly(myOnlyMonospacedCheckBox.isSelected());
    mySecondaryCombo.setRenderer(RENDERER);

    myUseSecondaryFontCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        mySecondaryCombo.setEnabled(myUseSecondaryFontCheckbox.isSelected());
        FontOptions.this.syncFontFamilies();
      }
    });
    ItemListener itemListener = new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          FontOptions.this.syncFontFamilies();
        }
      }
    };
    myPrimaryCombo.addItemListener(itemListener);
    mySecondaryCombo.addItemListener(itemListener);

    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FontOptions.this.syncFontFamilies();
      }
    };
    myPrimaryCombo.addActionListener(actionListener);
    mySecondaryCombo.addActionListener(actionListener);

    myEditorFontSizeField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(DocumentEvent event) {
        if (myIsInSchemeChange || !SwingUtilities.isEventDispatchThread()) return;
        String selectedFont = myPrimaryCombo.getFontName();
        if (selectedFont != null) {
          ModifiableFontPreferences fontPreferences = getFontPreferences();
          fontPreferences.register(selectedFont, getFontSizeFromField());
        }
        updateDescription(true);
      }
    });
    myEditorFontSizeField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN) return;
        boolean up = e.getKeyCode() == KeyEvent.VK_UP;
        try {
          int value = Integer.parseInt(myEditorFontSizeField.getText());
          value += (up ? 1 : -1);
          value = Math.min(OptionsConstants.MAX_EDITOR_FONT_SIZE, Math.max(OptionsConstants.MIN_EDITOR_FONT_SIZE, value));
          myEditorFontSizeField.setText(String.valueOf(value));
        }
        catch (NumberFormatException ignored) {
        }
      }
    });

    myLineSpacingField.getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      public void textChanged(DocumentEvent event) {
        if (myIsInSchemeChange) return;
        float lineSpacing = getLineSpacingFromField();
        if (getLineSpacing() != lineSpacing) {
          setCurrentLineSpacing(lineSpacing);
        }
        updateDescription(true);
      }
    });
    myLineSpacingField.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() != KeyEvent.VK_UP && e.getKeyCode() != KeyEvent.VK_DOWN) return;
        boolean up = e.getKeyCode() == KeyEvent.VK_UP;
        try {
          float value = Float.parseFloat(myLineSpacingField.getText());
          value += (up ? 1 : -1) * .1F;
          value = Math.min(OptionsConstants.MAX_EDITOR_LINE_SPACING, Math.max(OptionsConstants.MIN_EDITOR_LINE_SPACING, value));
          myLineSpacingField.setText(String.format(Locale.ENGLISH, "%.1f", value));
        }
        catch (NumberFormatException ignored) {
        }
      }
    });
    myEnableLigaturesCheckbox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        FontOptions.this.getFontPreferences().setUseLigatures(myEnableLigaturesCheckbox.isSelected());
      }
    });

    Wrapper wrapper = new Wrapper(mainPanel);
    wrapper.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
    myRootPanel = wrapper;
  }

  private int getFontSizeFromField() {
    try {
      return Math.min(OptionsConstants.MAX_EDITOR_FONT_SIZE, Math.max(OptionsConstants.MIN_EDITOR_FONT_SIZE, Integer.parseInt(myEditorFontSizeField.getText())));
    }
    catch (NumberFormatException e) {
      return OptionsConstants.DEFAULT_EDITOR_FONT_SIZE;
    }
  }

  private float getLineSpacingFromField() {
    try {
      return Math.min(OptionsConstants.MAX_EDITOR_LINE_SPACING, Math.max(OptionsConstants.MIN_EDITOR_LINE_SPACING, Float.parseFloat(myLineSpacingField.getText())));
    }
    catch (NumberFormatException e) {
      return OptionsConstants.DEFAULT_EDITOR_LINE_SPACING;
    }
  }

  private void syncFontFamilies() {
    if (myIsInSchemeChange) {
      return;
    }
    ModifiableFontPreferences fontPreferences = getFontPreferences();
    fontPreferences.clearFonts();
    String primaryFontFamily = myPrimaryCombo.getFontName();
    String secondaryFontFamily = mySecondaryCombo.isEnabled() ? mySecondaryCombo.getFontName() : null;
    int fontSize = getFontSizeFromField();
    if (primaryFontFamily != null) {
      if (!FontPreferences.DEFAULT_FONT_NAME.equals(primaryFontFamily)) {
        fontPreferences.addFontFamily(primaryFontFamily);
      }
      fontPreferences.register(primaryFontFamily, JBUI.scale(fontSize));
    }
    if (secondaryFontFamily != null) {
      if (!FontPreferences.DEFAULT_FONT_NAME.equals(secondaryFontFamily)) {
        fontPreferences.addFontFamily(secondaryFontFamily);
      }
      fontPreferences.register(secondaryFontFamily, JBUI.scale(fontSize));
    }
    updateDescription(true);
  }


  public static void showReadOnlyMessage(JComponent parent, final boolean sharedScheme) {
    if (!sharedScheme) {
      Messages.showMessageDialog(parent, ApplicationBundle.message("error.readonly.scheme.cannot.be.modified"), ApplicationBundle.message("title.cannot.modify.readonly.scheme"),
                                 Messages.getInformationIcon());
    }
    else {
      Messages.showMessageDialog(parent, ApplicationBundle.message("error.shared.scheme.cannot.be.modified"), ApplicationBundle.message("title.cannot.modify.readonly.scheme"),
                                 Messages.getInformationIcon());
    }
  }

  @Override
  public void updateOptionsList() {
    myIsInSchemeChange = true;

    myLineSpacingField.setText(Float.toString(getLineSpacing()));
    FontPreferences fontPreferences = getFontPreferences();
    List<String> fontFamilies = fontPreferences.getEffectiveFontFamilies();
    myPrimaryCombo.setFontName(fontPreferences.getFontFamily());
    boolean isThereSecondaryFont = fontFamilies.size() > 1;
    myUseSecondaryFontCheckbox.setSelected(isThereSecondaryFont);
    mySecondaryCombo.setFontName(isThereSecondaryFont ? fontFamilies.get(1) : null);
    myEditorFontSizeField.setText(String.valueOf(fontPreferences.getSize(fontPreferences.getFontFamily())));

    boolean readOnly = ColorAndFontOptions.isReadOnly(myOptions.getSelectedScheme());
    myPrimaryCombo.setEnabled(!readOnly);
    mySecondaryCombo.setEnabled(isThereSecondaryFont && !readOnly);
    myOnlyMonospacedCheckBox.setEnabled(!readOnly);
    myLineSpacingField.setEnabled(!readOnly);
    myEditorFontSizeField.setEnabled(!readOnly);
    myUseSecondaryFontCheckbox.setEnabled(!readOnly);

    myEnableLigaturesCheckbox.setEnabled(!readOnly);
    myLigaturesInfoLinkLabel.setEnabled(!readOnly);
    myEnableLigaturesCheckbox.setSelected(fontPreferences.useLigatures());

    myIsInSchemeChange = false;
  }

  @Nonnull
  protected ModifiableFontPreferences getFontPreferences() {
    return (ModifiableFontPreferences)getCurrentScheme().getFontPreferences();
  }

  protected float getLineSpacing() {
    return getCurrentScheme().getLineSpacing();
  }

  protected void setCurrentLineSpacing(float lineSpacing) {
    getCurrentScheme().setLineSpacing(lineSpacing);
  }

  @Override
  @Nullable
  public Runnable showOption(final String option) {
    return null;
  }

  @Override
  public void applyChangesToScheme() {
  }

  @Override
  public void selectOption(final String typeToSelect) {
  }

  protected EditorColorsScheme getCurrentScheme() {
    return myOptions.getSelectedScheme();
  }

  public boolean updateDescription(boolean modified) {
    EditorColorsScheme scheme = myOptions.getSelectedScheme();

    if (modified && (ColorAndFontOptions.isReadOnly(scheme))) {
      return false;
    }

    myDispatcher.getMulticaster().fontChanged();

    return true;
  }

  @Override
  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  @Override
  public JPanel getPanel() {
    return myRootPanel;
  }

  @Override
  public Set<String> processListOptions() {
    return new HashSet<>();
  }
}
