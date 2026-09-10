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

/**
 * A step of the spacing scale - the room a layout leaves between its children, and the room a component keeps
 * around what it draws. Both are the same thing, so both are named from here.
 * <p/>
 * A step is a name, never a measurement. There is no way to ask for one which is not on the scale, and no way to
 * read what a name is worth, so what {@link #LARGE} amounts to stays a decision of the frontend drawing it - the
 * desktop counts pixels and scales them, the browser hands the name to a class its theme can restyle. Retuning the
 * scale is then a change to whoever holds the numbers, not to the screens which asked for a step.
 * <p/>
 * The names climb one scale, so choosing between two of them is only ever how much room is wanted. A layout which
 * is not told otherwise leaves {@link #MEDIUM}.
 * <p/>
 * Room which has to line up with something that was measured - the height of a title bar the window manager
 * decided - is not a step of a scale, and belongs to whichever frontend did the measuring.
 *
 * @author VISTALL
 * @since 2026-09-03
 */
public final class Space {
    public static final Space NONE = new Space("NONE");
    public static final Space X_SMALL = new Space("X_SMALL");
    public static final Space SMALL = new Space("SMALL");
    public static final Space MEDIUM = new Space("MEDIUM");
    public static final Space LARGE = new Space("LARGE");
    public static final Space X_LARGE = new Space("X_LARGE");
    public static final Space XX_LARGE = new Space("XX_LARGE");
    public static final Space XXX_LARGE = new Space("XXX_LARGE");

    private final String myName;

    private Space(String name) {
        myName = name;
    }

    @Override
    public String toString() {
        return myName;
    }
}
