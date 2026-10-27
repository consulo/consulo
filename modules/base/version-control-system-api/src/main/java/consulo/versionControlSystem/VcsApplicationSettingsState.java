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
package consulo.versionControlSystem;

import com.dslplatform.json.CompiledJson;
import consulo.versionControlSystem.commit.CommitModeType;

/**
 * @author VISTALL
 * @since 2026-10-27
 */
@CompiledJson
public class VcsApplicationSettingsState {
    public String PATCH_STORAGE_LOCATION = null;
    public boolean SHOW_WHITESPACES_IN_LST = false;
    public boolean SHOW_LST_GUTTER_MARKERS = true;
    public boolean SHOW_LST_WORD_DIFFERENCES = true;
    public boolean DETECT_PATCH_ON_THE_FLY = false;
    public boolean CREATE_CHANGELISTS_AUTOMATICALLY = false;
    public CommitModeType COMMIT_MODE = CommitModeType.VERSION_CONTROL_TOOL_WINDOW;
    public boolean COMMIT_TOGGLE_MODE = false;
    /**
     * Option to show editor diff preview in non-modal commit interface with Commit toolwindow.
     */
    public boolean SHOW_EDITOR_PREVIEW_ON_DOUBLE_CLICK = true;
    /**
     * Option to show diff in commit interfaces without Commit toolwindow (modal, or commit panel inside Version Control toolwindow).
     */
    public boolean SHOW_DIFF_ON_DOUBLE_CLICK = true;
    public boolean MANAGE_IGNORE_FILES = false;
    public boolean DISABLE_MANAGE_IGNORE_FILES = false;
    public boolean GENERATE_IGNORE_FILES_IN_VCS_ROOTS = false;
}
