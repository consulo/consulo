/*
 * Copyright 2013-2024 consulo.io
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
package consulo.project.ui.wm;

import consulo.util.dataholder.Key;

/**
 * @author VISTALL
 * @since 2024-09-22
 */
public record IdeFrameState(int x, int y, int width, int height, boolean maximized, boolean fullScreen) {
    public static final Key<IdeFrameState> KEY = Key.create(IdeFrameState.class);

    public static final IdeFrameState EMPTY = new IdeFrameState(0, 0, 0, 0, false, false);
}
