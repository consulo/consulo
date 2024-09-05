/*
 * Copyright 2013-2024 consulo.io
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
package consulo.ide.impl.idea.ide.actionMacro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicImpl;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.keymap.KeymapUtil;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.event.AnActionListener;
import jakarta.inject.Inject;

import javax.swing.*;
import java.awt.event.KeyEvent;

/**
 * @author VISTALL
 * @since 2024-09-05
 */
@TopicImpl(ComponentScope.APPLICATION)
public class ActionMacroActionListener implements AnActionListener {
    private final ActionManager myActionManager;
    private final ActionMacroManager myActionMacroManager;

    @Inject
    public ActionMacroActionListener(ActionManager actionManager,
                                     ActionMacroManager actionMacroManager) {
        myActionManager = actionManager;
        myActionMacroManager = actionMacroManager;
    }

    @Override
    public void beforeActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        String id = myActionManager.getId(action);
        if (id == null) return;
        //noinspection HardCodedStringLiteral
        if ("StartStopMacroRecording".equals(id)) {
            myActionMacroManager.getLastActionInputEvents().add(event.getInputEvent());
        }
        else if (myActionMacroManager.isRecording()) {
            myActionMacroManager.getRecordingMacro().appendAction(id);

            String shortcut = null;
            if (event.getInputEvent() instanceof KeyEvent keyEvent) {
                shortcut = KeymapUtil.getKeystrokeText(KeyStroke.getKeyStrokeForEvent(keyEvent));
            }

            myActionMacroManager.notifyUser(dataContext, id + (shortcut != null ? " (" + shortcut + ")" : ""), false);
            myActionMacroManager.getLastActionInputEvents().add(event.getInputEvent());
        }
    }
}
