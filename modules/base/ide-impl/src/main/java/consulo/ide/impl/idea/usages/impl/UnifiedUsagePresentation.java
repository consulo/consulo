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
package consulo.ide.impl.idea.usages.impl;

import consulo.ui.TextAttribute;
import consulo.ui.image.Image;
import consulo.util.lang.Pair;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * What the unified usage view draws for a usage, a file or a target, computed off the UI thread.
 *
 * @author VISTALL
 * @since 2026-11-10
 */
record UnifiedUsagePresentation(@Nullable Image icon, List<Pair<String, TextAttribute>> text) {
}
