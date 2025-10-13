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

package consulo.ide.impl.idea.codeInspection.ex;

import consulo.codeEditor.CodeInsightColors;
import consulo.colorScheme.EditorColorsScheme;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.configurable.Configurable;
import consulo.configurable.SearchableConfigurable;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.colorScheme.ui.ColorAndFontDescriptionPanel;
import consulo.ide.impl.idea.application.options.colors.ColorAndFontOptions;
import consulo.colorScheme.ui.TextAttributesDescription;
import consulo.ide.impl.idea.codeInsight.daemon.impl.SeverityUtil;
import consulo.ide.impl.idea.openapi.options.colors.pages.GeneralColorsPage;
import consulo.configurable.Settings;
import consulo.ide.setting.ShowSettingsUtil;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl;
import consulo.language.editor.inspection.localize.InspectionLocalize;
import consulo.language.editor.rawHighlight.HighlightInfoType;
import consulo.language.editor.rawHighlight.HighlightInfoTypeImpl;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ListUtil;
import consulo.ui.style.StandardColors;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.Comparing;
import jakarta.annotation.Nullable;
import org.jdom.Element;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.util.List;
import java.util.*;

import static consulo.language.editor.impl.internal.rawHighlight.SeverityRegistrarImpl.SeverityBasedTextAttributes;

/**
 * @author anna
 * @since 2006-02-24
 */
public class SeverityEditorDialog extends DialogWrapper {
  private final JPanel myPanel;

  private final JList myOptionsList = new JBList();
  private final ColorAndFontDescriptionPanel myOptionsPanel = new ColorAndFontDescriptionPanel();

  private SeverityBasedTextAttributes myCurrentSelection;
  private static final Logger LOG = Logger.getInstance(SeverityEditorDialog.class);
  private final SeverityRegistrarImpl mySeverityRegistrar;
  private final CardLayout myCard;
  private final JPanel myRightPanel;
  @NonNls
  private static final String DEFAULT = "DEFAULT";
  @NonNls
  private static final String EDITABLE = "EDITABLE";

  public SeverityEditorDialog(Project project, HighlightSeverity severity, SeverityRegistrarImpl severityRegistrar) {
    super(project, true);
    mySeverityRegistrar = severityRegistrar;
    myOptionsList.setCellRenderer(new DefaultListCellRenderer() {
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component rendererComponent = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof SeverityBasedTextAttributes) {
          setText(((SeverityBasedTextAttributes)value).getSeverity().toString());
        }
        return rendererComponent;
      }
    });
    myOptionsList.addListSelectionListener(new ListSelectionListener() {
      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (myCurrentSelection != null) {
          apply(myCurrentSelection);
        }
        myCurrentSelection = (SeverityBasedTextAttributes)myOptionsList.getSelectedValue();
        if (myCurrentSelection != null) {
          reset(myCurrentSelection);
          myCard.show(myRightPanel, mySeverityRegistrar.isDefaultSeverity(myCurrentSelection.getSeverity()) ? DEFAULT : EDITABLE);
        }
      }
    });
    myOptionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    JPanel leftPanel = ToolbarDecorator.createDecorator(myOptionsList).setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        String name =
          Messages.showInputDialog(
            myPanel,
            InspectionLocalize.highlightSeverityCreateDialogNameLabel().get(),
            InspectionLocalize.highlightSeverityCreateDialogTitle().get(),
            Messages.getQuestionIcon(),
            "",
            new InputValidator() {
              @Override
              public boolean checkInput(String inputString) {
                ListModel listModel = myOptionsList.getModel();
                for (int i = 0; i < listModel.getSize(); i++) {
                  String severityName = ((SeverityBasedTextAttributes)listModel.getElementAt(i)).getSeverity().myName;
                  if (Comparing.strEqual(severityName, inputString)) return false;
                }
                return true;
              }

              @Override
              public boolean canClose(String inputString) {
                return checkInput(inputString);
              }
            }
          );
        if (name == null) return;
        TextAttributes textAttributes = CodeInsightColors.WARNINGS_ATTRIBUTES.getDefaultAttributes();
        HighlightInfoTypeImpl info = new HighlightInfoTypeImpl(
          new HighlightSeverity(name, 50),
          TextAttributesKey.createTextAttributesKey(name)
        );

        SeverityBasedTextAttributes newSeverityBasedTextAttributes = new SeverityBasedTextAttributes(textAttributes.clone(), info);
        ((DefaultListModel)myOptionsList.getModel()).addElement(newSeverityBasedTextAttributes);

        myOptionsList.clearSelection();
        ScrollingUtil.selectItem(myOptionsList, newSeverityBasedTextAttributes);
      }
    }).setMoveUpAction(button -> {
      apply(myCurrentSelection);
      ListUtil.moveSelectedItemsUp(myOptionsList);
    }).setMoveDownAction(button -> {
      apply(myCurrentSelection);
      ListUtil.moveSelectedItemsDown(myOptionsList);
    }).createPanel();
    ToolbarDecorator.findRemoveButton(leftPanel).addCustomUpdater(
      e -> !mySeverityRegistrar.isDefaultSeverity(((SeverityBasedTextAttributes)myOptionsList.getSelectedValue()).getSeverity())
    );
    ToolbarDecorator.findUpButton(leftPanel).addCustomUpdater(e -> {
      boolean canMove = ListUtil.canMoveSelectedItemsUp(myOptionsList);
      if (canMove) {
        SeverityBasedTextAttributes pair = (SeverityBasedTextAttributes)myOptionsList.getSelectedValue();
        if (pair != null && mySeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
          int newPosition = myOptionsList.getSelectedIndex() - 1;
          pair = (SeverityBasedTextAttributes)myOptionsList.getModel().getElementAt(newPosition);
          if (mySeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
            canMove = false;
          }
        }
      }

      return canMove;
    });
    ToolbarDecorator.findDownButton(leftPanel).addCustomUpdater(e -> {
      boolean canMove = ListUtil.canMoveSelectedItemsDown(myOptionsList);
      if (canMove) {
        SeverityBasedTextAttributes pair = (SeverityBasedTextAttributes)myOptionsList.getSelectedValue();
        if (pair != null && mySeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
          int newPosition = myOptionsList.getSelectedIndex() + 1;
          pair = (SeverityBasedTextAttributes)myOptionsList.getModel().getElementAt(newPosition);
          if (mySeverityRegistrar.isDefaultSeverity(pair.getSeverity())) {
            canMove = false;
          }
        }
      }

      return canMove;
    });

    myPanel = new JPanel(new BorderLayout());
    myPanel.add(leftPanel, BorderLayout.CENTER);
    myCard = new CardLayout();
    myRightPanel = new JPanel(myCard);
    JPanel disabled = new JPanel(new GridBagLayout());
    JButton button = new JButton(InspectionLocalize.severitiesDefaultSettingsMessage().get());
    button.addActionListener(e -> editColorsAndFonts());
    disabled.add(button, new GridBagConstraints(0, 0, 1, 1, 0, 0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    myRightPanel.add(DEFAULT, disabled);
    myRightPanel.add(EDITABLE, myOptionsPanel.getPanel());
    myCard.show(myRightPanel, EDITABLE);
    myPanel.add(myRightPanel, BorderLayout.EAST);
    fillList(severity);
    init();
    setTitle(InspectionLocalize.severitiesEditorDialogTitle());
    reset((SeverityBasedTextAttributes)myOptionsList.getSelectedValue());
  }

  private void editColorsAndFonts() {
    String toConfigure = getSelectedType().getSeverity(null).myName;
    doOKAction();
    myOptionsList.clearSelection();
    DataContext dataContext = DataManager.getInstance().getDataContext(myPanel);
    Settings optionsEditor = dataContext.getData(Settings.KEY);
    if (optionsEditor != null) {
      ColorAndFontOptions colorAndFontOptions = optionsEditor.findConfigurable(ColorAndFontOptions.class);
      assert colorAndFontOptions != null;
      SearchableConfigurable javaPage = colorAndFontOptions.findSubConfigurable(GeneralColorsPage.class);
      LOG.assertTrue(javaPage != null);
      optionsEditor.clearSearchAndSelect(javaPage).doWhenDone(() -> {
        Runnable runnable = javaPage.enableSearch(toConfigure);
        if (runnable != null) {
          SwingUtilities.invokeLater(runnable);
        }
      });
    }
    else {
      ColorAndFontOptions colorAndFontOptions = new ColorAndFontOptions();
      Configurable[] configurables = colorAndFontOptions.buildConfigurables();
      SearchableConfigurable javaPage = colorAndFontOptions.findSubConfigurable(GeneralColorsPage.class);
      LOG.assertTrue(javaPage != null);
      AsyncResult<Void> result = ShowSettingsUtil.getInstance().editConfigurable(dataContext.getData(Project.KEY), javaPage);

      result.doWhenProcessed(() -> {
        for (Configurable configurable : configurables) {
          configurable.disposeUIResources();
        }
        colorAndFontOptions.disposeUIResources();
      });
    }
  }

  private void fillList(HighlightSeverity severity) {
    DefaultListModel model = new DefaultListModel();
    model.removeAllElements();
    List<SeverityBasedTextAttributes> infoTypes = new ArrayList<>();
    infoTypes.addAll(SeverityUtil.getRegisteredHighlightingInfoTypes(mySeverityRegistrar));
    Collections.sort(infoTypes, (attributes1, attributes2) -> -mySeverityRegistrar.compare(attributes1.getSeverity(), attributes2.getSeverity()));
    SeverityBasedTextAttributes preselection = null;
    for (SeverityBasedTextAttributes type : infoTypes) {
      model.addElement(type);
      if (type.getSeverity().equals(severity)) {
        preselection = type;
      }
    }
    myOptionsList.setModel(model);
    myOptionsList.setSelectedValue(preselection, true);
  }


  private void apply(SeverityBasedTextAttributes info) {
    MyTextAttributesDescription description =
            new MyTextAttributesDescription(LocalizeValue.of(info.getType().toString()), LocalizeValue.empty(), new TextAttributes(), info.getType().getAttributesKey());
    myOptionsPanel.apply(description, null);
    Element textAttributes = new Element("temp");
    try {
      description.getTextAttributes().writeExternal(textAttributes);
      info.getAttributes().readExternal(textAttributes);
    }
    catch (Exception e) {
      LOG.error(e);
    }
  }

  private void reset(SeverityBasedTextAttributes info) {
    MyTextAttributesDescription description =
            new MyTextAttributesDescription(LocalizeValue.of(info.getType().toString()), LocalizeValue.empty(), info.getAttributes(), info.getType().getAttributesKey());
    Element textAttributes = new Element("temp");
    try {
      info.getAttributes().writeExternal(textAttributes);
      description.getTextAttributes().readExternal(textAttributes);
    }
    catch (Exception e) {
      LOG.error(e);
    }
    myOptionsPanel.reset(description);
  }

  @Override
  protected void doOKAction() {
    apply((SeverityBasedTextAttributes)myOptionsList.getSelectedValue());
    Collection<SeverityBasedTextAttributes> infoTypes = new HashSet<>(SeverityUtil.getRegisteredHighlightingInfoTypes(mySeverityRegistrar));
    ListModel listModel = myOptionsList.getModel();
    List<HighlightSeverity> order = new ArrayList<>();
    for (int i = listModel.getSize() - 1; i >= 0; i--) {
      SeverityBasedTextAttributes info = (SeverityBasedTextAttributes)listModel.getElementAt(i);
      order.add(info.getSeverity());
      if (!mySeverityRegistrar.isDefaultSeverity(info.getSeverity())) {
        infoTypes.remove(info);
        ColorValue stripeColor = info.getAttributes().getErrorStripeColor();
        mySeverityRegistrar.registerSeverity(info, stripeColor != null ? stripeColor : StandardColors.YELLOW);
      }
    }
    for (SeverityBasedTextAttributes info : infoTypes) {
      mySeverityRegistrar.unregisterSeverity(info.getSeverity());
    }
    mySeverityRegistrar.setOrder(order);
    super.doOKAction();
  }

  @Override
  @Nullable
  protected JComponent createCenterPanel() {
    return myPanel;
  }

  @Nullable
  public HighlightInfoType getSelectedType() {
    SeverityBasedTextAttributes selection = (SeverityBasedTextAttributes)myOptionsList.getSelectedValue();
    return selection != null ? selection.getType() : null;
  }

  private static class MyTextAttributesDescription extends TextAttributesDescription {
    public MyTextAttributesDescription(LocalizeValue name, LocalizeValue group, TextAttributes attributes, TextAttributesKey type) {
      super(name, group, attributes, type, null, null, null);
    }

    @Override
    public void apply(EditorColorsScheme scheme) {

    }

    @Override
    public boolean isErrorStripeEnabled() {
      return true;
    }


    @Override
    public TextAttributes getTextAttributes() {
      return super.getTextAttributes();
    }
  }
}
