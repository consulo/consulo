/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.fileTypes;

import consulo.application.Application;
import consulo.application.util.SystemInfo;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.process.ProcessHandlerBuilderFactory;
import consulo.process.cmd.GeneralCommandLine;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.image.ImageEffects;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.INativeFileType;
import consulo.virtualFileSystem.fileType.localize.FileTypeLocalize;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 */
public class NativeFileType implements INativeFileType {
    private static final Logger LOG = Logger.getInstance(NativeFileType.class);

    public static final NativeFileType INSTANCE = new NativeFileType();

    private static final Image ICON = ImageEffects.layered(PlatformIconGroup.filetypesAny_type(), PlatformIconGroup.nodesSymlink());

    private NativeFileType() {
    }

    @Override
    @Nonnull
    public String getId() {
        return "Native";
    }

    @Override
    @Nonnull
    public LocalizeValue getDescription() {
        return FileTypeLocalize.nativeFileTypeDescription();
    }

    @Nonnull
    @Override
    public Image getIcon() {
        return ICON;
    }

    @Override
    public boolean isBinary() {
        return true;
    }

    @RequiredUIAccess
    @Override
    public void openFileInAssociatedApplication(ComponentManager project, @Nonnull VirtualFile file) {
        Application application = ((Project)project).getApplication();

        application.executeOnPooledThread(() -> openAssociatedApplication(application, file));
    }

    public static void openAssociatedApplication(@Nonnull Application application, @Nonnull VirtualFile file) {
        UIAccess.assetIsNotUIThread();

        GeneralCommandLine cmd = new GeneralCommandLine();
        if (Platform.current().os().isWindows()) {
            cmd.setExePath("rundll32.exe");
            cmd.addParameter("url.dll,FileProtocolHandler");
        }
        else if (Platform.current().os().isMac()) {
            cmd.setExePath("/usr/bin/open");
        }
        else if (SystemInfo.hasXdgOpen()) {
            cmd.setExePath("xdg-open");
        }
        else {
            return;
        }

        cmd.addParameter(file.getPath());

        try {
            application.getInstance(ProcessHandlerBuilderFactory.class).newBuilder(cmd).build();
        }
        catch (Exception e) {
            LOG.warn("Failed to open: " + file.getPath(), e);
        }
    }
}
