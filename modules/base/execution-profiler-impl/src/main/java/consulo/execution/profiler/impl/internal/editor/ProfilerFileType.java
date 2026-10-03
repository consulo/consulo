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

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerFileType implements FileType {
    public static final ProfilerFileType SESSION = new ProfilerFileType(
        "PROFILER_SESSION",
        LocalizeValue.localizeTODO("Profiling session"),
        PlatformIconGroup.actionsProfile()
    );

    public static final ProfilerFileType CAPTURE = new ProfilerFileType(
        "PROFILER_CAPTURE",
        LocalizeValue.localizeTODO("Profiler capture"),
        PlatformIconGroup.actionsProfilecpu()
    );

    private final String myId;
    private final LocalizeValue myDisplayName;
    private final Image myIcon;

    private ProfilerFileType(String id, LocalizeValue displayName, Image icon) {
        myId = id;
        myDisplayName = displayName;
        myIcon = icon;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public LocalizeValue getDisplayName() {
        return myDisplayName;
    }

    @Override
    public LocalizeValue getDescription() {
        return myDisplayName;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }
}
