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
package consulo.ui.event.details;

import consulo.ui.Point2D;

/**
 * The details of an event no user input drove - a {@code setValue} call, a selection made by code. It says so
 * outright where a null used to leave "programmatic" and "the frontend did not fill the details in" apart only
 * by guesswork. It carries no position of its own.
 *
 * @author VISTALL
 * @since 2026-08-19
 */
public final class ProgrammaticInputDetails extends InputDetails {
    public static final ProgrammaticInputDetails INSTANCE = new ProgrammaticInputDetails();

    private ProgrammaticInputDetails() {
        super(new Point2D(0, 0), new Point2D(0, 0));
    }
}
