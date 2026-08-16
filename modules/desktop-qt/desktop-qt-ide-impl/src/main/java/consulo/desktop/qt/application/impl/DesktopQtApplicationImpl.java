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
package consulo.desktop.qt.application.impl;

import consulo.application.impl.internal.ReadMostlyRWLock;
import consulo.application.impl.internal.UnifiedApplication;
import consulo.application.internal.StartupProgress;
import consulo.component.internal.ComponentBinding;
import consulo.desktop.qt.ui.impl.DesktopQtUIAccess;
import consulo.ui.UIAccess;
import consulo.util.lang.ref.SimpleReference;

/**
 * @author VISTALL
 * @since 2026-08-16
 */
public class DesktopQtApplicationImpl extends UnifiedApplication {
    public DesktopQtApplicationImpl(ComponentBinding componentBinding, SimpleReference<? extends StartupProgress> splashRef) {
        super(componentBinding, splashRef);

        // the qt frontend owns a real ui thread, so a write action can be handed to it the way the awt one
        // does. the StampedRWLock a unified application takes cannot transfer, and the write thread would
        // then block on giveAndWait while the ui thread waits for the very lock it holds
        myLock = new ReadMostlyRWLock(null);
    }

    @Override
    public UIAccess getLastUIAccess() {
        return DesktopQtUIAccess.INSTANCE;
    }
}
