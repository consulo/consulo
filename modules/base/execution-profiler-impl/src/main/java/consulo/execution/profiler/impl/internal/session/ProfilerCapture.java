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
package consulo.execution.profiler.impl.internal.session;

import consulo.execution.profiler.ProfilerData;
import consulo.execution.profiler.impl.internal.editor.ProfilerCaptureVirtualFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerCapture {
    private final @Nullable ProfilerSession mySession;
    private final LocalizeValue myTitle;
    private final Image myIcon;
    private final ProfilerData myData;
    private final boolean myClosable;

    private @Nullable ProfilerCaptureVirtualFile myVirtualFile;
    private boolean myShown;

    public ProfilerCapture(@Nullable ProfilerSession session, LocalizeValue title, Image icon, ProfilerData data, boolean closable) {
        mySession = session;
        myTitle = title;
        myIcon = icon;
        myData = data;
        myClosable = closable;
    }

    public @Nullable ProfilerSession getSession() {
        return mySession;
    }

    public LocalizeValue getTitle() {
        return myTitle;
    }

    public Image getIcon() {
        return myIcon;
    }

    public ProfilerData getData() {
        return myData;
    }

    public boolean isClosable() {
        return myClosable;
    }

    public String getFileName() {
        ProfilerSession session = mySession;
        String title = myTitle.get();
        return session == null ? title : session.getTitle() + " - " + title;
    }

    @RequiredUIAccess
    public ProfilerCaptureVirtualFile getVirtualFile() {
        ProfilerCaptureVirtualFile file = myVirtualFile;
        if (file == null) {
            file = new ProfilerCaptureVirtualFile(this);
            myVirtualFile = file;
        }
        return file;
    }

    @RequiredUIAccess
    public @Nullable ProfilerCaptureVirtualFile findVirtualFile() {
        return myVirtualFile;
    }

    @RequiredUIAccess
    public boolean isShown() {
        return myShown;
    }

    @RequiredUIAccess
    public void markShown() {
        myShown = true;
    }

    void invalidate() {
        ProfilerCaptureVirtualFile file = myVirtualFile;
        if (file != null) {
            file.setValid(false);
        }
    }

    @Override
    public String toString() {
        return getFileName();
    }
}
