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
package consulo.it.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormattingContext;
import consulo.language.codeStyle.service.AsyncDocumentFormattingService;
import consulo.language.codeStyle.service.AsyncFormattingRequest;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.ui.notification.NotificationGroup;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Async document formatting service used by the formatting integration tests. It is inert until a test enables it; while enabled it
 * formats the files whose name starts with {@link #FILE_PREFIX} by upper-casing the text of the formatting ranges, and records the
 * requests it gets.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessAsyncFormattingService extends AsyncDocumentFormattingService {
    public static final String FILE_PREFIX = "headless-format";

    public static final NotificationGroup GROUP = NotificationGroup.balloonGroup("Headless Formatting");

    public record Request(List<TextRange> ranges, boolean quickFormat, boolean whitespaceOnly, @Nullable Path ioFile, boolean onUIThread) {
    }

    public enum Mode {
        /**
         * Answers with the upper-cased text.
         */
        RESPOND,
        /**
         * Never answers: the request times out.
         */
        NO_RESPONSE,
        /**
         * Answers with an error.
         */
        ERROR
    }

    private static volatile boolean ourEnabled;
    private static volatile Mode ourMode = Mode.RESPOND;
    private static volatile Duration ourTimeout = DEFAULT_TIMEOUT;
    private static final List<Request> ourRequests = new CopyOnWriteArrayList<>();
    private static final AtomicInteger ourCancelCount = new AtomicInteger();

    public static void setEnabled(boolean enabled) {
        ourEnabled = enabled;
        ourMode = Mode.RESPOND;
        ourTimeout = DEFAULT_TIMEOUT;
        ourRequests.clear();
        ourCancelCount.set(0);
    }

    public static void setMode(Mode mode, Duration timeout) {
        ourMode = mode;
        ourTimeout = timeout;
    }

    public static List<Request> getRequests() {
        return List.copyOf(ourRequests);
    }

    public static int getCancelCount() {
        return ourCancelCount.get();
    }

    @Override
    public Set<Feature> getFeatures() {
        return Set.of(Feature.FORMAT_FRAGMENTS);
    }

    @Override
    @RequiredReadAction
    public boolean canFormat(PsiFile file) {
        return ourEnabled && file.getName().startsWith(FILE_PREFIX);
    }

    @Override
    protected @Nullable FormattingTask createFormattingTask(AsyncFormattingRequest formattingRequest) {
        return new FormattingTask() {
            @Override
            public void run() {
                ourRequests.add(new Request(
                    formattingRequest.getFormattingRanges(),
                    formattingRequest.isQuickFormat(),
                    formattingRequest.canChangeWhitespaceOnly(),
                    formattingRequest.getIOFile(),
                    Application.get().isDispatchThread()
                ));
                switch (ourMode) {
                    case NO_RESPONSE -> {
                        return;
                    }
                    case ERROR -> {
                        formattingRequest.onError(LocalizeValue.of("Headless Formatter"), LocalizeValue.of("cannot format"));
                        return;
                    }
                    default -> {
                    }
                }
                StringBuilder text = new StringBuilder(formattingRequest.getDocumentText());
                for (TextRange range : formattingRequest.getFormattingRanges()) {
                    String upperCased = range.substring(text.toString()).toUpperCase(Locale.ROOT);
                    text.replace(range.getStartOffset(), range.getEndOffset(), upperCased);
                }
                formattingRequest.onTextReady(text.toString());
            }

            @Override
            public boolean cancel() {
                ourCancelCount.incrementAndGet();
                return true;
            }
        };
    }

    @Override
    protected NotificationGroup getNotificationGroup() {
        return GROUP;
    }

    @Override
    protected LocalizeValue getName() {
        return LocalizeValue.of("Headless Formatter");
    }

    @Override
    protected Duration getTimeout() {
        return ourTimeout;
    }

    @Override
    protected void prepareForFormatting(Document document, FormattingContext formattingContext) {
    }
}
