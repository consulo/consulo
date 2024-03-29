/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.application.dumb;

/**
 * A marker interface for the things that are allowed to run in dumb mode (when indices are in background update).
 * Implementors must take care of handling and/or not calling non-DumbAware parts of system
 * <p>
 * Known implementors are:
 * <li> {@link consulo.ide.impl.idea.openapi.actionSystem.AnAction}s
 * <li> {@link consulo.ide.impl.idea.openapi.fileEditor.FileEditorProvider}s
 * <li> post-startup activities ({@link consulo.ide.impl.idea.openapi.startup.StartupManager#registerPostStartupActivity(Runnable)})
 * <li> Stacktrace {@link consulo.ide.impl.idea.execution.filters.Filter}s
 * <li> {@link consulo.ide.impl.idea.ide.SelectInTarget}s
 * <li> {@link consulo.ide.impl.idea.codeInsight.completion.CompletionContributor}s
 * <li> {@link consulo.ide.impl.idea.lang.annotation.Annotator}s
 * <li> {@link consulo.ide.impl.idea.codeInsight.daemon.LineMarkerProvider}s
 * <li> {@link consulo.ide.impl.idea.codeHighlighting.TextEditorHighlightingPass}es
 * <li> {@link consulo.ide.impl.idea.codeInspection.LocalInspectionTool}s
 * <li> {@link consulo.ide.impl.idea.openapi.wm.ToolWindowFactory}s
 * <li> {@link consulo.ide.impl.idea.lang.injection.MultiHostInjector}s
 *
 * @author peter
 * @see consulo.ide.impl.idea.openapi.project.DumbService
 * @see consulo.ide.impl.idea.openapi.project.DumbAwareRunnable
 * @see PossiblyDumbAware
 */
public interface DumbAware {
}
