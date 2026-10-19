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
package consulo.codeEditor.internal.bridge;

import consulo.ui.color.ColorValue;

/**
 * Lines of the editor left of a gap joined to lines of the editor right of it, drawn as a band across the gap.
 * A resolved connector is drawn as its outline only.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public record EditorBridgeConnector(int startLine1, int endLine1, int startLine2, int endLine2, ColorValue color, boolean resolved) {
}
