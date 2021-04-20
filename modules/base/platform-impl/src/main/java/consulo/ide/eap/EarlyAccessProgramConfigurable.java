/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ide.eap;

import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.JBColor;
import com.intellij.ui.LightColors;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.desktop.util.awt.component.VerticalLayoutPanel;
import consulo.roots.ui.configuration.session.ConfigurableSession;
import consulo.ui.annotation.RequiredUIAccess;
import org.jetbrains.annotations.Nls;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 16:30/15.10.13
 */
public class EarlyAccessProgramConfigurable implements Configurable, Configurable.NoScroll {
  public static class EarlyAccessCellRender implements ListCellRenderer {
    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
      CheckBoxList checkBoxList = (CheckBoxList)list;
      EarlyAccessProgramDescriptor earlyAccessProgramDescriptor = (EarlyAccessProgramDescriptor)checkBoxList.getItemAt(index);

      JCheckBox checkbox = (JCheckBox)value;

      checkbox.setEnabled(list.isEnabled());
      checkbox.setFocusPainted(false);
      checkbox.setBorderPainted(true);

      if (earlyAccessProgramDescriptor == null) {
        return checkbox;
      }
      else {
        checkbox.setEnabled(earlyAccessProgramDescriptor.isAvailable());

        JPanel panel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, true, true)) {
          @Override
          public Dimension getPreferredSize() {
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.min(size.width, 200), size.height);
          }
        };
        panel.setEnabled(earlyAccessProgramDescriptor.isAvailable());

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(checkbox, BorderLayout.WEST);

        if (earlyAccessProgramDescriptor.isRestartRequired()) {
          JBLabel comp = new JBLabel("Restart required");
          comp.setForeground(JBColor.GRAY);
          topPanel.add(comp, BorderLayout.EAST);
        }

        panel.add(topPanel);
        panel.setBorder(new CustomLineBorder(0, 0, 1, 0));

        String description = StringUtil.notNullizeIfEmpty(earlyAccessProgramDescriptor.getDescription(), "Description is not available");
        JTextPane textPane = new JTextPane();
        textPane.setText(description);
        textPane.setEditable(false);
        if (!earlyAccessProgramDescriptor.isAvailable()) {
          textPane.setForeground(JBColor.GRAY);
        }
        panel.add(textPane);
        return panel;
      }
    }
  }

  private CheckBoxList<EarlyAccessProgramDescriptor> myList;

  private final Application myApplication;

  public EarlyAccessProgramConfigurable(Application application) {
    myApplication = application;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return IdeBundle.message("eap.configurable.name");
  }

  @Nullable
  @Override
  public String getHelpTopic() {
    return "platform/preferences/eap";
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent() {
    myList = new CheckBoxList<>();
    myList.setBorder(null);

    List<EarlyAccessProgramDescriptor> extensions = new ArrayList<>(EarlyAccessProgramDescriptor.EP_NAME.getExtensionList());
    extensions.sort((o1, o2) -> {
      if (o1.isAvailable() && !o2.isAvailable()) {
        return -1;
      }
      else if (o2.isAvailable() && !o1.isAvailable()) {
        return 1;
      }
      return o1.getName().compareToIgnoreCase(o2.getName());
    });

    EarlyAccessProgramManager manager = ConfigurableSession.get().getOrCopy(myApplication, EarlyAccessProgramManager.class);

    myList.setItems(extensions, EarlyAccessProgramDescriptor::getName, desc -> manager.getState(desc.getClass()));
    myList.setCellRenderer(new EarlyAccessCellRender());
    myList.setCheckBoxListListener((index, value) -> {
      EarlyAccessProgramDescriptor descriptor = extensions.get(index);
      manager.setState(descriptor.getClass(), value);
    });
    return JBUI.Panels.simplePanel().addToTop(createWarningPanel()).addToCenter(ScrollPaneFactory.createScrollPane(myList, true));
  }

  private static JComponent createWarningPanel() {
    VerticalLayoutPanel panel = JBUI.Panels.verticalPanel();
    panel.setBackground(LightColors.RED);
    panel.setBorder(new CompoundBorder(JBUI.Borders.customLine(JBColor.GRAY), JBUI.Borders.empty(5)));

    JBLabel warnLabel = new JBLabel("WARNING", AllIcons.General.BalloonWarning, SwingConstants.LEFT);
    warnLabel.setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, warnLabel.getFont()).deriveFont(Font.BOLD));
    panel.addComponent(warnLabel);
    JTextArea textArea = new JTextArea(IdeBundle.message("eap.configurable.warning.text"));
    textArea.setLineWrap(true);
    textArea.setFont(JBUI.Fonts.label());
    textArea.setOpaque(false);
    textArea.setEditable(false);
    panel.addComponent(textArea);
    return panel;
  }

  @RequiredUIAccess
  @Override
  public void disposeUIResources() {
    myList = null;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    EarlyAccessProgramManager manager = ConfigurableSession.get().getOrCopy(myApplication, EarlyAccessProgramManager.class);

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList()) {
      if (myList.isItemSelected(descriptor) != manager.getState(descriptor.getClass())) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    EarlyAccessProgramManager manager = ConfigurableSession.get().getOrCopy(myApplication, EarlyAccessProgramManager.class);

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList()) {
      manager.setState(descriptor.getClass(), myList.isItemSelected(descriptor));
    }
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    EarlyAccessProgramManager manager = ConfigurableSession.get().getOrCopy(myApplication, EarlyAccessProgramManager.class);

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList()) {
      myList.setItemSelected(descriptor, manager.getState(descriptor.getClass()));
    }
  }
}
