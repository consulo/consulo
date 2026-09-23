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
package consulo.ide.impl.idea.ide;

import consulo.annotation.component.ComponentProfiles;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.progress.ProgressManager;
import consulo.document.FileDocumentManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * Analog of {@code consulo.desktop.awt.application.DesktopSaveAndSyncHandlerImpl} for the frontends which
 * render {@link consulo.ui} components rather than swing. Saving after the frame goes quiet is what the
 * toolkit bound one adds, and it needs a queue of input events to tell quiet from busy - everything else a
 * handler does is shared.
 *
 * @author VISTALL
 * @since 2026-09-23
 */
@Singleton
@ServiceImpl(profiles = ComponentProfiles.UNIFIED)
public class UnifiedSaveAndSyncHandlerImpl extends BaseSaveAndSyncHandler {
    @Inject
    public UnifiedSaveAndSyncHandlerImpl(
        Application application,
        GeneralSettings generalSettings,
        ProgressManager progressManager,
        FileDocumentManager fileDocumentManager
    ) {
        super(application, generalSettings, progressManager, fileDocumentManager);
    }
}
