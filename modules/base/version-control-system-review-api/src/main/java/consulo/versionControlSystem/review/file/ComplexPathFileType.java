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
package consulo.versionControlSystem.review.file;

import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;
import consulo.virtualFileSystem.fileType.localize.FileTypeLocalize;

/**
 * The type of a file without content: binary and read-only, so no document or text PSI is ever built for it.
 *
 * @author VISTALL
 * @since 2026-10-26
 */
public final class ComplexPathFileType implements FileType {
    public static final ComplexPathFileType INSTANCE = new ComplexPathFileType();

    private ComplexPathFileType() {
    }

    @Override
    public String getId() {
        return "COMPLEX_PATH";
    }

    @Override
    public LocalizeValue getDescription() {
        return FileTypeLocalize.unknownFileTypeDescription();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.filetypesUnknown();
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
