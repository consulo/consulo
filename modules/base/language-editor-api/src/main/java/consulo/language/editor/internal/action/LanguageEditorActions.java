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
package consulo.language.editor.internal.action;

/**
 * @author UNV
 * @since 2026-09-10
 */
public interface LanguageEditorActions {
    String EXPAND_LIVE_TEMPLATE_BY_TAB = "ExpandLiveTemplateByTab";

    String INSERT_LIVE_TEMPLATE = "InsertLiveTemplate";

    String NEXT_LIVE_TEMPLATE_VARIABLE = "NextTemplateVariable";

    String PREVIOUS_LIVE_TEMPLATE_VARIABLE = "PreviousTemplateVariable";

    String SAVE_AS_LIVE_TEMPLATE = "SaveAsTemplate";

    String SURROUND_WITH = "SurroundWith";

    String SURROUND_WITH_LIVE_TEMPLATE = "SurroundWithLiveTemplate";
}
