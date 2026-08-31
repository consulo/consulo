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
package consulo.web.ui.impl.internal.clipboard;

import consulo.annotation.component.ServiceImpl;
import consulo.ui.UIAccess;
import consulo.ui.ex.CopyPasteManager;
import consulo.ui.ex.impl.internal.clipboard.BaseCopyPasteManagerImpl;
import consulo.util.dataholder.Key;
import jakarta.inject.Singleton;

/**
 * One browser session is one {@link UIAccess}, and it dies when the session does - so the state hangs
 * off the access itself rather than off a map this service would have to clean up.
 *
 * @author VISTALL
 * @since 2026-08-07
 */
@Singleton
@ServiceImpl
public class WebCopyPasteManagerImpl extends BaseCopyPasteManagerImpl implements CopyPasteManager {
    private static final Key<SessionState> STATE = Key.create("consulo.web.copyPasteState");

    @Override
    protected SessionState getSessionState(UIAccess uiAccess) {
        SessionState state = uiAccess.getUserData(STATE);
        if (state == null) {
            state = new SessionState();
            uiAccess.putUserData(STATE, state);
        }
        return state;
    }
}
