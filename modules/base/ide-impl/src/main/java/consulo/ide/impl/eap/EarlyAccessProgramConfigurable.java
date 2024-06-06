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
package consulo.ide.impl.eap;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.configurable.*;
import consulo.ide.IdeBundle;
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.ui.ex.awt.VerticalFlowLayout;
import consulo.util.lang.StringUtil;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.LightColors;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.VerticalLayoutPanel;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import org.jetbrains.annotations.Nls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 16:30/15.10.13
 */
@ExtensionImpl
public class EarlyAccessProgramConfigurable implements ApplicationConfigurable, Configurable.NoScroll {
  private Map<EarlyAccessProgramDescriptor, JCheckBox> myCheckBoxes;

  private final Application myApplication;

  @Inject
  public EarlyAccessProgramConfigurable(Application application) {
    myApplication = application;
  }

  @Nonnull
  @Override
  public String getId() {
    return "eap";
  }

  @Nullable
  @Override
  public String getParentId() {
    return StandardConfigurableIds.PLATFORM_AND_PLUGINS_GROUP;
  }

  @Nls
  @Override
  public String getDisplayName() {
    return IdeBundle.message("eap.configurable.name");
  }

  @RequiredUIAccess
  @Nullable
  @Override
  public JComponent createComponent() {
    myCheckBoxes = new LinkedHashMap<>();

    JPanel panel = new JPanel(new VerticalFlowLayout());

    List<EarlyAccessProgramDescriptor> extensions = new ArrayList<>(EarlyAccessProgramDescriptor.EP_NAME.getExtensionList(myApplication));
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

    for (EarlyAccessProgramDescriptor descriptor : extensions) {
      JPanel eapPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, true, true)) {
        @Override
        public Dimension getPreferredSize() {
          Dimension size = super.getPreferredSize();
          return new Dimension(Math.min(size.width, 200), size.height);
        }
      };
      eapPanel.setEnabled(descriptor.isAvailable());

      JPanel topPanel = new JPanel(new BorderLayout());
      JCheckBox checkBox = new JCheckBox(descriptor.getName());
      checkBox.setEnabled(descriptor.isAvailable());
      checkBox.addItemListener(e -> manager.setState(descriptor.getClass(), checkBox.isSelected()));
      myCheckBoxes.put(descriptor, checkBox);
      
      checkBox.setSelected(manager.getState(descriptor.getClass()));
      topPanel.add(checkBox, BorderLayout.WEST);

      if (descriptor.isRestartRequired()) {
        JBLabel comp = new JBLabel("Restart required");
        comp.setForeground(JBColor.GRAY);
        topPanel.add(comp, BorderLayout.EAST);
      }

      eapPanel.add(topPanel);
      eapPanel.setBorder(new CustomLineBorder(0, 0, 1, 0));

      String description = StringUtil.notNullize(descriptor.getDescription());
      JTextPane textPane = new JTextPane();
      textPane.setText(description);
      textPane.setEditable(false);
      if (!descriptor.isAvailable()) {
        textPane.setForeground(JBColor.GRAY);
      }
      eapPanel.add(textPane);


      panel.add(eapPanel);
    }

    return JBUI.Panels.simplePanel().addToTop(createWarningPanel()).addToCenter(ScrollPaneFactory.createScrollPane(panel, true));
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
    myCheckBoxes = null;
  }

  @RequiredUIAccess
  @Override
  public boolean isModified() {
    EarlyAccessProgramManager manager = EarlyAccessProgramManager.getInstance();

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList(myApplication)) {
      JCheckBox box = myCheckBoxes.get(descriptor);

      if (box.isSelected() != manager.getState(descriptor.getClass())) {
        return true;
      }
    }
    return false;
  }

  @RequiredUIAccess
  @Override
  public void apply() throws ConfigurationException {
    // not need apply - because we already set it via listener, and session will copy it to manager
  }

  @RequiredUIAccess
  @Override
  public void reset() {
    EarlyAccessProgramManager manager = EarlyAccessProgramManager.getInstance();

    for (EarlyAccessProgramDescriptor descriptor : EarlyAccessProgramDescriptor.EP_NAME.getExtensionList(myApplication)) {
      JCheckBox box = myCheckBoxes.get(descriptor);

      box.setSelected(manager.getState(descriptor.getClass()));
    }
  }
}
