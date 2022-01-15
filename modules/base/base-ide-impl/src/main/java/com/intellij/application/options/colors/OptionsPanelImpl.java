/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.options.ex.Settings;
import com.intellij.ui.JBColor;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.panels.Wrapper;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import consulo.util.concurrent.AsyncResult;

import javax.annotation.Nonnull;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.*;
import java.util.EventListener;
import java.util.HashSet;
import java.util.Set;

public class OptionsPanelImpl implements OptionsPanel {
  public static final String SELECTED_COLOR_OPTION_PROPERTY = "selected.color.option.type";

  public interface ColorDescriptionPanel {
    @Nonnull
    JComponent getPanel();

    void resetDefault();

    void reset(@Nonnull EditorSchemeAttributeDescriptor description);

    void apply(@Nonnull EditorSchemeAttributeDescriptor descriptor, EditorColorsScheme scheme);

    void addListener(@Nonnull Listener listener);

    interface Listener extends EventListener {
      void onSettingsChanged();

      void onHyperLinkClicked(@Nonnull HyperlinkEvent e);
    }
  }

  private final ColorOptionsTree myOptionsTree;
  private final ColorDescriptionPanel myOptionsPanel;

  private final ColorAndFontOptions myOptions;
  private final SchemesPanel mySchemesProvider;
  private final String myCategoryName;

  private final PropertiesComponent myProperties;

  private final EventDispatcher<ColorAndFontSettingsListener> myDispatcher = EventDispatcher.create(ColorAndFontSettingsListener.class);

  private final JPanel myPanel;

  public OptionsPanelImpl(ColorAndFontOptions options, SchemesPanel schemesProvider, String categoryName) {
    this(options, schemesProvider, categoryName, new ColorAndFontDescriptionPanel());
  }

  public OptionsPanelImpl(ColorAndFontOptions options, SchemesPanel schemesProvider, String categoryName, ColorDescriptionPanel optionsPanel) {
    myPanel = new JPanel(new BorderLayout());

    myOptions = options;
    mySchemesProvider = schemesProvider;
    myCategoryName = categoryName;
    myProperties = PropertiesComponent.getInstance();

    myOptionsPanel = optionsPanel;
    optionsPanel.addListener(new ColorDescriptionPanel.Listener() {
      @Override
      public void onSettingsChanged() {
        myDispatcher.getMulticaster().settingsChanged();
      }

      @Override
      public void onHyperLinkClicked(@Nonnull HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
          Settings settings = DataManager.getInstance().getDataContext(myPanel).getData(Settings.KEY);
          String pageName = e.getDescription();
          Element element = e.getSourceElement();
          String attrName;
          try {
            attrName = element.getDocument().getText(element.getStartOffset(), element.getEndOffset() - element.getStartOffset());
          }
          catch (BadLocationException e1) {
            return;
          }
          final SearchableConfigurable page = myOptions.findSubConfigurable(pageName);
          if (page != null && settings != null) {
            Runnable runnable = page.enableSearch(attrName);
            AsyncResult<Void> callback = settings.select(page);
            if (runnable != null) callback.doWhenDone(runnable);
          }
        }
      }
    });

    myOptionsTree = new ColorOptionsTree(myCategoryName);

    myOptionsTree.addTreeSelectionListener(e -> {
      if (!mySchemesProvider.areSchemesLoaded()) return;
      processListValueChanged();
    });

    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myOptionsTree, true);
    myPanel.add(scrollPane, BorderLayout.CENTER);
    JComponent panel = myOptionsPanel.getPanel();
    panel.setBorder(JBUI.Borders.empty(5));

    Wrapper wrapper = new Wrapper(panel);
    wrapper.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 1, 0, 0));
    myPanel.add(wrapper, BorderLayout.EAST);

    myPanel.setBorder(JBUI.Borders.customLine(JBColor.border(), 1, 0, 0, 0));
  }

  @Override
  public void addListener(ColorAndFontSettingsListener listener) {
    myDispatcher.addListener(listener);
  }

  private void processListValueChanged() {
    Object selectedValue = myOptionsTree.getSelectedValue();
    ColorAndFontDescription description = selectedValue instanceof ColorAndFontDescription ? (ColorAndFontDescription)selectedValue : null;
    if (description == null) {
      if (selectedValue == null) {
        String preselectedType = myProperties.getValue(SELECTED_COLOR_OPTION_PROPERTY);
        if (preselectedType != null) {
          myOptionsTree.selectOptionByType(preselectedType);
          description = myOptionsTree.getSelectedDescriptor();
        }
      }
    }
    if (description != null) {
      myProperties.setValue(SELECTED_COLOR_OPTION_PROPERTY, description.getType());
      myOptionsPanel.reset(description);
      myDispatcher.getMulticaster().selectedOptionChanged(description);
    }
    else {
      myOptionsPanel.resetDefault();
    }
  }

  private void fillOptionsList() {
    myOptionsTree.fillOptions(myOptions);
  }

  @Override
  public JPanel getPanel() {
    return myPanel;
  }

  @Override
  public void updateOptionsList() {
    fillOptionsList();
    processListValueChanged();
  }

  @Override
  public Runnable showOption(final String attributeDisplayName) {
    return () -> myOptionsTree.selectOptionByName(attributeDisplayName);
  }

  @Override
  public void applyChangesToScheme() {
    ColorAndFontDescription descriptor = myOptionsTree.getSelectedDescriptor();
    if (descriptor != null) {
      myOptionsPanel.apply(descriptor, myOptions.getSelectedScheme());
    }
  }

  @Override
  public void selectOption(String attributeType) {
    myOptionsTree.selectOptionByType(attributeType);
  }

  @Override
  public Set<String> processListOptions() {
    HashSet<String> result = new HashSet<>();
    EditorSchemeAttributeDescriptor[] descriptions = myOptions.getCurrentDescriptions();
    for (EditorSchemeAttributeDescriptor description : descriptions) {
      if (description.getGroup().getValue().equals(myCategoryName)) {
        result.add(description.toString());
      }
    }
    return result;
  }
}
