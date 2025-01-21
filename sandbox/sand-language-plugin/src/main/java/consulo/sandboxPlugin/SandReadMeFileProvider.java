/*
 * Copyright 2013-2025 consulo.io
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
package consulo.sandboxPlugin;

import consulo.annotation.component.ExtensionImpl;
import consulo.fileEditor.ReadMeFileProvider;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.nio.file.Path;

/**
 * @author VISTALL
 * @since 2025-01-21
 */
@ExtensionImpl
public class SandReadMeFileProvider implements ReadMeFileProvider {
    @Nullable
    @Override
    public Path resolveFile(@Nonnull Path projectPath) {
        return projectPath.resolve("SAND.md");
    }
}
