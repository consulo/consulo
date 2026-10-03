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
package consulo.execution.profiler.impl.internal.snapshot;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.component.ProcessCanceledException;
import consulo.execution.profiler.Failure;
import consulo.execution.profiler.ProfilerDumpFileParsingResult;
import consulo.execution.profiler.ProfilerDumpParserProvider;
import consulo.execution.profiler.Success;
import consulo.execution.profiler.impl.internal.view.ProfilerUIUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
public final class ProfilerDumpParserLookup {
    private static final Logger LOG = Logger.getInstance(ProfilerDumpParserLookup.class);

    private ProfilerDumpParserLookup() {
    }

    public static List<ProfilerDumpParserProvider> findByExtension(Application application, String fileName) {
        String lowerName = fileName.toLowerCase(Locale.ROOT);
        List<ProfilerDumpParserProvider> providers = new ArrayList<>();
        application.getExtensionPoint(ProfilerDumpParserProvider.class).forEach(provider -> {
            if (matchesExtension(provider.getRequiredFileExtension(), lowerName)) {
                providers.add(provider);
            }
        });
        return providers;
    }

    public static boolean hasExtensionProvider(Application application, String fileName) {
        return !findByExtension(application, fileName).isEmpty();
    }

    public static boolean hasExclusiveExtensionProvider(Application application, String fileName) {
        return hasExclusiveExtensionProvider(findByExtension(application, fileName));
    }

    public static boolean hasExclusiveExtensionProvider(List<ProfilerDumpParserProvider> providers) {
        for (ProfilerDumpParserProvider provider : providers) {
            if (provider.isExclusiveExtension()) {
                return true;
            }
        }
        return false;
    }

    public static List<ProfilerDumpParserProvider> findAnyFileProviders(Application application) {
        List<ProfilerDumpParserProvider> providers = new ArrayList<>();
        application.getExtensionPoint(ProfilerDumpParserProvider.class).forEach(provider -> {
            if (provider.getRequiredFileExtension() == null) {
                providers.add(provider);
            }
        });
        return providers;
    }

    public static ProfilerDumpFileParsingResult parse(
        Project project,
        File file,
        List<ProfilerDumpParserProvider> providers,
        ProgressIndicator indicator
    ) {
        if (providers.isEmpty()) {
            return new Failure("No installed profiler can open '" + file.getName() + "'");
        }

        List<String> messages = new ArrayList<>();
        for (ProfilerDumpParserProvider provider : providers) {
            indicator.checkCanceled();
            try {
                ProfilerDumpFileParsingResult result = provider.createParser(project).parse(file, indicator);
                switch (result) {
                    case Success success -> {
                        return success;
                    }
                    case Failure failure -> {
                        String message = failure.getMessage();
                        if (message != null && !message.isBlank()) {
                            messages.add(message);
                        }
                    }
                }
            }
            catch (ProcessCanceledException e) {
                throw e;
            }
            catch (Throwable e) {
                LOG.error("Profiler snapshot parser " + provider.getId() + " failed to read " + file, e);
                messages.add(ProfilerUIUtil.describe(e));
            }
        }
        return new Failure(messages.isEmpty() ? null : String.join("\n", messages));
    }

    public static LocalizeValue getMessage(Failure failure, String fileName) {
        String message = failure.getMessage();
        return message == null || message.isBlank()
            ? LocalizeValue.localizeTODO("The snapshot could not be read: " + fileName)
            : LocalizeValue.of(message);
    }

    private static boolean matchesExtension(@Nullable String extension, String lowerName) {
        return extension != null && !extension.isEmpty() && lowerName.endsWith("." + extension.toLowerCase(Locale.ROOT));
    }
}
