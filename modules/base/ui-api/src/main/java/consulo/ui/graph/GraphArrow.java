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
 * Head drawn where an arrow meets a node. Which frontend shape stands for each head is up to the frontend.
 *
 * @author VISTALL
 * @since 2026-09-30
 */
public enum GraphArrow {
    NONE,
    /**
     * Filled triangle - the default head of an arrow.
     */
    FILLED,
    /**
     * Two open strokes.
     */
    OPEN,
    /**
     * Hollow triangle.
     */
    TRIANGLE,
    DIAMOND,
    CIRCLE
}
