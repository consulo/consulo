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

import consulo.execution.profiler.impl.internal.session.ProfilerCapture;
import consulo.execution.profiler.impl.internal.view.ProfilerCaptureViewFactory;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.DockLayout;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerCaptureFileEditor extends ProfilerFileEditorBase {
    private final ProfilerCaptureVirtualFile myFile;
    private final ProfilerCaptureViewFactory myCaptureViewFactory;

    public ProfilerCaptureFileEditor(ProfilerCaptureVirtualFile file, ProfilerCaptureViewFactory captureViewFactory) {
        super(file, LocalizeValue.localizeTODO("Profiler Capture"));
        myFile = file;
        myCaptureViewFactory = captureViewFactory;
    }

    @RequiredUIAccess
    @Override
    protected Component createComponent() {
        ProfilerCapture capture = myFile.getCapture();
        capture.markShown();

        DockLayout root = DockLayout.create(Space.NONE);
        root.center(myCaptureViewFactory.createView(capture.getData(), this));
        return root;
    }
}
