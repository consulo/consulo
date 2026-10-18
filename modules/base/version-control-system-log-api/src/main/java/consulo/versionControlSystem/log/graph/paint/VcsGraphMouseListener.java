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
package consulo.versionControlSystem.log.graph.paint;

/**
 * Mouse over the image of a {@link VcsGraphRow}, in pixels of the image: {@code x} from its left edge, {@code y} from
 * the top of the row, which is {@code rowHeight} high.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
public interface VcsGraphMouseListener {
    void mouseMoved(float x, float y, int rowHeight);

    void mouseClicked(float x, float y, int rowHeight);

    void mouseExited();
}
