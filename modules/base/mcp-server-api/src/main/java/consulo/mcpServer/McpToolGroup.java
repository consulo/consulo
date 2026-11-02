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
package consulo.mcpServer;

import consulo.localize.LocalizeValue;
import consulo.mcpServer.localize.McpServerLocalize;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-11-02
 */
public record McpToolGroup(String id, LocalizeValue name) {
    public static final McpToolGroup PROJECT_AND_FILES = new McpToolGroup("project.and.files", McpServerLocalize.groupProjectAndFiles());
    public static final McpToolGroup SEARCH = new McpToolGroup("search", McpServerLocalize.groupSearch());
    public static final McpToolGroup CODE_ANALYSIS = new McpToolGroup("code.analysis", McpServerLocalize.groupCodeAnalysis());
    public static final McpToolGroup EDITING = new McpToolGroup("editing", McpServerLocalize.groupEditing());
    public static final McpToolGroup BUILD_AND_RUN = new McpToolGroup("build.and.run", McpServerLocalize.groupBuildAndRun());
    public static final McpToolGroup VERSION_CONTROL = new McpToolGroup("version.control", McpServerLocalize.groupVersionControl());
    public static final McpToolGroup IDE = new McpToolGroup("ide", McpServerLocalize.groupIde());
    public static final McpToolGroup OTHER = new McpToolGroup("other", McpServerLocalize.groupOther());

    public static final List<McpToolGroup> STANDARD =
        List.of(PROJECT_AND_FILES, SEARCH, CODE_ANALYSIS, EDITING, BUILD_AND_RUN, VERSION_CONTROL, IDE, OTHER);
}
