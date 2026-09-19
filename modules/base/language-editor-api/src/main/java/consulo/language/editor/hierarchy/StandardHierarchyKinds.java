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
package consulo.language.editor.hierarchy;

import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;

/**
 * The hierarchy kinds the platform ships, and the ones its bundled actions and keymaps are written
 * against. A language reuses these rather than declaring its own, so that a type hierarchy opened from
 * any language lands in the same toolbar, the same context menu and the same keymap entry.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public final class StandardHierarchyKinds {
    public static final HierarchyKind TYPE = HierarchyKind.builder("type")
        .actionPlace(ActionPlaces.TYPE_HIERARCHY_VIEW_TOOLBAR)
        .popupPlace(ActionPlaces.TYPE_HIERARCHY_VIEW_POPUP)
        .popupActionGroupId(IdeActions.GROUP_TYPE_HIERARCHY_POPUP)
        .shortcutActionId(IdeActions.ACTION_TYPE_HIERARCHY)
        .dragAndDrop(true)
        .occurrenceText(
            LanguageEditorLocalize.hierarchyTypeNextOccurenceName(),
            LanguageEditorLocalize.hierarchyTypePrevOccurenceName()
        )
        .build();

    public static final HierarchyKind METHOD = HierarchyKind.builder("method")
        .actionPlace(ActionPlaces.METHOD_HIERARCHY_VIEW_TOOLBAR)
        .popupPlace(ActionPlaces.METHOD_HIERARCHY_VIEW_POPUP)
        .popupActionGroupId(IdeActions.GROUP_METHOD_HIERARCHY_POPUP)
        .shortcutActionId(IdeActions.ACTION_METHOD_HIERARCHY)
        .occurrenceText(
            LanguageEditorLocalize.hierarchyMethodNextOccurenceName(),
            LanguageEditorLocalize.hierarchyMethodPrevOccurenceName()
        )
        .build();

    public static final HierarchyKind CALL = HierarchyKind.builder("call")
        .actionPlace(ActionPlaces.CALL_HIERARCHY_VIEW_TOOLBAR)
        .popupPlace(ActionPlaces.CALL_HIERARCHY_VIEW_POPUP)
        .popupActionGroupId(IdeActions.GROUP_CALL_HIERARCHY_POPUP)
        .shortcutActionId(IdeActions.ACTION_CALL_HIERARCHY)
        .occurrenceText(
            LanguageEditorLocalize.hierarchyCallNextOccurenceName(),
            LanguageEditorLocalize.hierarchyCallPrevOccurenceName()
        )
        .build();

    private StandardHierarchyKinds() {
    }
}
