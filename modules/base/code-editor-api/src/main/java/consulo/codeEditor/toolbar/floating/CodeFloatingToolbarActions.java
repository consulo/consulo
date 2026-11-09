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
package consulo.codeEditor.toolbar.floating;

/**
 * @author VISTALL
 * @since 2026-11-09
 */
public interface CodeFloatingToolbarActions {
    String GROUP = "Floating.CodeToolbar";
    /**
     * Each customized toolbar group must include this group.
     */
    String EXTENSIONS_GROUP = "Floating.CodeToolbar.Extensions";
    /**
     * Reformat, comment, common actions with code.
     */
    String ADDITIONAL_GROUP = "Floating.CodeToolbar.Additional";
    String DISABLE = "Floating.CodeToolbar.Disable";
    String GOTO_NEXT_MENU = "CodeFloatingToolbar.GotoNextMenu";
    String GOTO_PREVIOUS_MENU = "CodeFloatingToolbar.GotoPrevMenu";
}
