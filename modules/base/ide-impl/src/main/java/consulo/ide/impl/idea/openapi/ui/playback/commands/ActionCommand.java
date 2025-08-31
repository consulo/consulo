/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.ui.playback.commands;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.application.util.registry.Registry;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.openapi.ui.playback.PlaybackContext;
import consulo.ide.impl.idea.openapi.util.TimedOutCallback;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.event.AnActionListener;
import consulo.ui.ex.awt.internal.ActionAWTUtil;
import consulo.ui.ex.keymap.KeymapManager;
import consulo.util.concurrent.ActionCallback;
import consulo.util.lang.ref.Ref;

import javax.swing.*;
import java.awt.event.InputEvent;

public class ActionCommand extends TypeCommand {

    public static String PREFIX = CMD_PREFIX + "action";

    public ActionCommand(String text, int line) {
        super(text, line);
    }

    protected ActionCallback _execute(final PlaybackContext context) {
        final String actionName = getText().substring(PREFIX.length()).trim();

        final ActionManager am = ActionManager.getInstance();
        final AnAction targetAction = am.getAction(actionName);
        if (targetAction == null) {
            dumpError(context, "Unknown action: " + actionName);
            return new ActionCallback.Rejected();
        }


        if (!context.isUseDirectActionCall()) {
            Shortcut[] sc = KeymapManager.getInstance().getActiveKeymap().getShortcuts(actionName);
            KeyStroke stroke = null;
            for (Shortcut each : sc) {
                if (each instanceof KeyboardShortcut) {
                    KeyboardShortcut ks = (KeyboardShortcut) each;
                    KeyStroke first = ks.getFirstKeyStroke();
                    KeyStroke second = ks.getSecondKeyStroke();
                    if (first != null && second == null) {
                        stroke = KeyStroke.getKeyStroke(first.getKeyCode(), first.getModifiers(), false);
                        break;
                    }
                }
            }

            if (stroke != null) {
                final ActionCallback
                    result = new TimedOutCallback(Registry.intValue("actionSystem.commandProcessingTimeout"), "Timed out calling action id=" + actionName, new Throwable(), true) {
                    @Override
                    protected void dumpError() {
                        context.error(getMessage(), getLine());
                    }
                };
                context.message("Invoking action via shortcut: " + stroke.toString(), getLine());

                final KeyStroke finalStroke = stroke;

                IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(new Runnable() {
                    @Override
                    public void run() {
                        final Ref<AnActionListener> listener = new Ref<>();
                        listener.set(new AnActionListener() {
                            @Override
                            public void beforeActionPerformed(final AnAction action, DataContext dataContext, AnActionEvent event) {
                                SwingUtilities.invokeLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (context.isDisposed()) {
                                            am.removeAnActionListener(listener.get());
                                            return;
                                        }

                                        if (targetAction.equals(action)) {
                                            context.message("Performed action: " + actionName, context.getCurrentLine());
                                            am.removeAnActionListener(listener.get());
                                            result.setDone();
                                        }
                                    }
                                });
                            }
                        });
                        am.addAnActionListener(listener.get());

                        context.runPooledThread(new Runnable() {
                            @Override
                            public void run() {
                                type(context.getRobot(), finalStroke);
                            }
                        });
                    }
                });

                return result;
            }
        }

        final InputEvent input = getInputEvent(actionName);

        final ActionCallback result = new ActionCallback();

        context.getRobot().delay(Registry.intValue("actionSystem.playback.delay"));
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                am.tryToExecute(targetAction, input, null, null, false).doWhenProcessed(result.createSetDoneRunnable());
            }
        });

        return result;
    }

    public static InputEvent getInputEvent(String actionName) {
        return ActionAWTUtil.getInputEvent(actionName);
    }
}