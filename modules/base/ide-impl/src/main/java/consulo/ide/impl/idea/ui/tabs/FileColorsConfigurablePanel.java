/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

import consulo.application.ui.UISettings;
import consulo.configurable.Settings;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.ide.util.scopeChooser.ScopeChooserConfigurable;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.IdeBorderFactory;
import consulo.ui.ex.awt.JBLabel;
import consulo.ui.ex.awt.ToolbarDecorator;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author spleaner
 * @author Konstantin Bulenkov
 */
public class FileColorsConfigurablePanel extends JPanel implements Disposable {
    private FileColorManagerImpl myManager;
    private final JCheckBox myEnabledCheckBox;
    private final JCheckBox myTabsEnabledCheckBox;
    private final JCheckBox myProjectViewEnabledCheckBox;
    private final FileColorSettingsTable myLocalTable;
    private final FileColorSettingsTable mySharedTable;

    public FileColorsConfigurablePanel(@Nonnull Project project, @Nonnull FileColorManagerImpl manager) {
        setLayout(new BorderLayout());
        myManager = manager;

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.X_AXIS));

        myEnabledCheckBox = new JCheckBox("Enable File Colors");
        myEnabledCheckBox.setMnemonic('F');
        topPanel.add(myEnabledCheckBox);

        myTabsEnabledCheckBox = new JCheckBox("Use in Editor Tabs");
        myTabsEnabledCheckBox.setMnemonic('T');
        topPanel.add(myTabsEnabledCheckBox);

        myProjectViewEnabledCheckBox = new JCheckBox("Use in Project View");
        myProjectViewEnabledCheckBox.setMnemonic('P');
        topPanel.add(myProjectViewEnabledCheckBox);

        topPanel.add(Box.createHorizontalGlue());

        add(topPanel, BorderLayout.NORTH);

        JPanel mainPanel = new JPanel(new GridLayout(2, 1));
        mainPanel.setPreferredSize(new Dimension(300, 500));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 0));

        final List<FileColorConfiguration> localConfigurations = manager.getLocalConfigurations();
        myLocalTable = new FileColorSettingsTable(project, manager, localConfigurations) {
            @Override
            protected void apply(@Nonnull List<FileColorConfiguration> configurations) {
                List<FileColorConfiguration> copied = new ArrayList<>();
                try {
                    for (FileColorConfiguration configuration : configurations) {
                        copied.add(configuration.clone());
                    }
                }
                catch (CloneNotSupportedException e) {//
                }
                manager.getModel().setConfigurations(copied, false);
            }
        };

        JPanel panel = ToolbarDecorator.createDecorator(myLocalTable)
            .addExtraAction(new AnAction(
                LocalizeValue.localizeTODO("Share"),
                LocalizeValue.empty(),
                PlatformIconGroup.actionsShow()
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    share();
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation().setEnabled(myLocalTable.getSelectedRow() != -1);
                }
            })
            .createPanel();
        JPanel localPanel = new JPanel(new BorderLayout());
        localPanel.setBorder(IdeBorderFactory.createTitledBorder("Local colors", false));
        localPanel.add(panel, BorderLayout.CENTER);
        mainPanel.add(localPanel);

        mySharedTable = new FileColorSettingsTable(project, manager, manager.getSharedConfigurations()) {
            @Override
            protected void apply(@Nonnull List<FileColorConfiguration> configurations) {
                List<FileColorConfiguration> copied = new ArrayList<>();
                for (FileColorConfiguration configuration : configurations) {
                    try {
                        copied.add(configuration.clone());
                    }
                    catch (CloneNotSupportedException e) {
                        assert false : "Should not happen!";
                    }
                }
                manager.getModel().setConfigurations(copied, true);
            }
        };

        JPanel sharedPanel = new JPanel(new BorderLayout());
        sharedPanel.setBorder(IdeBorderFactory.createTitledBorder("Shared colors", false));
        JPanel shared = ToolbarDecorator.createDecorator(mySharedTable)
            .addExtraAction(new AnAction(
                LocalizeValue.localizeTODO("Unshare"),
                LocalizeValue.empty(),
                PlatformIconGroup.actionsCancel()
            ) {
                @Override
                @RequiredUIAccess
                public void actionPerformed(@Nonnull AnActionEvent e) {
                    unshare();
                }

                @Override
                public void update(@Nonnull AnActionEvent e) {
                    e.getPresentation().setEnabled(mySharedTable.getSelectedRow() != -1);
                }
            })
            .createPanel();

        sharedPanel.add(shared, BorderLayout.CENTER);
        mainPanel.add(sharedPanel);
        add(mainPanel, BorderLayout.CENTER);

        JPanel infoPanel = new JPanel(new BorderLayout());
        infoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        infoPanel.add(new JBLabel("Scopes are processed from top to bottom with Local colors first.",
            MessageType.INFO.getDefaultIcon(), SwingConstants.LEFT
        ));
        JButton editScopes = new JButton("Manage Scopes...");
        editScopes.addActionListener(e -> {
            DataContext dataContext = DataManager.getInstance().getDataContext(infoPanel);

            Settings settings = dataContext.getData(Settings.KEY);
            if (settings != null) {
                settings.select(ScopeChooserConfigurable.class);
            }
        });
        infoPanel.add(editScopes, BorderLayout.EAST);
        add(infoPanel, BorderLayout.SOUTH);

        myLocalTable.getEmptyText().setText("No local colors");
        mySharedTable.getEmptyText().setText("No shared colors");
    }

    private void unshare() {
        int rowCount = mySharedTable.getSelectedRowCount();
        if (rowCount > 0) {
            int[] rows = mySharedTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                FileColorConfiguration removed = mySharedTable.removeConfiguration(rows[i]);
                if (removed != null) {
                    myLocalTable.addConfiguration(removed);
                }
            }
        }
    }

    private void share() {
        int rowCount = myLocalTable.getSelectedRowCount();
        if (rowCount > 0) {
            int[] rows = myLocalTable.getSelectedRows();
            for (int i = rows.length - 1; i >= 0; i--) {
                FileColorConfiguration removed = myLocalTable.removeConfiguration(rows[i]);
                if (removed != null) {
                    mySharedTable.addConfiguration(removed);
                }
            }
        }
    }

    @Override
    public void dispose() {
        myManager = null;
    }

    public boolean isModified() {
        boolean modified;

        modified = myEnabledCheckBox.isSelected() != myManager.isEnabled();
        modified |= myTabsEnabledCheckBox.isSelected() != myManager.isEnabledForTabs();
        modified |= myProjectViewEnabledCheckBox.isSelected() != myManager.isEnabledForProjectView();
        modified |= myLocalTable.isModified() || mySharedTable.isModified();

        return modified;
    }

    public void apply() {
        myManager.setEnabled(myEnabledCheckBox.isSelected());
        myManager.setEnabledForTabs(myTabsEnabledCheckBox.isSelected());
        myManager.setEnabledForProjectView(myProjectViewEnabledCheckBox.isSelected());

        myLocalTable.apply();
        mySharedTable.apply();

        UISettings.getInstance().fireUISettingsChanged();
    }

    public void reset() {
        myEnabledCheckBox.setSelected(myManager.isEnabled());
        myTabsEnabledCheckBox.setSelected(myManager.isEnabledForTabs());
        myProjectViewEnabledCheckBox.setSelected(myManager.isEnabledForProjectView());

        if (myLocalTable.isModified()) {
            myLocalTable.reset();
        }
        if (mySharedTable.isModified()) {
            mySharedTable.reset();
        }
    }
}
