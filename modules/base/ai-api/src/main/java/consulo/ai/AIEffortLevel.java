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
package consulo.ai;

import consulo.localize.LocalizeValue;

/**
 * How hard a model thinks before it answers - the levels are the vendor's own, for example {@code low} or {@code max}.
 *
 * @param id          what the provider passes on to the vendor
 * @param displayName what the user picks
 * @author VISTALL
 * @since 2026-11-11
 */
public record AIEffortLevel(String id, LocalizeValue displayName) {
}
