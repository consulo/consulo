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
import consulo.ui.color.ColorValue;

/**
 * The lines a component draws along its own edges, collected an edge at a time and written in one go by
 * {@link #apply()}.
 * <p/>
 * A line is a hairline in whichever colour is asked for, and how it is drawn is the frontend's to decide - a
 * component which asks for all four edges is drawn with whatever corner the current style rounds them to. How
 * thick a line is was never a decision a screen made, so there is nothing here to say it with.
 * <p/>
 * Each edge is either set, {@link #topReset() reset} or {@link #topReuse() reused}; reuse is what happens to an
 * edge which is not mentioned, and is spelled out so that leaving one alone can be written down. Nothing reaches
 * the component before {@link #apply()}, so clearing an edge and setting another is one change rather than two.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public interface BorderBuilder {
    BorderBuilder topSet();

    BorderBuilder topSet(ColorValue color);

    BorderBuilder topReset();

    BorderBuilder topReuse();

    BorderBuilder bottomSet();

    BorderBuilder bottomSet(ColorValue color);

    BorderBuilder bottomReset();

    BorderBuilder bottomReuse();

    BorderBuilder leftSet();

    BorderBuilder leftSet(ColorValue color);

    BorderBuilder leftReset();

    BorderBuilder leftReuse();

    BorderBuilder rightSet();

    BorderBuilder rightSet(ColorValue color);

    BorderBuilder rightReset();

    BorderBuilder rightReuse();

    BorderBuilder allSet();

    BorderBuilder allSet(ColorValue color);

    BorderBuilder allReset();

    @RequiredUIAccess
    void apply();
}
