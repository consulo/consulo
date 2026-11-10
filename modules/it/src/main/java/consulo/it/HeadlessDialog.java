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
package consulo.it;

import consulo.it.internal.ui.HeadlessWindow;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.dialog.Dialog;
import consulo.ui.ex.dialog.DialogDescriptor;
import consulo.ui.ex.dialog.DialogValue;
import consulo.ui.ex.dialog.action.DialogCancelAction;
import consulo.ui.ex.dialog.action.DialogOkAction;
import consulo.ui.layout.Layout;
import org.jspecify.annotations.Nullable;

/**
 * A dialog shown to a test: its components can be looked up by id and its buttons pressed.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
public final class HeadlessDialog {
    private final Dialog myDialog;

    HeadlessDialog(Dialog dialog) {
        myDialog = dialog;
    }

    public DialogDescriptor getDescriptor() {
        return myDialog.getDescriptor();
    }

    /**
     * @throws AssertionError when the dialog has no component of the id and type
     */
    @RequiredUIAccess
    public <C extends Component> C find(String id, Class<C> type) {
        Component content = myDialog.getWindow() instanceof HeadlessWindow window ? window.getContent() : null;
        Component found = content == null ? null : find(content, id);
        if (!type.isInstance(found)) {
            throw new AssertionError("no " + type.getSimpleName() + " with id '" + id + "' in " + getDescriptor().getId());
        }
        return type.cast(found);
    }

    @RequiredUIAccess
    private static @Nullable Component find(Component component, String id) {
        if (id.equals(component.getId())) {
            return component;
        }
        if (component instanceof Layout<?> layout) {
            Component[] result = new Component[1];
            layout.forEachChild(child -> {
                if (result[0] == null) {
                    result[0] = find(child, id);
                }
            });
            return result[0];
        }
        return null;
    }

    /**
     * Presses the OK button as a click would: the descriptor may keep the dialog open, for example while a
     * refactoring prepares.
     *
     * @throws AssertionError when the OK button is disabled
     */
    @RequiredUIAccess
    public void pressOk() {
        DialogDescriptor descriptor = getDescriptor();
        if (!descriptor.doUpdateOkButtonState()) {
            throw new AssertionError("OK is disabled in " + descriptor.getId());
        }
        DialogOkAction action = new DialogOkAction(LocalizeValue.empty());
        DialogValue value = descriptor.getOkValue();
        if (descriptor.canHandle(action, value, myDialog.getWindow())) {
            descriptor.onHandleValue(action, value);
            myDialog.doOkAction(value);
        }
    }

    @RequiredUIAccess
    public void pressCancel() {
        DialogDescriptor descriptor = getDescriptor();
        DialogCancelAction action = new DialogCancelAction();
        if (descriptor.canHandle(action, null, myDialog.getWindow())) {
            descriptor.onHandleValue(action, null);
            myDialog.doCancelAction();
        }
    }
}
