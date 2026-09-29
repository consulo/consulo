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
package consulo.ui.ex.font;

import org.jspecify.annotations.Nullable;

import java.net.URL;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public record BundledFont(URL url, String family, int weight, boolean italic, @Nullable String legacyFamily, boolean monospaced) {
    public String fileName() {
        String path = url.getPath();
        return path.substring(path.lastIndexOf('/') + 1);
    }
}
