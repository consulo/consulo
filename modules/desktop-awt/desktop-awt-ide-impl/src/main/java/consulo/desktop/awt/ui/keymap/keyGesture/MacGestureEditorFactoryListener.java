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
package consulo.desktop.awt.ui.keymap.keyGesture;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.util.SystemInfo;
import consulo.codeEditor.event.EditorFactoryEvent;
import consulo.codeEditor.event.EditorFactoryListener;
import consulo.platform.Platform;

/**
 * Hooks the mac trackpad gestures onto every created editor. Registered from the ide layer so the editor
 * itself stays free of the keymap dispatchers the gesture support feeds into.
 *
 * @author VISTALL
 * @since 2026-08-17
 */
@ExtensionImpl
public class MacGestureEditorFactoryListener implements EditorFactoryListener {
    @Override
    public void editorCreated(EditorFactoryEvent event) {
        if (Platform.current().os().isMac() && SystemInfo.isJetBrainsJvm) {
            new MacGestureSupportForEditor(event.getEditor().getComponent());
        }
    }
}
