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
package consulo.sandboxPlugin.ide.debug;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.execution.debug.breakpoint.XBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpointType;
import consulo.execution.debug.breakpoint.XLineBreakpointTypeResolver;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

@ExtensionImpl
public class SandLineBreakpointTypeResolver implements XLineBreakpointTypeResolver {
    @Override
    @RequiredReadAction
    public @Nullable XLineBreakpointType<?> resolveBreakpointType(Project project, VirtualFile virtualFile, int line) {
        return project.getApplication().getExtensionPoint(XBreakpointType.class).findExtension(SandLineBreakpointType.class);
    }

    @Override
    public FileType getFileType() {
        return SandFileType.INSTANCE;
    }
}
