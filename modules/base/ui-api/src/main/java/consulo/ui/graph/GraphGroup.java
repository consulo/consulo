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
package consulo.ui.graph;

import consulo.localize.LocalizeValue;

/**
 * A named set of nodes of a {@link Graph}, drawn as a frame around them. Two nodes are in one group when their
 * {@link GraphModel#getGroup} values are equal.
 *
 * @author VISTALL
 * @since 2026-10-01
 */
public interface GraphGroup {
    LocalizeValue getName();
}
