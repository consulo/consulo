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

/**
 * @author VISTALL
 * @since 2026-09-30
 */
@FunctionalInterface
public interface GraphEdgeRender<E> {
    static <V> GraphEdgeRender<V> defaultRender() {
        return (presentation, source, target) -> {
        };
    }

    /**
     * Called for every arrow of the model, from {@code source} to {@code target}. A presentation left untouched is a
     * solid line with a {@link GraphArrow#FILLED} head at the target.
     */
    void render(GraphEdgePresentation presentation, E source, E target);
}
