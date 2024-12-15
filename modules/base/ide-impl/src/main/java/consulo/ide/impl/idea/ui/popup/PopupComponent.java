/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.ui.popup;

import consulo.awt.hacking.PopupHacking;
import consulo.logging.Logger;
import consulo.ui.ex.awt.DialogWrapper;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.PopupUtil;
import consulo.ui.ex.popup.JBPopup;

import javax.swing.*;
import java.awt.*;

public interface PopupComponent {
    Logger LOG = Logger.getInstance(PopupComponent.class);

    void hide(boolean dispose);

    void show();

    Window getWindow();

    void setRequestFocus(boolean requestFocus);

    boolean isPopupWindow(Window window);

    interface Factory {
        PopupComponent getPopup(Component owner, Component content, int x, int y, JBPopup jbPopup);

        boolean isNativePopup();

        boolean isNativeBorder();

        class AwtDefault implements Factory {
            private boolean myNativeBorder;

            @Override
            public PopupComponent getPopup(Component owner, Component content, int x, int y, JBPopup jbPopup) {
                final PopupFactory factory = PopupFactory.getSharedInstance();
                final Popup popup = factory.getPopup(owner, content, x, y);

                if (content instanceof JComponent jComponent) {
                    myNativeBorder = jComponent.getClientProperty("FlatLaf.internal.FlatPopupFactory.popupUsesNativeBorder") != null;
                }

                Window resolvedWindow = SwingUtilities.getWindowAncestor(content);
                return new AwtPopupWrapper(popup, jbPopup, resolvedWindow);
            }

            @Override
            public boolean isNativePopup() {
                return true;
            }

            @Override
            public boolean isNativeBorder() {
                return myNativeBorder;
            }
        }

        class AwtHeavyweight implements Factory {
            private boolean myNativeBorder;

            @Override
            public PopupComponent getPopup(Component owner, Component content, int x, int y, JBPopup jbPopup) {
                final PopupFactory factory = PopupFactory.getSharedInstance();

                final int oldType = PopupUtil.getPopupType(factory);
                PopupUtil.setPopupType(factory, 2);
                final Popup popup = factory.getPopup(owner, content, x, y);
                if (oldType >= 0) {
                    PopupUtil.setPopupType(factory, oldType);
                }

                if (content instanceof JComponent jComponent) {
                    myNativeBorder = jComponent.getClientProperty("FlatLaf.internal.FlatPopupFactory.popupUsesNativeBorder") != null;
                }

                Window resolvedWindow = SwingUtilities.getWindowAncestor(content);
                return new AwtPopupWrapper(popup, jbPopup, resolvedWindow);
            }

            @Override
            public boolean isNativeBorder() {
                return myNativeBorder;
            }

            @Override
            public boolean isNativePopup() {
                return true;
            }
        }

        class Dialog implements Factory {
            @Override
            public PopupComponent getPopup(Component owner, Component content, int x, int y, JBPopup jbPopup) {
                return new DialogPopupWrapper(owner, content, x, y, jbPopup);
            }

            @Override
            public boolean isNativePopup() {
                return false;
            }

            @Override
            public boolean isNativeBorder() {
                return false;
            }
        }
    }

    class DialogPopupWrapper implements PopupComponent {
        private final JDialog myDialog;
        private boolean myRequestFocus = true;

        @Override
        public void setRequestFocus(boolean requestFocus) {
            myRequestFocus = requestFocus;
        }

        @Override
        public boolean isPopupWindow(Window window) {
            return myDialog != null && myDialog == window;
        }

        public DialogPopupWrapper(Component owner, Component content, int x, int y, JBPopup jbPopup) {
            if (!owner.isShowing()) {
                throw new IllegalArgumentException("Popup owner must be showing");
            }

            final Window wnd = owner instanceof Window ? (Window) owner : SwingUtilities.getWindowAncestor(owner);
            if (wnd instanceof Frame) {
                myDialog = new JDialog((Frame) wnd);
            }
            else if (wnd instanceof Dialog) {
                myDialog = new JDialog((Dialog) wnd);
            }
            else {
                myDialog = new JDialog();
            }

            myDialog.getContentPane().setLayout(new BorderLayout());
            myDialog.getContentPane().add(content, BorderLayout.CENTER);
            myDialog.getRootPane().putClientProperty(JBPopup.KEY, jbPopup);
            myDialog.setUndecorated(true);
            myDialog.setBackground(UIUtil.getPanelBackground());
            myDialog.pack();
            myDialog.setLocation(x, y);
        }

        @Override
        public Window getWindow() {
            return myDialog;
        }

        @Override
        public void hide(boolean dispose) {
            myDialog.setVisible(false);
            if (dispose) {
                myDialog.dispose();
                myDialog.getRootPane().putClientProperty(JBPopup.KEY, null);
            }
        }

        @Override
        public void show() {
            if (!myRequestFocus) {
                myDialog.setFocusableWindowState(false);
            }
            myDialog.setVisible(true);
            
            SwingUtilities.invokeLater(() -> myDialog.setFocusableWindowState(true));
        }
    }

    class AwtPopupWrapper implements PopupComponent {

        private final Popup myPopup;
        private final JBPopup myJBPopup;
        private final Window myResolvedWindow;

        public AwtPopupWrapper(Popup popup, JBPopup jbPopup, Window resolvedWindow) {
            myPopup = popup;
            myJBPopup = jbPopup;
            myResolvedWindow = resolvedWindow;
        }

        @Override
        public boolean isPopupWindow(Window window) {
            final Window wnd = getWindow();
            return wnd != null && wnd == window;
        }

        @Override
        public void hide(boolean dispose) {
            myPopup.hide();

            Window window = getWindow();
            JRootPane rootPane = window instanceof RootPaneContainer ? ((RootPaneContainer) window).getRootPane() : null;
            DialogWrapper.cleanupRootPane(rootPane);
            DialogWrapper.cleanupWindowListeners(window);
        }

        @Override
        public void show() {
            myPopup.show();
            Window wnd = getWindow();
            if (wnd instanceof JWindow) {
                ((JWindow) wnd).getRootPane().putClientProperty(JBPopup.KEY, myJBPopup);
            }
        }

        @Override
        public Window getWindow() {
            if (myResolvedWindow != null) {
                return myResolvedWindow;
            }

            final Component c = PopupHacking.getComponent(myPopup);
            return c instanceof JWindow ? (JWindow) c : null;
        }

        @Override
        public void setRequestFocus(boolean requestFocus) {
        }
    }
}
