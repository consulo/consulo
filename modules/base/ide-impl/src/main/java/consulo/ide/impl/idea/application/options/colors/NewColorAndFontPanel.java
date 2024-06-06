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

package consulo.ide.impl.idea.application.options.colors;

import consulo.application.ApplicationBundle;
import consulo.application.ApplicationManager;
import consulo.colorScheme.EditorColorsScheme;
import consulo.language.editor.colorScheme.setting.ColorSettingsPage;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.awt.OnePixelSplitter;
import consulo.ui.ex.awtUnsafe.TargetAWT;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class NewColorAndFontPanel extends JPanel {
  private final ColorSettingsPage mySettingsPage;
  private final SchemesPanel mySchemesPanel;
  private final OptionsPanel myOptionsPanel;
  private final PreviewPanel myPreviewPanel;
  private final AbstractAction myCopyAction;
  private final String myCategory;
  private final Collection<String> myOptionList;

  public NewColorAndFontPanel(final SchemesPanel schemesPanel,
                              final OptionsPanel optionsPanel,
                              final PreviewPanel previewPanel,
                              final String category,
                              final Collection<String> optionList,
                              final ColorSettingsPage page) {
    super(new BorderLayout(0, 10));
    mySchemesPanel = schemesPanel;
    myOptionsPanel = optionsPanel;
    myPreviewPanel = previewPanel;
    myCategory = category;
    myOptionList = optionList;
    mySettingsPage = page;

    JPanel top = new JPanel(new BorderLayout());

    top.add(TargetAWT.to(mySchemesPanel.getComponent()), BorderLayout.NORTH);
    top.add(myOptionsPanel.getPanel(), BorderLayout.CENTER);
    if (optionsPanel instanceof ConsoleFontOptions) {
      JPanel wrapper = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      myCopyAction = new AbstractAction(ApplicationBundle.message("action.apply.editor.font.settings")) {
        @Override
        public void actionPerformed(ActionEvent e) {
          EditorColorsScheme scheme = ((ConsoleFontOptions)myOptionsPanel).getCurrentScheme();
          scheme.setConsoleFontName(scheme.getEditorFontName());
          scheme.setConsoleFontPreferences(scheme.getFontPreferences());
          scheme.setConsoleFontSize(scheme.getEditorFontSize());
          scheme.setConsoleLineSpacing(scheme.getLineSpacing());
          myOptionsPanel.updateOptionsList();
          myPreviewPanel.updateView();
        }
      };
      wrapper.add(new JButton(myCopyAction));
      top.add(wrapper, BorderLayout.SOUTH);
    }
    else {
      myCopyAction = null;
    }

    // We don't want to show non-used preview panel (it's considered to be not in use if it doesn't contain text).
    if (myPreviewPanel.getPanel() != null && (page == null || !StringUtil.isEmptyOrSpaces(page.getDemoText()))) {
      OnePixelSplitter splitter = new OnePixelSplitter(true);
      splitter.setFirstComponent(top);
      splitter.setSecondComponent((JComponent)myPreviewPanel.getPanel());
      add(splitter, BorderLayout.CENTER);
    }
    else {
      add(top, BorderLayout.CENTER);
    }

    previewPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void selectionInPreviewChanged(final String typeToSelect) {
        optionsPanel.selectOption(typeToSelect);
      }
    });

    optionsPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void settingsChanged() {
        if (schemesPanel.updateDescription(true)) {
          optionsPanel.applyChangesToScheme();
          previewPanel.updateView();
        }
      }

      @Override
      public void selectedOptionChanged(final Object selected) {
        if (ApplicationManager.getApplication().isDispatchThread()) {
          myPreviewPanel.blinkSelectedHighlightType(selected);
        }
      }

    });
    mySchemesPanel.addListener(new ColorAndFontSettingsListener.Abstract() {
      @Override
      public void schemeChanged(final Object source) {
        myOptionsPanel.updateOptionsList();
        myPreviewPanel.updateView();
        if (optionsPanel instanceof ConsoleFontOptions) {
          ConsoleFontOptions options = (ConsoleFontOptions)optionsPanel;
          boolean readOnly = ColorAndFontOptions.isReadOnly(options.getCurrentScheme());
          myCopyAction.setEnabled(!readOnly);
        }
      }
    });

  }

  public static NewColorAndFontPanel create(final PreviewPanel previewPanel, String category, final ColorAndFontOptions options, Collection<String> optionList, ColorSettingsPage page) {
    final SchemesPanel schemesPanel = new SchemesPanel(options);

    final OptionsPanel optionsPanel = new OptionsPanelImpl(options, schemesPanel, category);


    return new NewColorAndFontPanel(schemesPanel, optionsPanel, previewPanel, category, optionList, page);
  }

  public Runnable showOption(final String option) {
    return myOptionsPanel.showOption(option);
  }

  @Nonnull
  public Set<String> processListOptions() {
    if (myOptionList == null) {
      return myOptionsPanel.processListOptions();
    }
    else {
      final HashSet<String> result = new HashSet<String>();
      for (String s : myOptionList) {
        result.add(s);
      }
      return result;
    }
  }


  public String getDisplayName() {
    return myCategory;
  }

  public void reset(Object source) {
    resetSchemesCombo(source);
  }

  public void disposeUIResources() {
    myPreviewPanel.disposeUIResources();
  }

  public void addSchemesListener(final ColorAndFontSettingsListener schemeListener) {
    mySchemesPanel.addListener(schemeListener);
  }

  private void resetSchemesCombo(Object source) {
    mySchemesPanel.resetSchemesCombo(source);
  }

  public boolean contains(final EditorSchemeAttributeDescriptor descriptor) {
    return descriptor.getGroup().getValue().equals(myCategory);
  }

  public JComponent getPanel() {
    return this;
  }

  public void updatePreview() {
    myPreviewPanel.updateView();
  }

  public void addDescriptionListener(final ColorAndFontSettingsListener listener) {
    myOptionsPanel.addListener(listener);
  }

  public boolean containsFontOptions() {
    return false;
  }

  public ColorSettingsPage getSettingsPage() {
    return mySettingsPage;
  }
}
