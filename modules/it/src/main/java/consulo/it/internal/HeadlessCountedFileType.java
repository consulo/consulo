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
package consulo.it.internal;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.fileType.FileType;

/**
 * File type of the {@code .counted} files used by the indexing integration tests: the only type accepted by
 * {@link HeadlessCountingIndex}, so that every other file type is rejected by its file type hint.
 *
 * @author VISTALL
 */
public final class HeadlessCountedFileType implements FileType {
    public static final HeadlessCountedFileType INSTANCE = new HeadlessCountedFileType();

    public static final String EXTENSION = "counted";

    private HeadlessCountedFileType() {
    }

    @Override
    public String getId() {
        return "HEADLESS_COUNTED";
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.of("Headless counted files");
    }

    @Override
    public String getDefaultExtension() {
        return EXTENSION;
    }

    @Override
    public Image getIcon() {
        return Image.empty(16);
    }
}
