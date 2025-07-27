/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ui.ex.awt.action;

import consulo.application.ApplicationManager;
import consulo.ui.ex.keymap.Keymap;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.keymap.event.KeymapManagerListener;
import consulo.ui.ex.update.Activatable;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.*;

import jakarta.annotation.Nullable;
import javax.swing.*;

public final class ShadowAction implements Disposable {

  private final AnAction myAction;
  private AnAction myCopyFromAction;
  private final JComponent myComponent;

  private final KeymapManagerListener myKeymapManagerListener;

  private ShortcutSet myShortcutSet;
  private String myActionId;

  private final Keymap.Listener myKeymapListener;
  private Keymap myKeymap;

  private Presentation myPresentation;
  private final UiNotifyConnector myUiNotify;

  public ShadowAction(AnAction action, AnAction copyFromAction, JComponent component, Presentation presentation) {
    this(action, copyFromAction, component);
    myPresentation = presentation;
  }

  public ShadowAction(AnAction action, AnAction copyFromAction, JComponent component) {
    myAction = action;

    myCopyFromAction = copyFromAction;
    myComponent = component;
    myActionId = ActionManager.getInstance().getId(myCopyFromAction);

    myAction.getTemplatePresentation().copyFrom(copyFromAction.getTemplatePresentation());

    myKeymapListener = new Keymap.Listener() {
      @Override
      public void onShortcutChanged(String actionId) {
        if (myActionId == null || actionId.equals(myActionId)) {
          rebound();
        }
      }
    };

    myKeymapManagerListener = new KeymapManagerListener() {
      @Override
      public void activeKeymapChanged(Keymap keymap) {
        rebound();
      }
    };

    myUiNotify = new UiNotifyConnector(myComponent, new Activatable() {
      @Override
      public void showNotify() {
        _connect();
      }

      @Override
      public void hideNotify() {
        disconnect();
      }
    });
  }

  private void _connect() {
    disconnect();
    KeymapManager mgr = getKeymapManager();
    if (mgr == null) return;


    mgr.addKeymapManagerListener(myKeymapManagerListener);
    rebound();
  }

  private void disconnect() {
    KeymapManager mgr = getKeymapManager();
    if (mgr == null) return;


    mgr.removeKeymapManagerListener(myKeymapManagerListener);
    if (myKeymap != null) {
      myKeymap.removeShortcutChangeListener(myKeymapListener);
    }
  }

  private void rebound() {
    KeymapManager mgr = getKeymapManager();
    if (mgr == null) return;

    myActionId = ActionManager.getInstance().getId(myCopyFromAction);
    if (myPresentation == null) {
      myAction.copyFrom(myCopyFromAction);
    } else {
      myAction.getTemplatePresentation().copyFrom(myPresentation);
      myAction.copyShortcutFrom(myCopyFromAction);
    }

    unregisterAll();

    myKeymap = mgr.getActiveKeymap();
    myKeymap.addShortcutChangeListener(myKeymapListener);

    if (myActionId == null) return;

    Shortcut[] shortcuts = myKeymap.getShortcuts(myActionId);
    myShortcutSet = new CustomShortcutSet(shortcuts);
    myAction.registerCustomShortcutSet(myShortcutSet, myComponent);
  }

  private void unregisterAll() {
    if (myShortcutSet != null) {
      myAction.unregisterCustomShortcutSet(myComponent);
    }

    if (myKeymap != null) {
      myKeymap.removeShortcutChangeListener(myKeymapListener);
    }
  }


  @Override
  public void dispose() {
    unregisterAll();
    myUiNotify.dispose();
    disconnect();
  }

  @Nullable
  private static KeymapManager getKeymapManager() {
    if (ApplicationManager.getApplication().isDisposed()) return null;
    return KeymapManager.getInstance();
  }

  public void reconnect(AnAction copyFromAction) {
    disconnect();
    myCopyFromAction = copyFromAction;
    _connect();
  }
}
