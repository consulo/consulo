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
package consulo.execution.impl.internal.action.runPopup;

import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 2025-10-21
 */
public class ActionFilterUtil {
    public static final Key<String> SEARCH_TAG = Key.create("SEARCH_TAG");

    public static final String TAG_PINNED = "pinned";
    public static final String TAG_RECENT = "recent";
    public static final String TAG_REGULAR_HIDE = "regular-hide"; // hidden behind the "All configurations" toggle
    public static final String TAG_REGULAR_SHOW = "regular-show"; // shown regularly
    public static final String TAG_REGULAR_DUPE = "regular-dupe"; // shown regularly until search (pinned/recent duplicate)
    public static final String TAG_HIDDEN = "hidden";   // hidden until search
}
