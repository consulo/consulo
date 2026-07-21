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
package consulo.it.internal;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.container.plugin.PluginId;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.KeyboardShortcut;
import consulo.ui.ex.action.TimerListener;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.util.concurrent.ActionCallback;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.awt.Component;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Headless {@code ActionManager}: the production impl lives in {@code ide-impl}. The action
 * registry itself works as real (a concurrent id-&gt;action map), which is what tool-window
 * registration ({@code ActivateToolWindowAction.ensureToolWindowActionRegistered}) needs during
 * project startup; UI factories (popup menus, toolbars) stay unsupported.
 *
 * @author VISTALL
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.INTEGRATION_TEST)
public class HeadlessActionManager extends ActionManager {
    private final Map<String, AnAction> myId2Action = new ConcurrentHashMap<>();

    @Override
    public ActionPopupMenu createActionPopupMenu(String place, ActionGroup group) {
        throw new UnsupportedOperationException("headless: action popup menus are not available");
    }

    @Override
    public ActionToolbar createActionToolbar(String place, ActionGroup group, boolean horizontal) {
        throw new UnsupportedOperationException("headless: action toolbars are not available");
    }

    @Override
    public AnAction getAction(String actionId) {
        return myId2Action.get(actionId);
    }

    @Override
    public @Nullable String getId(AnAction action) {
        for (Map.Entry<String, AnAction> entry : myId2Action.entrySet()) {
            if (entry.getValue() == action) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public void registerAction(String actionId, AnAction action) {
        myId2Action.put(actionId, action);
    }

    @Override
    public void registerAction(String actionId, AnAction action, @Nullable PluginId pluginId) {
        myId2Action.put(actionId, action);
    }

    @Override
    public String[] getPluginActions(PluginId pluginId) {
        return new String[0];
    }

    @Override
    public void unregisterAction(String actionId) {
        myId2Action.remove(actionId);
    }

    @Override
    public String[] getActionIds(String idPrefix) {
        List<String> ids = new ArrayList<>();
        for (String id : myId2Action.keySet()) {
            if (id.startsWith(idPrefix)) {
                ids.add(id);
            }
        }
        return ids.toArray(String[]::new);
    }

    @Override
    public boolean isGroup(String actionId) {
        return myId2Action.get(actionId) instanceof ActionGroup;
    }

    @Override
    public AnAction getActionOrStub(String id) {
        return myId2Action.get(id);
    }

    @Override
    public void addTimerListener(int delay, TimerListener listener) {
    }

    @Override
    public void removeTimerListener(TimerListener listener) {
    }

    @Override
    public void addTransparentTimerListener(int delay, TimerListener listener) {
    }

    @Override
    public void removeTransparentTimerListener(TimerListener listener) {
    }

    @Override
    public ActionCallback tryToExecute(
        AnAction action, InputEvent inputEvent, @Nullable Component contextComponent,
        @Nullable String place, boolean now
    ) {
        return ActionCallback.REJECTED;
    }

    @Override
    public void addAnActionListener(AnActionListener listener) {
    }

    @Override
    public void removeAnActionListener(AnActionListener listener) {
    }

    @Override
    public @Nullable KeyboardShortcut getKeyboardShortcut(String actionId) {
        return null;
    }
}
