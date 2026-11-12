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
package consulo.application.impl.internal.platform;

import consulo.platform.Platform;
import consulo.platform.PlatformFileSystem;
import consulo.platform.PlatformJvm;
import consulo.platform.PlatformOperatingSystem;
import consulo.platform.PlatformNetwork;
import consulo.platform.PlatformUser;
import consulo.project.localize.ProjectLocalize;
import consulo.ui.UIAccess;
import consulo.util.dataholder.UserDataHolderBase;

import java.io.File;
import java.net.URL;

/**
 * Stands for a platform target that is not reachable right now (not connected, or no longer configured).
 * <p>
 * Every operation fails - code holding it never silently falls back to the local machine.
 *
 * @author VISTALL
 * @since 2026-11-13
 */
public final class UnknownPlatform extends UserDataHolderBase implements Platform {
    private final String myTargetId;
    private final String myName;

    public UnknownPlatform(String targetId, String name) {
        myTargetId = targetId;
        myName = name;
    }

    @Override
    public String getId() {
        return myTargetId;
    }

    @Override
    public String getName() {
        return myName;
    }

    @Override
    public PlatformFileSystem fs() {
        throw unavailable();
    }

    @Override
    public PlatformOperatingSystem os() {
        throw unavailable();
    }

    @Override
    public PlatformJvm jvm() {
        throw unavailable();
    }

    @Override
    public PlatformNetwork net() {
        throw unavailable();
    }

    @Override
    public PlatformUser user() {
        throw unavailable();
    }

    @Override
    public void openInBrowser(URL url) {
        throw unavailable();
    }

    @Override
    public void openFileInFileManager(File file, UIAccess uiAccess) {
        throw unavailable();
    }

    @Override
    public void openDirectoryInFileManager(File file, UIAccess uiAccess) {
        throw unavailable();
    }

    private IllegalStateException unavailable() {
        return new IllegalStateException(ProjectLocalize.errorPlatformTargetUnavailable(myName).get());
    }

    @Override
    public String toString() {
        return "UnknownPlatform{" + myTargetId + "}";
    }
}
