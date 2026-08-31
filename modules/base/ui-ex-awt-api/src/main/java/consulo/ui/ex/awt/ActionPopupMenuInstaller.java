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
package consulo.ui.ex.awt;

import consulo.ui.ex.action.ActionGroup;

/**
 * Implemented by the stand in a frontend without an awt hierarchy hands to platform code that asks for a
 * {@link javax.swing.JComponent}. A swing popup cannot be shown over such a component, so {@link PopupHandler}
 * lets it install the popup its own way instead of attaching a mouse listener that would never fire.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public interface ActionPopupMenuInstaller {
    void installActionPopupMenu(ActionGroup group, String place);

    /**
     * The group is resolved on every showing rather than once here - the customization schema can hand back a
     * different group after the user edits the menus.
     */
    void installActionPopupMenu(String groupId, String place);
}
