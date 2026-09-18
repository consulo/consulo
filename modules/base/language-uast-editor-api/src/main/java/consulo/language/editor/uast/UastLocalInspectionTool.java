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
package consulo.language.editor.uast;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.editor.inspection.InspectionTool;
import consulo.language.editor.inspection.InspectionToolState;
import consulo.language.editor.inspection.LocalInspectionToolSession;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.editor.rawHighlight.HighlightDisplayLevel;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.language.uast.visitor.AbstractUastNonRecursiveVisitor;
import consulo.language.uast.visitor.UastVisitor;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.Nullable;

/**
 * A language-independent local inspection written against the UAST tree.
 * <p>
 * This is a separate extension point and deliberately does NOT extend {@link InspectionTool}:
 * a single UAST inspection is fanned out by {@code consulo.language.editor.uast.internal.UastInspectionExtender} into one
 * {@code UastInspectionAdapter} per registered {@link UastLanguagePlugin}, so that the inspection
 * appears (and can be enabled, disabled and suppressed) once per language that has a UAST binding.
 *
 * @author VISTALL
 * @since 2026-09-18
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class UastLocalInspectionTool {
    /**
     * @return the display name of the inspection, shown in the inspection settings and problem descriptions
     */
    public abstract LocalizeValue getDisplayName();

    /**
     * @return the group name of the inspection, shown in the inspection settings
     */
    public abstract LocalizeValue getGroupDisplayName();

    /**
     * @return the default highlighting level of the problems reported by this inspection
     */
    public abstract HighlightDisplayLevel getDefaultLevel();

    /**
     * @return true if the inspection is enabled in the default inspection profile
     */
    public boolean isEnabledByDefault() {
        return true;
    }

    /**
     * Mirrors {@link InspectionTool#createStateProvider()}: the persistent, configurable settings of this inspection.
     * The state object is handed back to {@link #buildVisitor(ProblemsHolder, boolean, UastLanguagePlugin, Object)},
     * {@link #inspectionStarted} and {@link #inspectionFinished}.
     *
     * @return the state provider, or null for a stateless inspection (the platform default is used then)
     */
    public @Nullable InspectionToolState<?> createStateProvider() {
        return null;
    }

    /**
     * Short name used as the inspection identifier. Mirrors {@link InspectionTool#getShortName(String)}:
     * the simple class name with the {@code Inspection} suffix removed.
     * <p>
     * Every adapter created for this tool derives its own per-language id from this name
     * and reports this name as its alternative id, so a suppression comment with this name works across languages.
     *
     * @return short name of this inspection
     */
    public String getShortName() {
        return InspectionTool.getShortName(getClass().getSimpleName());
    }

    /**
     * The UAST element types this inspection is interested in.
     * Only PSI elements that convert to one of these types are fed to the visitor returned by
     * {@link #buildVisitor(ProblemsHolder, boolean, UastLanguagePlugin)}.
     *
     * @return the UAST element types the visitor handles
     */
    public abstract Class<? extends UElement>[] getUElementTypesHint();

    /**
     * Creates the UAST visitor which reports problems into the holder. Override this overload when the inspection has
     * settings, see {@link #createStateProvider()}; the default delegates to
     * {@link #buildVisitor(ProblemsHolder, boolean, UastLanguagePlugin)}.
     * <p>
     * The visitor is expected to be NON-recursive (every visit method should return {@code true}):
     * it is fed with every element of the file anyway, see {@link UastVisitorAdapter}.
     * The visitor must be thread-safe since it might be called on several elements concurrently.
     *
     * @param holder     where the visitor will register problems found
     * @param isOnTheFly true if the inspection was run in non-batch mode
     * @param plugin     the UAST plugin of the language the inspection is currently running for
     * @param state      state from {@link #createStateProvider()} {@code getState()}
     * @return not-null non-recursive visitor for this inspection
     */
    public UastVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, UastLanguagePlugin plugin, Object state) {
        return buildVisitor(holder, isOnTheFly, plugin);
    }

    /**
     * Creates the UAST visitor which reports problems into the holder (stateless variant).
     * The default visits nothing; override this or the state-aware overload.
     *
     * @param holder     where the visitor will register problems found
     * @param isOnTheFly true if the inspection was run in non-batch mode
     * @param plugin     the UAST plugin of the language the inspection is currently running for
     * @return not-null non-recursive visitor for this inspection
     */
    public UastVisitor buildVisitor(ProblemsHolder holder, boolean isOnTheFly, UastLanguagePlugin plugin) {
        return new AbstractUastNonRecursiveVisitor() {
        };
    }

    /**
     * Called once per file before the visitor runs, mirrors {@code LocalInspectionTool#inspectionStarted}.
     */
    public void inspectionStarted(LocalInspectionToolSession session, boolean isOnTheFly, Object state) {
    }

    /**
     * Called once per file after the visitor ran, mirrors {@code LocalInspectionTool#inspectionFinished}.
     */
    public void inspectionFinished(LocalInspectionToolSession session, ProblemsHolder problemsHolder, Object state) {
    }
}
