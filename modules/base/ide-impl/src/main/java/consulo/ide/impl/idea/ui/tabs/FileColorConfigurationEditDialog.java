/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ui.tabs;

import consulo.content.internal.scope.CustomScopesProviderEx;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.ide.impl.idea.notification.impl.ui.StickyButton;
import consulo.ide.impl.idea.notification.impl.ui.StickyButtonUI;
import consulo.language.editor.FileColorManager;
import consulo.language.editor.scope.NamedScopeManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.style.StyleManager;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.*;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public class FileColorConfigurationEditDialog extends DialogWrapper {
    @Nonnull
    private final Project myProject;
    private FileColorConfiguration myConfiguration;
    private JComboBox<NamedScope> myScopeComboBox;
    private final FileColorManager myManager;
    private Map<String, AbstractButton> myColorToButtonMap;
    private static final String CUSTOM_COLOR_NAME = "Custom";
    private final Map<String, NamedScope> myScopeById = new HashMap<>();

    public FileColorConfigurationEditDialog(@Nonnull Project project,
                                            @Nonnull FileColorManager manager,
                                            @Nullable FileColorConfiguration configuration) {
        super(true);
        myProject = project;

        setTitle(configuration == null ? "Add color label" : "Edit color label");
        setResizable(false);

        myManager = manager;
        myConfiguration = configuration;

        init();
        updateCustomButton();
        if (myConfiguration != null && !StringUtil.isEmpty(myConfiguration.getScopeName())) {
            myScopeComboBox.setSelectedItem(myConfiguration.getScopeName());
        }
        updateOKButton();
    }

    public JComboBox getScopeComboBox() {
        return myScopeComboBox;
    }

    @Override
    protected JComponent createNorthPanel() {
        JPanel result = new JPanel();
        result.setLayout(new BoxLayout(result, BoxLayout.Y_AXIS));

        List<NamedScope> scopeList = new ArrayList<>();
        NamedScopesHolder[] scopeHolders = NamedScopeManager.getAllNamedScopeHolders(myProject);
        for (NamedScopesHolder scopeHolder : scopeHolders) {
            NamedScope[] scopes = scopeHolder.getScopes();
            Collections.addAll(scopeList, scopes);
        }
        CustomScopesProviderEx.filterNoSettingsScopes(myProject, scopeList);
        for (NamedScope scope : scopeList) {
            myScopeById.put(scope.getScopeId(), scope);
        }

        myScopeComboBox = new ComboBox<>(new CollectionComboBoxModel<>(new ArrayList<>(myScopeById.values())));
        myScopeComboBox.setRenderer(new ColoredListCellRenderer<>() {
            @Override
            protected void customizeCellRenderer(@Nonnull JList<? extends NamedScope> list, NamedScope value, int index, boolean selected, boolean hasFocus) {
                append(value.getPresentableName());
            }
        });
        myScopeComboBox.addActionListener(e -> {
            updateCustomButton();
            updateOKButton();
        });

        JPanel pathPanel = new JPanel();
        pathPanel.setLayout(new BorderLayout());

        JLabel pathLabel = new JLabel("Scope:");
        pathLabel.setDisplayedMnemonic('S');
        pathLabel.setLabelFor(myScopeComboBox);
        pathPanel.add(pathLabel, BorderLayout.WEST);
        pathPanel.add(myScopeComboBox, BorderLayout.CENTER);

    /*
    final JButton newScope = new JButton("Add scope...");
    newScope.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        // TBD: refresh scope list
      }
    });
    pathPanel.add(newScope, BorderLayout.EAST);
    */

        result.add(pathPanel);

        JPanel colorPanel = new JPanel();
        colorPanel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        colorPanel.setLayout(new BoxLayout(colorPanel, BoxLayout.X_AXIS));
        JLabel colorLabel = new JLabel("Color:");
        colorPanel.add(colorLabel);
        colorPanel.add(createColorButtonsPanel(myConfiguration));
        colorPanel.add(Box.createHorizontalGlue());
        result.add(colorPanel);

        return result;
    }

    private void updateCustomButton() {
        Object item = myScopeComboBox.getSelectedItem();
        if (item instanceof NamedScope namedScope) {
            Color color = myConfiguration == null ? null : ColorUtil.fromHex(myConfiguration.getColorName(), null);
            CustomColorButton button = (CustomColorButton) myColorToButtonMap.get(CUSTOM_COLOR_NAME);

            if (color != null) {
                button.setColor(color);
                button.setSelected(true);
                button.repaint();
            }
        }
    }

    @Override
    protected void doOKAction() {
        close(OK_EXIT_CODE);

        NamedScope scope = (NamedScope) myScopeComboBox.getSelectedItem();
        if (myConfiguration != null) {
            myConfiguration.setScopeName(scope.getScopeId());
            myConfiguration.setColorName(getColorName());
        }
        else {
            myConfiguration = new FileColorConfiguration(scope.getScopeId(), getColorName());
        }
    }

    public FileColorConfiguration getConfiguration() {
        return myConfiguration;
    }

    private JComponent createColorButtonsPanel(FileColorConfiguration configuration) {
        JPanel result = new JPanel(new BorderLayout());
        result.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JPanel inner = new JPanel();
        inner.setLayout(new BoxLayout(inner, BoxLayout.X_AXIS));
        inner.setBorder(
            BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(JBColor.border(), 1),
                BorderFactory.createEmptyBorder(5, 5, 5, 5))
        );

        if (!StyleManager.get().getCurrentStyle().isDark()) {
            inner.setBackground(Color.WHITE);
        }

        result.add(inner, BorderLayout.CENTER);

        ButtonGroup group = new ButtonGroup();

        myColorToButtonMap = new HashMap<>();

        Collection<String> names = myManager.getColorNames();
        for (String name : names) {
            ColorButton colorButton = new ColorButton(name, myManager.getColor(name));
            group.add(colorButton);
            inner.add(colorButton);
            myColorToButtonMap.put(name, colorButton);
            inner.add(Box.createHorizontalStrut(5));
        }
        CustomColorButton customButton = new CustomColorButton();
        group.add(customButton);
        inner.add(customButton);
        myColorToButtonMap.put(customButton.getText(), customButton);
        inner.add(Box.createHorizontalStrut(5));


        if (configuration != null) {
            AbstractButton button = myColorToButtonMap.get(configuration.getColorName());
            if (button != null) {
                button.setSelected(true);
            }
        }

        return result;
    }

    @Nullable
    private String getColorName() {
        for (String name : myColorToButtonMap.keySet()) {
            AbstractButton button = myColorToButtonMap.get(name);
            if (button.isSelected()) {
                return button instanceof CustomColorButton ? ColorUtil.toHex(((CustomColorButton) button).getColor()) : name;
            }
        }
        return null;
    }

    @RequiredUIAccess
    @Override
    public JComponent getPreferredFocusedComponent() {
        return myScopeComboBox;
    }

    private void updateOKButton() {
        getOKAction().setEnabled(isOKActionEnabled());
    }

    @Override
    public boolean isOKActionEnabled() {
        NamedScope scopeName = (NamedScope) myScopeComboBox.getSelectedItem();
        return scopeName != null && getColorName() != null;
    }

    @Override
    protected JComponent createCenterPanel() {
        return null;
    }

    private class ColorButton extends StickyButton {
        protected Color myColor;

        protected ColorButton(String text, Color color) {
            super(FileColorManagerImpl.getAlias(text));
            setUI(new ColorButtonUI());
            myColor = color;
            addActionListener(this::doPerformAction);
            setBackground(new JBColor(Color.WHITE, UIUtil.getControlColor()));
            setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        }

        protected void doPerformAction(ActionEvent e) {
            updateOKButton();
        }

        Color getColor() {
            return myColor;
        }

        public void setColor(Color color) {
            myColor = color;
        }

        @Override
        public Color getForeground() {
            if (getModel().isSelected()) {
                return JBColor.foreground();
            }
            else if (getModel().isRollover()) {
                return JBColor.GRAY;
            }
            else {
                return getColor();
            }
        }

        @Override
        protected ButtonUI createUI() {
            return new ColorButtonUI();
        }
    }

    private class CustomColorButton extends ColorButton {
        private CustomColorButton() {
            super(CUSTOM_COLOR_NAME, Color.WHITE);
            myColor = null;
        }

        @Override
        protected void doPerformAction(ActionEvent e) {
            ColorChooser.chooseColor(FileColorConfigurationEditDialog.this.getRootPane(), "Choose Color", myColor, color -> {
                if (color != null) {
                    myColor = color;
                }

                setSelected(myColor != null);
                getOKAction().setEnabled(myColor != null);
            });
        }

        @Override
        public Color getForeground() {
            return getModel().isSelected() ? Color.BLACK : JBColor.GRAY;
        }

        @Override
        Color getColor() {
            return myColor == null ? Color.WHITE : myColor;
        }
    }

    private class ColorButtonUI extends StickyButtonUI<ColorButton> {

        @Override
        protected Color getBackgroundColor(ColorButton button) {
            return button.getColor();
        }

        @Override
        protected Color getFocusColor(ColorButton button) {
            return button.getColor().darker();
        }

        @Override
        protected Color getSelectionColor(ColorButton button) {
            return button.getColor();
        }

        @Override
        protected Color getRolloverColor(ColorButton button) {
            return button.getColor();
        }

        @Override
        protected int getArcSize() {
            return 20;
        }
    }
}
