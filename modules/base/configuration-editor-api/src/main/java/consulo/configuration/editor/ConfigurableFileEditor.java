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
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.internal.FileEditorWithModifiedIcon;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.ButtonStyle;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.CommonShortcuts;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
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

    private @Nullable Component myPreferredFocusedComponent;

    private @Nullable DockLayout myContentPanel;

    private @Nullable Component myApplyPanel;

    private boolean myDisposed;

    private boolean myModified;

    public ConfigurableFileEditor(Project project, VirtualFile virtualFile) {
        super(project, virtualFile);
    }


    protected abstract U createConfigurable();

    @RequiredUIAccess
    protected void init() {
        if (myConfigurable != null || myDisposed) {
            return;
        }

        myConfigurable = createConfigurable();

        Component component = createComponent(myConfigurable);

        myConfigurable.reset();

        myPreferredFocusedComponent = myConfigurable.getPreferredFocusedUIComponent();

        myContentPanel = DockLayout.create();
        myContentPanel.center(component);

        myApplyPanel = createApplyPanel();
        myApplyPanel.setVisible(false);
        myContentPanel.bottom(myApplyPanel);

        if (TargetAWT.to(myContentPanel) instanceof JComponent contentComponent) {
            DumbAwareAction.create(anActionEvent -> doSave()).registerCustomShortcutSet(CommonShortcuts.getSaveAll(), contentComponent, this);
        }

        myUpdateFuture = myProject.getUIAccess().getScheduler().scheduleWithFixedDelay(this::checkModified, 500, 500, TimeUnit.MILLISECONDS);
    }

    @RequiredUIAccess
    private Component createComponent(U configurable) {
        Component uiComponent = configurable.createUIComponent(this);
        return uiComponent == null ? DockLayout.create() : uiComponent;
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
                    MessageBoxes.okError(LocalizeValue.of(e.getMessage()))
                        .title(e.getTitle())
                        .showAsync(myContentPanel);
                }
            }
        }
    }

    @RequiredUIAccess
    private Component createApplyPanel() {
        DockLayout panel = DockLayout.create();
        panel.borderBuilder().topSet().apply();

        HorizontalLayout buttonsPanel = HorizontalLayout.create();
        buttonsPanel.paddingBuilder().allSet(Space.MEDIUM).apply();

        Button applyButton = Button.create(CommonLocalize.buttonApply(), event -> doSave());
        applyButton.addStyle(ButtonStyle.PRIMARY);
        buttonsPanel.add(applyButton);

        panel.right(buttonsPanel);
        return panel;
    }

    @RequiredUIAccess
    private void checkModified() {
        Component applyPanel = myApplyPanel;
        if (applyPanel == null || myDisposed) {
            return;
        }

        boolean modified = myConfigurable != null && myConfigurable.isModified();
        if (modified == myModified) {
            return;
        }

        myModified = modified;

        applyPanel.setVisible(modified);

        FileEditorManager.getInstance(myProject).refreshIconsAsync();
    }

    @Override
    public boolean isModified() {
        return myModified;
    }

    @Override
    @RequiredUIAccess
    public Component getUIComponent() {
        init();
        return myContentPanel;
    }

    @Override
    @RequiredUIAccess
    public @Nullable Component getPreferredFocusedUIComponent() {
        if (myDisposed) {
            return null;
        }

        init();
        return myPreferredFocusedComponent;
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
