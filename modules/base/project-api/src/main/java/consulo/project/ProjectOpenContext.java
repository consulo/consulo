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
package consulo.project;

import consulo.util.dataholder.Key;
import consulo.util.dataholder.UnprotectedUserDataHolder;

/**
 * @author VISTALL
 * @since 2024-09-23
 */
public final class ProjectOpenContext extends UnprotectedUserDataHolder {
    public static final Key<Boolean> FORCE_OPEN_IN_NEW_FRAME = Key.create("ProjectOpenContext#FORCE_OPEN_IN_NEW_FRAME");
    public static final Key<Project> ACTIVE_PROJECT = Key.create("ProjectOpenContext#ACTIVE_PROJECT");
}
