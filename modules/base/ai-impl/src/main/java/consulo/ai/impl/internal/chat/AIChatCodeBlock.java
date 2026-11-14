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
package consulo.ai.impl.internal.chat;

import consulo.ai.AICodeBlockHighlighter;
import consulo.application.concurrent.coroutine.ReadLock;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.UIAction;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.clipboard.DataTransfer;
import consulo.util.concurrent.coroutine.Coroutine;
import consulo.util.concurrent.coroutine.CoroutineScope;
import org.jspecify.annotations.Nullable;

import java.net.URL;
import java.util.Map;
import java.util.Objects;

/**
 * A fenced code block of an answer, colored as the editor colors its language, with a button copying the code. The
 * coloring runs off the ui thread; while the answer streams only the last code given is shown.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIChatCodeBlock {
    private static final String COPY_TARGET = "code";
    private static final String CSS = "body { margin: 0; padding: 0; } pre { margin: 0; padding: 8px; }";

    private final Project myProject;
    private final HtmlView myView = HtmlView.create();
    private @Nullable String myLanguage;
    private @Nullable String myCode;
    private long myGeneration;

    @RequiredUIAccess
    AIChatCodeBlock(Project project) {
        myProject = project;
        myView.setSizeToContent(true);
    }

    Component getComponent() {
        return myView;
    }

    @RequiredUIAccess
    void update(@Nullable String language, String code) {
        if (code.equals(myCode) && Objects.equals(language, myLanguage)) {
            return;
        }
        myLanguage = language;
        myCode = code;
        long generation = ++myGeneration;
        CoroutineScope.launchAsync(
            myProject.coroutineContext(),
            () -> Coroutine
                .first(ReadLock.<Object, String>apply(ignored -> AICodeBlockHighlighter.getInstance().highlight(myProject, language, code)))
                .then(UIAction.<String, String>apply(html -> {
                    if (generation == myGeneration) {
                        myView.render(new HtmlView.RenderData(
                            "<div id=\"" + COPY_TARGET + "\">" + html + "</div>",
                            CSS,
                            new URL[0],
                            Map.of(COPY_TARGET, DataTransfer.of(code))
                        ));
                    }
                    return html;
                }))
        );
    }
}
