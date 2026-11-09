/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.idea.ide.ui.customization;

import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.ex.keymap.localize.KeyMapLocalize;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogService;
import consulo.ui.layout.DockLayout;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * Chooses the icon of one action.
 *
 * @author VISTALL
 * @since 2026-11-09
 */
public class EditIconDialogDescriptor extends DialogDescriptor {
    private final CustomActionsSchemaImpl mySchema;
    private final CustomActionsSchemaImpl myApplicationSchema;
    private final String myActionId;
    private final boolean myInsideMenu;

    private @Nullable BrowseIconsComboBox myComboBox;

    public EditIconDialogDescriptor(CustomActionsSchemaImpl schema, CustomActionsSchemaImpl applicationSchema, String actionId, boolean insideMenu) {
        super(KeyMapLocalize.titleChooseActionIcon());
        mySchema = schema;
        myApplicationSchema = applicationSchema;
        myActionId = actionId;
        myInsideMenu = insideMenu;
    }

    /**
     * @return whether the icon of the action changed
     */
    @RequiredUIAccess
    public static CompletableFuture<Boolean> showAsync(
        Component parent,
        CustomActionsSchemaImpl schema,
        CustomActionsSchemaImpl applicationSchema,
        String actionId,
        boolean insideMenu
    ) {
        EditIconDialogDescriptor descriptor = new EditIconDialogDescriptor(schema, applicationSchema, actionId, insideMenu);
        return Application.get().getInstance(DialogService.class)
            .build(parent, descriptor)
            .showAsync()
            .thenApply(value -> descriptor.applyIcon());
    }

    @RequiredUIAccess
    @Override
    public Component createCenterComponent(Disposable uiDisposable) {
        BrowseIconsComboBox comboBox = new BrowseIconsComboBox(mySchema, myApplicationSchema, myInsideMenu);
        comboBox.selectIconForAction(myActionId);
        myComboBox = comboBox;

        DockLayout layout = DockLayout.create();
        layout.top(comboBox.getComponent());
        return layout;
    }

    @RequiredUIAccess
    @Override
    public Component getPreferredFocusedComponent() {
        BrowseIconsComboBox comboBox = myComboBox;
        return comboBox != null ? comboBox.getComboBox() : null;
    }

    @Override
    public String getDimensionServiceKey() {
        return getClass().getName();
    }

    private boolean applyIcon() {
        BrowseIconsComboBox comboBox = myComboBox;
        ActionIconInfo selected = comboBox != null ? comboBox.getSelectedItem() : null;
        return selected != null && CustomizableActionsPanel.setCustomIcon(mySchema, myApplicationSchema, myActionId, selected);
    }
}
