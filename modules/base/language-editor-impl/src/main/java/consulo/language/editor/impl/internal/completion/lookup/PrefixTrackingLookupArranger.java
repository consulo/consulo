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
package consulo.language.editor.impl.internal.completion.lookup;

import org.jspecify.annotations.Nullable;

/**
 * An arranger which remembers the prefix it last arranged against.
 * <p/>
 * A lookup being reused carries items which were matched against an older prefix, and whether they still stand is
 * decided by comparing that prefix with the one the lookup now has. Only the completion arranger tracks this, and it
 * lives with the completion engine - the lookup asks through here rather than reaching up to it.
 *
 * @author VISTALL
 */
public interface PrefixTrackingLookupArranger {
    /**
     * {@code null} before anything has been arranged.
     */
    @Nullable
    String getLastLookupPrefix();
}
