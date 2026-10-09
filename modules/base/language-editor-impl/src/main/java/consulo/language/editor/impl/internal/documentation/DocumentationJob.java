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
package consulo.language.editor.impl.internal.documentation;

import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
final class DocumentationJob {
    @Nullable DocumentationCollector myCollector;
    @Nullable String myDocumentation;
    @Nullable String myMessage;
    @Nullable String myBrowseUrl;
    @Nullable DocumentationPage myPage;

    static DocumentationJob message(String message) {
        DocumentationJob job = new DocumentationJob();
        job.myMessage = message;
        return job;
    }

    static DocumentationJob nothing() {
        return new DocumentationJob();
    }

    static DocumentationJob browse(String url) {
        DocumentationJob job = new DocumentationJob();
        job.myBrowseUrl = url;
        return job;
    }

    static DocumentationJob collect(DocumentationCollector collector, @Nullable String documentation) {
        DocumentationJob job = new DocumentationJob();
        job.myCollector = collector;
        job.myDocumentation = documentation;
        return job;
    }
}
