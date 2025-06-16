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
package consulo.execution.test.sm;

import consulo.application.Application;
import consulo.logging.Logger;
import consulo.ui.ModalityState;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.UIUtil;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * @author Roman Chernyatchik
 */
public class SMRunnerUtil {
    private static final Logger LOG = Logger.getInstance(SMRunnerUtil.class);

    private SMRunnerUtil() {
    }

    /**
     * Adds runnable to Event Dispatch Queue
     * if we aren't in UnitTest of Headless environment mode
     *
     * @param runnable Runnable
     */
    public static void addToInvokeLater(Runnable runnable) {
        Application application = Application.get();
        if (application.isHeadlessEnvironment() && !application.isUnitTestMode()) {
            runnable.run();
        }
        else {
            UIUtil.invokeLaterIfNeeded(runnable);
        }
    }

    public static void registerAsAction(
        KeyStroke keyStroke,
        String actionKey,
        final Runnable action,
        JComponent component
    ) {
        InputMap inputMap = component.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

        inputMap.put(keyStroke, actionKey);
        component.getActionMap().put(inputMap.get(keyStroke), new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    @RequiredUIAccess
    public static void runInEventDispatchThread(Runnable runnable, ModalityState state) {
        try {
            if (SwingUtilities.isEventDispatchThread()) {
                runnable.run();
            }
            else {
                Application.get().invokeAndWait(runnable::run, state);
            }
        }
        catch (Exception e) {
            LOG.warn(e);
        }
    }
}
