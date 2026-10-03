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
package consulo.execution.profiler.impl.internal.editor;

import consulo.disposer.Disposer;
import consulo.execution.profiler.impl.internal.session.ProfilerSession;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionLayout;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionManager;
import consulo.execution.profiler.impl.internal.session.ProfilerSessionPanel;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerSessionFileEditor extends ProfilerFileEditorBase {
    private final ProfilerSessionVirtualFile myFile;
    private final ProfilerSessionManager mySessionManager;

    public ProfilerSessionFileEditor(ProfilerSessionVirtualFile file, ProfilerSessionManager sessionManager) {
        super(file, LocalizeValue.localizeTODO("Profiler"));
        myFile = file;
        mySessionManager = sessionManager;
    }

    @RequiredUIAccess
    @Override
    protected Component createComponent() {
        ProfilerSession session = myFile.getSession();
        if (session.isDisposed()) {
            return ProfilerUIUtil.hint(LocalizeValue.localizeTODO("The profiling session is closed"));
        }

        ProfilerSessionPanel panel = mySessionManager.createPanel(session, ProfilerSessionLayout.FULL);
        Disposer.register(this, panel);
        return panel.getComponent();
    }
}
