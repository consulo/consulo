/*
 * Copyright 2013-2018 consulo.io
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
package consulo.desktop.impl.ide;

import com.intellij.jna.JnaLoader;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.ui.mac.foundation.Foundation;
import com.intellij.ui.mac.foundation.ID;
import com.sun.jna.Callback;
import consulo.logging.Logger;

import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 2018-09-01
 */
public class MacTopMenuInitializer {
  private static final Logger LOG = Logger.getInstance(MacTopMenuInitializer.class);

  private static final Callback IMPL = new Callback() {
    @SuppressWarnings("unused")
    public void callback(ID self, String selector) {
      SwingUtilities.invokeLater(() -> {
        ActionManager am = ActionManager.getInstance();
        MouseEvent me = new MouseEvent(JOptionPane.getRootFrame(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, 0, 0, 1, false);
        am.tryToExecute(am.getAction("CheckForUpdate"), me, null, null, false);
      });
    }
  };

  public static void installAutoUpdateMenu() {
    try {
      if (JnaLoader.isLoaded()) {
        installAutoUpdateMenu0();
      }
    }
    catch (Throwable t) {
      LOG.warn(t);
    }
  }

  private static void installAutoUpdateMenu0() {
    ID pool = Foundation.invoke("NSAutoreleasePool", "new");

    ID app = Foundation.invoke("NSApplication", "sharedApplication");
    ID menu = Foundation.invoke(app, Foundation.createSelector("menu"));
    ID item = Foundation.invoke(menu, Foundation.createSelector("itemAtIndex:"), 0);
    ID appMenu = Foundation.invoke(item, Foundation.createSelector("submenu"));

    ID checkForUpdatesClass = Foundation.allocateObjcClassPair(Foundation.getObjcClass("NSMenuItem"), "NSCheckForUpdates");
    Foundation.addMethod(checkForUpdatesClass, Foundation.createSelector("checkForUpdates"), IMPL, "v");

    Foundation.registerObjcClassPair(checkForUpdatesClass);

    ID checkForUpdates = Foundation.invoke("NSCheckForUpdates", "alloc");
    Foundation.invoke(checkForUpdates, Foundation.createSelector("initWithTitle:action:keyEquivalent:"), Foundation.nsString("Check for Updates..."), Foundation.createSelector("checkForUpdates"),
                      Foundation.nsString(""));
    Foundation.invoke(checkForUpdates, Foundation.createSelector("setTarget:"), checkForUpdates);

    Foundation.invoke(appMenu, Foundation.createSelector("insertItem:atIndex:"), checkForUpdates, 1);
    Foundation.invoke(checkForUpdates, Foundation.createSelector("release"));

    Foundation.invoke(pool, Foundation.createSelector("release"));
  }
}
