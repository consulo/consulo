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
package consulo.project;

import consulo.project.internal.ProjectEx;

/**
 * @author VISTALL
 * @since 2026-03-08
 */
public enum ProjectType {
    REGULAR(ProjectEx.REGULAR_PROJECT),
    DEFAULT(ProjectEx.DEFAULT_PROJECT),
    WELCOME(ProjectEx.WELCOME_PROJECT);

    private final int myProfileBit;

    ProjectType(int profileBit) {
        myProfileBit = profileBit;
    }

    public int getProfileBit() {
        return myProfileBit;
    }
}
