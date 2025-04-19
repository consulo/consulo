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
import consulo.application.Application;
import consulo.application.eap.EarlyAccessProgramDescriptor;
import consulo.application.eap.EarlyAccessProgramManager;
import consulo.configurable.*;
import consulo.ide.localize.IdeLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import java.awt.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2013-10-15
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

    @Nonnull
    @Override
    public String getDisplayName() {
        return IdeLocalize.eapConfigurableName().get();
    }

    @Nullable
    @Override
    @RequiredUIAccess
    public JComponent createComponent() {
        myCheckBoxes = new LinkedHashMap<>();

        JPanel panel = new JPanel(new VerticalFlowLayout());

        List<EarlyAccessProgramDescription> descriptions = myApplication.getExtensionPoint(EarlyAccessProgramDescriptor.class)
            .collectExtensionsToListSafe(EarlyAccessProgramDescription::new);
        Collections.sort(descriptions);

        EarlyAccessProgramManager manager = ConfigurableSession.get().getOrCopy(myApplication, EarlyAccessProgramManager.class);

        for (EarlyAccessProgramDescription description : descriptions) {
            JPanel eapPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, true, true)) {
                @Override
                public Dimension getPreferredSize() {
                    Dimension size = super.getPreferredSize();
                    return new Dimension(Math.min(size.width, 200), size.height);
                }
            };
            eapPanel.setEnabled(description.available());

            JPanel topPanel = new JPanel(new BorderLayout());
            JCheckBox checkBox = new JCheckBox(description.name());
            checkBox.setEnabled(description.available());
            checkBox.addItemListener(e -> manager.setState(description.clazz(), checkBox.isSelected()));
            myCheckBoxes.put(description.descriptor(), checkBox);

            checkBox.setSelected(manager.getState(description.clazz()));
            topPanel.add(checkBox, BorderLayout.WEST);

            if (description.restartRequired()) {
                JBLabel comp = new JBLabel("Restart required");
                comp.setForeground(JBColor.GRAY);
                topPanel.add(comp, BorderLayout.EAST);
            }

            eapPanel.add(topPanel);
            eapPanel.setBorder(new CustomLineBorder(0, 0, 1, 0));

            JTextPane textPane = new JTextPane();
            textPane.setText(description.decription());
            textPane.setEditable(false);
            if (!description.available()) {
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

        JBLabel warnLabel = new JBLabel("WARNING", PlatformIconGroup.generalBalloonwarning(), SwingConstants.LEFT);
        warnLabel.setFont(UIUtil.getFont(UIUtil.FontSize.BIGGER, warnLabel.getFont()).deriveFont(Font.BOLD));
        panel.addComponent(warnLabel);
        JTextArea textArea = new JTextArea(IdeLocalize.eapConfigurableWarningText().get());
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
        return myApplication.getExtensionPoint(EarlyAccessProgramDescriptor.class)
            .anyMatchSafe(descriptor -> myCheckBoxes.get(descriptor).isSelected() != manager.getState(descriptor.getClass()));
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
        myApplication.getExtensionPoint(EarlyAccessProgramDescriptor.class)
            .forEach(descriptor -> myCheckBoxes.get(descriptor).setSelected(manager.getState(descriptor.getClass())));
    }
}
