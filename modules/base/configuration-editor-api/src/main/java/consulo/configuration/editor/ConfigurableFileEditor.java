/*
 * Copyright 2013-2025 consulo.io
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
package consulo.configuration.editor;

import consulo.configurable.ConfigurationException;
import consulo.configurable.UnnamedConfigurable;
import consulo.configurable.internal.ConfigurableUIMigrationUtil;
import consulo.fileEditor.internal.FileEditorWithModifiedIcon;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2025-01-09
 */
public abstract class ConfigurableFileEditor<U extends UnnamedConfigurable> extends ConfigurationFileEditor implements FileEditorWithModifiedIcon {
    private Future<?> myUpdateFuture = CompletableFuture.completedFuture(null);

    protected U myConfigurable;

    private JComponent myPreferedFocusedComponent;

    private JPanel myContentPanel;

    private boolean myDisposed;

    public ConfigurableFileEditor(Project project, VirtualFile virtualFile) {
        super(project, virtualFile);
    }

    @Nonnull
    protected abstract U createConfigurable();

    @RequiredUIAccess
    protected void init() {
        if (myConfigurable != null || myDisposed) {
            return;
        }

        myConfigurable = createConfigurable();
        JComponent component = ConfigurableUIMigrationUtil.createComponent(myConfigurable, this);
        assert component != null;
        UiNotifyConnector.doWhenFirstShown(component, () -> {
            myUpdateFuture = myProject.getUIAccess().getScheduler().scheduleWithFixedDelay(this::checkModified, 500, 500, TimeUnit.MILLISECONDS);
        });
        myPreferedFocusedComponent = ConfigurableUIMigrationUtil.getPreferredFocusedComponent(myConfigurable);

        myContentPanel = new JPanel(new BorderLayout());
        myContentPanel.add(component, BorderLayout.CENTER);

        DumbAwareAction.create(anActionEvent -> {
            doSave();
        }).registerCustomShortcutSet(CommonShortcuts.getSaveAll(), myContentPanel, this);
    }

    protected void onApply(U configurable) {
    }

    @RequiredUIAccess
    private void doSave() {
        if (myConfigurable.isModified()) {
            try {
                myConfigurable.apply();

                onApply(myConfigurable);
            }
            catch (ConfigurationException e) {
                if (e.getMessage() != null) {
                    Messages.showMessageDialog(myProject, e.getMessage(), e.getTitle(), Messages.getErrorIcon());
                }
            }
        }
    }

    @RequiredUIAccess
    private void checkModified() {
        BorderLayout layout = (BorderLayout) myContentPanel.getLayout();
        Component layoutComponent = layout.getLayoutComponent(BorderLayout.SOUTH);

        if (myConfigurable != null && myConfigurable.isModified()) {
            if (layoutComponent != null) {
                return;
            }

            // bottom panel
            DockLayout panel = DockLayout.create();
            panel.addBorder(BorderPosition.TOP, BorderStyle.LINE, 1);

            HorizontalLayout buttonsPanel = HorizontalLayout.create();
            buttonsPanel.addBorders(BorderStyle.EMPTY, null, 5);

            Button applyButton = Button.create(CommonLocalize.buttonApply(), event -> doSave());
            applyButton.addStyle(ButtonStyle.PRIMARY);
            buttonsPanel.add(applyButton);

            panel.right(buttonsPanel);

            myContentPanel.add(TargetAWT.to(panel), BorderLayout.SOUTH);

            panel.forceRepaint();

            myContentPanel.validate();
            myContentPanel.repaint();
        }
        else {
            if (layoutComponent != null) {
                myContentPanel.remove(layoutComponent);
            }
        }
    }

    @Nonnull
    @Override
    @RequiredUIAccess
    public JComponent getComponent() {
        init();
        return myContentPanel;
    }

    @Nullable
    @Override
    @RequiredUIAccess
    public JComponent getPreferredFocusedComponent() {
        if (myDisposed) {
            return null;
        }
        
        init();
        return myPreferedFocusedComponent;
    }

    @Override
    @RequiredUIAccess
    public void dispose() {
        myDisposed = true;

        myUpdateFuture.cancel(false);

        if (myConfigurable != null) {
            myConfigurable.disposeUIResources();
            myConfigurable = null;
        }
    }
}
