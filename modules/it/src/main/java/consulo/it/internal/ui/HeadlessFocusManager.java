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
package consulo.it.internal.ui;

import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.ui.FocusManager;
import consulo.ui.event.GlobalFocusListener;
import consulo.util.lang.ControlFlowException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 */
public class HeadlessFocusManager implements FocusManager {
    private static final Logger LOG = Logger.getInstance(HeadlessFocusManager.class);

    private final List<GlobalFocusListener> myListeners = new CopyOnWriteArrayList<>();

    @Override
    public Disposable addListener(GlobalFocusListener listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    public void fireFocusChanged() {
        for (GlobalFocusListener listener : myListeners) {
            try {
                listener.focusChanged();
            }
            catch (Throwable e) {
                if (e instanceof ControlFlowException) {
                    throw e;
                }
                LOG.error("Focus listener failed", e);
            }
        }
    }
}
