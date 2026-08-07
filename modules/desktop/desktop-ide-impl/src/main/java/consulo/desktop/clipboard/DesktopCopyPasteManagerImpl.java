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
package consulo.desktop.clipboard;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.UIAccess;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.impl.internal.clipboard.BaseCopyPasteManagerImpl;
import jakarta.inject.Singleton;

/**
 * AWT and SWT hold a single {@link UIAccess} for the whole run, so there is nothing to key on.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
@Singleton
@ServiceImpl
public class DesktopCopyPasteManagerImpl extends BaseCopyPasteManagerImpl implements CopyPasteManager {
    private final SessionState myState = new SessionState();

    @Override
    protected SessionState getSessionState(UIAccess uiAccess) {
        return myState;
    }
}
