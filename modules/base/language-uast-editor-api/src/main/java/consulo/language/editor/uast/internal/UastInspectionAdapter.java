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
package consulo.language.editor.uast.internal;

import consulo.language.Language;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalInspectionTool;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.editor.uast.UastLocalInspectionTool;
import consulo.language.editor.uast.UastVisitorAdapter;
import consulo.language.psi.PsiElementVisitor;
import consulo.language.uast.UastLanguagePlugin;
import consulo.localize.LocalizeValue;

/**
 * Adapts one {@link UastLocalInspectionTool} to one language: a regular {@link LocalInspectionTool}
 * bound to the language of the given {@link UastLanguagePlugin}.
 * Instances are created by {@link UastInspectionExtender}, one per (tool, plugin) pair.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
public final class UastInspectionAdapter extends LocalInspectionTool {
    private final UastLocalInspectionTool myTool;
    private final UastLanguagePlugin myPlugin;

    public UastInspectionAdapter(UastLocalInspectionTool tool, UastLanguagePlugin plugin) {
        myTool = tool;
        myPlugin = plugin;
    }

    public UastLocalInspectionTool getTool() {
        return myTool;
    }

    public UastLanguagePlugin getPlugin() {
        return myPlugin;
    }

    @Override
    public Language getLanguage() {
        return myPlugin.getLanguage();
    }

    /**
     * The short name is unique per language, since the same tool is registered once per {@link UastLanguagePlugin}.
     */
    @Override
    public String getShortName() {
        return myTool.getShortName() + "." + sanitize(myPlugin.getLanguage().getID());
    }

    @Override
    public String getID() {
        return getShortName();
    }

    /**
     * The original tool name, so that suppression with the original name works across languages.
     */
    @Override
    public String getAlternativeID() {
        return myTool.getShortName();
    }

    @Override
    public LocalizeValue getDisplayName() {
        return myTool.getDisplayName();
    }

    @Override
    public LocalizeValue getGroupDisplayName() {
        return myTool.getGroupDisplayName();
    }

    @Override
    public HighlightDisplayLevel getDefaultLevel() {
        return myTool.getDefaultLevel();
    }

    @Override
    public boolean isEnabledByDefault() {
        return myTool.isEnabledByDefault();
    }

    /**
     * The state of the wrapped tool, or the platform default (stateless) when the tool declares none.
     */
    @Override
    public InspectionToolState<?> createStateProvider() {
        InspectionToolState<?> state = myTool.createStateProvider();
        return state != null ? state : super.createStateProvider();
    }

    @Override
    public void inspectionStarted(LocalInspectionToolSession session, boolean isOnTheFly, Object state) {
        myTool.inspectionStarted(session, isOnTheFly, state);
    }

    @Override
    public void inspectionFinished(LocalInspectionToolSession session, ProblemsHolder problemsHolder, Object state) {
        myTool.inspectionFinished(session, problemsHolder, state);
    }

    @Override
    public PsiElementVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, LocalInspectionToolSession session, Object state) {
        return new UastVisitorAdapter(myPlugin, myTool.buildVisitor(holder, isOnTheFly, myPlugin, state), true, myTool.getUElementTypesHint());
    }

    /**
     * Replaces every character which is not allowed by {@link LocalInspectionTool#VALID_ID_PATTERN} with an underscore.
     * A language id is not necessarily a valid inspection id: the C# language id is literally {@code C#}
     * and fails the {@link LocalInspectionTool#isValidID(String)} validation.
     */
    private static String sanitize(String languageId) {
        StringBuilder builder = new StringBuilder(languageId.length());
        for (int i = 0; i < languageId.length(); i++) {
            char c = languageId.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '.' || c == '-') {
                builder.append(c);
            }
            else {
                builder.append('_');
            }
        }
        return builder.toString();
    }
}
