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
package consulo.externalTool.impl.internal;

import consulo.execution.ui.console.InvalidExpressionException;
import consulo.execution.ui.console.RegexpFilter;
import consulo.externalTool.impl.internal.localize.ExternalToolLocalize;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.JBPopupMenu;
import consulo.ui.ex.awt.Messages;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.ui.ex.awt.PopupHandler;
import consulo.application.CommonBundle;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Yura Cangea
 */
class FilterDialog extends DialogWrapper {
  private final JTextField myRegexpField = new JTextField();
  private final JTextField myNameField = new JTextField();
  private final JTextField myDescriptionField = new JTextField();

  private JPopupMenu myPopup;

  private FilterDialog(Component component) {
    super(component, true);
    init();
    setOKActionEnabled(true);
    myRegexpField.setToolTipText(ExternalToolLocalize.toolsFiltersAddMacroTooltip().get());
  }

  public static boolean editFilter(FilterInfo filterInfo, JComponent parentComponent, String title) throws InvalidExpressionException {
    FilterDialog dialog = new FilterDialog(parentComponent);
    dialog.setTitle(title);
    dialog.myNameField.setText(filterInfo.getName());
    dialog.myDescriptionField.setText(filterInfo.getDescription());
    dialog.myRegexpField.setText(filterInfo.getRegExp());
    dialog.show();
    if (!dialog.isOK()) return false;
    filterInfo.setName(dialog.myNameField.getText());
    filterInfo.setDescription(dialog.myDescriptionField.getText());
    filterInfo.setRegExp(dialog.myRegexpField.getText());
    return true;
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myRegexpField;
  }

  @Override
  protected JComponent createCenterPanel() {
    JPanel mainPanel = new JPanel(new BorderLayout());

    JPanel panel = new JPanel(new GridBagLayout());

    GridBagConstraints constr;

    constr = new GridBagConstraints();
    constr.gridx = 0;
    constr.gridy = 0;
    constr.anchor = GridBagConstraints.WEST;
    constr.weighty = 0;
    constr.gridwidth = 1;
    constr.insets = new Insets(5, 0, 0, 0);
    panel.add(new JLabel(ExternalToolLocalize.toolsFiltersAddNameLabel().get()), constr);

    constr.gridx = 0;
    constr.gridy = 1;
    constr.weightx = 1;
    constr.gridwidth = 3;
    constr.fill = GridBagConstraints.HORIZONTAL;
    panel.add(myNameField, constr);

    constr.gridx = 0;
    constr.gridy = 2;
    constr.weightx = 0;
    panel.add(new JLabel(ExternalToolLocalize.toolsFiltersAddDescriptionLabel().get()), constr);

    constr.gridx = 0;
    constr.gridy = 3;
    constr.gridwidth = 2;
    constr.weightx = 1;
    panel.add(myDescriptionField, constr);

    constr.gridy = 4;
    constr.gridx = 0;
    constr.gridwidth = 2;
    constr.weightx = 0;
    panel.add(new JLabel(ExternalToolLocalize.toolsFiltersAddRegexLabel().get()), constr);

    constr.gridx = 0;
    constr.gridy = 5;
    constr.gridwidth = 3;
    panel.add(myRegexpField, constr);

    makePopup();

    panel.setPreferredSize(new Dimension(335, 160));

    mainPanel.add(panel, BorderLayout.NORTH);

    return mainPanel;
  }

  private void makePopup() {
    myPopup = new JBPopupMenu();
    String[] macrosName = RegexpFilter.getMacrosName();
    JMenuItem[] items = new JMenuItem[macrosName.length];
    for (int i = 0; i < macrosName.length; i++) {
      items[i] = myPopup.add(macrosName[i]);
      items[i].addActionListener(new MenuItemListener(macrosName[i]));
    }
    myRegexpField.addMouseListener(new PopupListener());
  }

  @Override
  protected void doOKAction() {
    String errorMessage = null;
    if (noText(myNameField.getText())) {
      errorMessage = ExternalToolLocalize.toolsFiltersAddNameRequiredError().get();
    } else if (noText(myRegexpField.getText())) {
      errorMessage = ExternalToolLocalize.toolsFiltersAddRegexRequiredError().get();
    }

    if (errorMessage != null) {
      Messages.showMessageDialog(getContentPane(), errorMessage, CommonBundle.getErrorTitle(), Messages.getErrorIcon());
      return;
    }

    try {
      checkRegexp(myRegexpField.getText());
    } catch (InvalidExpressionException e) {
      Messages.showMessageDialog(getContentPane(), e.getMessage(), ExternalToolLocalize.toolsFiltersAddRegexInvalidTitle().get(), Messages.getErrorIcon());
      return;
    }
    super.doOKAction();
  }

  private void checkRegexp(String regexpText) {
    RegexpFilter.validate(regexpText);
  }

  private boolean noText(String text) {
    return "".equals(text);
  }

  @Override
  protected String getDimensionServiceKey(){
    return "#consulo.externalTool.impl.internal.FilterDialog";
  }

  @Override
  protected String getHelpId() {
    return "reference.settings.ide.settings.external.tools.output.filters.add.filter";
  }

  private class MenuItemListener implements ActionListener {
    private final String myMacrosName;

    private MenuItemListener(String macrosName) {
      myMacrosName = macrosName;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      int position = myRegexpField.getCaretPosition();
      try {
        if (myRegexpField.getText().indexOf(myMacrosName) == -1) {
          myRegexpField.getDocument().insertString(position, myMacrosName, null);
          myRegexpField.setCaretPosition(position + myMacrosName.length());
        }
      } catch (BadLocationException ex) {
      }
      IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myRegexpField);
    }
  }

  private class PopupListener extends PopupHandler {
    @Override
    public void invokePopup(Component comp, int x, int y) {
      myPopup.show(comp, x, y);
    }
  }
}
