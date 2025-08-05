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
package consulo.ide.impl.idea.ui.debugger.extensions;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.AllIcons;
import consulo.dataContext.DataContext;
import consulo.disposer.Disposable;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.application.Application;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.ui.ex.awt.JBScrollPane;
import consulo.ide.impl.idea.ui.debugger.UiDebuggerExtension;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;

/**
 * @author kirillk
 * @since 2011-08-04
 */
@ExtensionImpl
public class ActionTracer implements UiDebuggerExtension, AnActionListener {
    private final Logger LOG = Logger.getInstance(ActionTracer.class);

    private JTextArea myText;
    private JPanel myComponent;

    private Disposable myListenerDisposable;

    @Override
    public JComponent getComponent() {
        if (myComponent == null) {
            myText = new JTextArea();
            final JBScrollPane log = new JBScrollPane(myText);
            final AnAction clear = new AnAction("Clear", "Clear log", AllIcons.General.Reset) {
                @Override
                public void actionPerformed(AnActionEvent e) {
                    myText.setText(null);
                }
            };
            myComponent = new JPanel(new BorderLayout());
            final DefaultActionGroup group = new DefaultActionGroup();
            group.add(clear);
            myComponent.add(
                ActionManager.getInstance().createActionToolbar(ActionPlaces.UNKNOWN, group, true).getComponent(),
                BorderLayout.NORTH
            );
            myComponent.add(log);

            myListenerDisposable = Disposable.newDisposable();
            Application.get().getMessageBus().connect(myListenerDisposable).subscribe(AnActionListener.class, this);
        }

        return myComponent;
    }

    @Override
    public String getName() {
        return "Actions";
    }

    @Override
    public void afterActionPerformed(AnAction action, DataContext dataContext, AnActionEvent event) {
        StringBuffer out = new StringBuffer();
        final ActionManager actionManager = ActionManager.getInstance();
        final String id = actionManager.getId(action);
        out.append("id=" + id);
        if (id != null) {
            out.append(" shortcuts:");
            final Shortcut[] shortcuts = KeymapManager.getInstance().getActiveKeymap().getShortcuts(id);
            for (int i = 0; i < shortcuts.length; i++) {
                Shortcut shortcut = shortcuts[i];
                out.append(shortcut);
                if (i < shortcuts.length - 1) {
                    out.append(",");
                }
            }
        }
        out.append("\n");
        final Document doc = myText.getDocument();
        try {
            doc.insertString(doc.getLength(), out.toString(), null);
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    final int y = (int) myText.getBounds().getMaxY();
                    myText.scrollRectToVisible(new Rectangle(0, y, myText.getBounds().width, 0));
                }
            });
        }
        catch (BadLocationException e) {
            LOG.error(e);
        }
    }

    @Override
    public void disposeUiResources() {
        Disposer.dispose(myListenerDisposable);
        myComponent = null;
        myText = null;
    }
}
