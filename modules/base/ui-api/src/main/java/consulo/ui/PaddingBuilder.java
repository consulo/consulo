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
package consulo.ui;

import consulo.ui.annotation.RequiredUIAccess;

/**
 * The room a component keeps between its own edges and what it draws, collected an edge at a time and written in
 * one go by {@link #apply()}.
 * <p/>
 * Room is named from the {@link Space} scale rather than counted, so a screen never carries a number and the
 * frontend stays free to decide what a step is worth.
 * <p/>
 * Each edge is either set, {@link #topReset() reset} or {@link #topReuse() reused}; reuse is what happens to an
 * edge which is not mentioned, and is spelled out so that leaving one alone can be written down. Nothing reaches
 * the component before {@link #apply()}, so clearing every edge and setting one is a single change rather than a
 * component briefly wearing neither.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public interface PaddingBuilder {
    PaddingBuilder topSet(Space space);

    PaddingBuilder topReset();

    PaddingBuilder topReuse();

    PaddingBuilder bottomSet(Space space);

    PaddingBuilder bottomReset();

    PaddingBuilder bottomReuse();

    PaddingBuilder leftSet(Space space);

    PaddingBuilder leftReset();

    PaddingBuilder leftReuse();

    PaddingBuilder rightSet(Space space);

    PaddingBuilder rightReset();

    PaddingBuilder rightReuse();

    PaddingBuilder verticalSet(Space space);

    PaddingBuilder horizontalSet(Space space);

    PaddingBuilder allSet(Space space);

    PaddingBuilder allReset();

    @RequiredUIAccess
    void apply();
}
