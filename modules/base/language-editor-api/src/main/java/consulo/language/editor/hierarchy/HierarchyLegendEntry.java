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
package consulo.language.editor.hierarchy;

import consulo.localize.LocalizeValue;
import consulo.ui.image.Image;

/**
 * One row of the key shown beneath a hierarchy, pairing a marker with what it means. The platform draws
 * these, so a model states the meaning and never builds a component.
 *
 * @author VISTALL
 * @since 2026-09-19
 */
public record HierarchyLegendEntry(Image icon, LocalizeValue text) {
}
