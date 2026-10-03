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
package consulo.ide.impl.idea.find.actions;

import consulo.document.RangeMarker;
import consulo.navigation.Navigatable;
import consulo.ui.TextAttribute;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
record UnifiedShowUsagesItem(
    UnifiedShowUsagesItemKind kind,
    @Nullable VirtualFile file,
    @Nullable Image fileIcon,
    String fileName,
    String path,
    int offset,
    int line,
    int column,
    List<Pair<String, TextAttribute>> text,
    String plainText,
    @Nullable Navigatable navigatable,
    @Nullable RangeMarker marker
) {
    static UnifiedShowUsagesItem sentinel(UnifiedShowUsagesItemKind kind, String text) {
        return new UnifiedShowUsagesItem(kind, null, null, "", "", -1, 0, 0, List.of(), text, null, null);
    }

    boolean isSentinel() {
        return kind != UnifiedShowUsagesItemKind.USAGE;
    }

    int navigationOffset() {
        RangeMarker marker = this.marker;
        return marker != null && marker.isValid() ? marker.getStartOffset() : offset;
    }

    void dispose() {
        RangeMarker marker = this.marker;
        if (marker != null) {
            marker.dispose();
        }
    }
}
