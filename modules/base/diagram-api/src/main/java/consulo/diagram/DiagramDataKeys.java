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
package consulo.diagram;

import consulo.util.dataholder.Key;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-01
 */
public final class DiagramDataKeys {
    public static final Key<DiagramProvider<?>> PROVIDER = Key.create("DIAGRAM_PROVIDER");
    public static final Key<DiagramDataModel<?>> DATA_MODEL = Key.create("DIAGRAM_DATA_MODEL");
    public static final Key<List<DiagramNode<?>>> SELECTED_NODES = Key.create("DIAGRAM_SELECTED_NODES");

    private DiagramDataKeys() {
    }
}
