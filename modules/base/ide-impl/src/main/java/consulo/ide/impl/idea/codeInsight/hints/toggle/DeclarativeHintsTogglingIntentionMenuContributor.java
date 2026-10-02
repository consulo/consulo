// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.toggle;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPassFactory;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.editor.inlay.DeclarativeInlayHintsCollector;
import consulo.language.editor.inlay.DeclarativeInlayHintsProviderFactoryUtil;
import consulo.language.editor.inlay.DeclarativeInlayHintsSettings;
import consulo.language.editor.inlay.DeclarativeInlayOptionInfo;
import consulo.language.editor.inlay.DeclarativeInlayPayload;
import consulo.language.editor.inlay.DeclarativeInlayPosition;
import consulo.language.editor.inlay.DeclarativeInlayTreeSink;
import consulo.language.editor.inlay.DeclarativePresentationTreeBuilder;
import consulo.language.editor.inlay.HintFormat;
import consulo.language.editor.inlay.InlayProviderInfo;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionMenuContributor;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

@ExtensionImpl
public class DeclarativeHintsTogglingIntentionMenuContributor implements IntentionMenuContributor, DumbAware {
    @Override
    @RequiredReadAction
    public void collectActions(
        Editor hostEditor,
        PsiFile hostFile,
        IntentionsInfo intentions,
        int passIdToShowIntentionsFor,
        int offset
    ) {
        Context context = Context.gather(hostFile.getProject(), hostEditor, hostFile);
        if (context == null) {
            return;
        }
        for (ProviderInfo providerInfo : context.getProvidersToToggle()) {
            DeclarativeHintsTogglingIntention action = new DeclarativeHintsTogglingIntention(
                providerInfo.providerId(),
                providerInfo.providerName(),
                providerInfo.providerEnabled()
            );
            IntentionActionDescriptor descriptor =
                new IntentionActionDescriptor(action, List.of(), LocalizeValue.empty(), null, null, null, HighlightSeverity.INFORMATION);
            intentions.intentionsToShow.add(descriptor);
        }
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        for (ProviderWithOptionInfo providersWithOption : context.getProvidersWithOptions()) {
            String providerId = providersWithOption.providerId();
            InlayProviderInfo providerInfo = DeclarativeInlayHintsProviderFactoryUtil.getProviderInfo(hostFile.getLanguage(), providerId);
            if (providerInfo == null) {
                continue;
            }
            String optionId = providersWithOption.optionId();
            DeclarativeInlayOptionInfo optionInfo = null;
            for (DeclarativeInlayOptionInfo option : providerInfo.options()) {
                if (option.id().equals(optionId)) {
                    optionInfo = option;
                    break;
                }
            }
            if (optionInfo == null) {
                continue;
            }
            Boolean optionEnabledInSettings = settings.isOptionEnabled(optionId, providerId);
            boolean optionEnabled = optionEnabledInSettings != null ? optionEnabledInSettings : optionInfo.isEnabledByDefault();
            boolean providerEnabled = providersWithOption.providerEnabled();
            DeclarativeHintsTogglingOptionIntention.Mode mode;
            if (providerEnabled) {
                if (optionEnabled) {
                    mode = DeclarativeHintsTogglingOptionIntention.Mode.DISABLE_OPTION;
                }
                else {
                    mode = DeclarativeHintsTogglingOptionIntention.Mode.ENABLE_OPTION;
                }
            }
            else {
                mode = DeclarativeHintsTogglingOptionIntention.Mode.ENABLE_PROVIDER_AND_OPTION;
            }
            DeclarativeHintsTogglingOptionIntention action = new DeclarativeHintsTogglingOptionIntention(
                optionId,
                providerId,
                providersWithOption.providerName(),
                optionInfo.name(),
                mode
            );
            IntentionActionDescriptor descriptor =
                new IntentionActionDescriptor(action, List.of(), LocalizeValue.empty(), null, null, null, HighlightSeverity.INFORMATION);
            intentions.intentionsToShow.add(descriptor);
        }
    }

    private static class DummyInlayTreeSink implements DeclarativeInlayTreeSink {
        private final Set<String> myAttemptedToAddUnderOptions = new HashSet<>();
        private boolean myAttemptedToAddWithoutOptions;

        private final Set<String> myCurrentOptions = new HashSet<>();

        @Override
        public void addPresentation(
            DeclarativeInlayPosition position,
            @Nullable List<DeclarativeInlayPayload> payloads,
            @Nullable String tooltip,
            HintFormat hintFormat,
            Consumer<DeclarativePresentationTreeBuilder> builder
        ) {
            if (myCurrentOptions.isEmpty()) {
                myAttemptedToAddWithoutOptions = true;
            }
            else {
                myAttemptedToAddUnderOptions.addAll(myCurrentOptions);
            }
        }

        @Override
        public void whenOptionEnabled(String optionId, Runnable block) {
            myCurrentOptions.add(optionId);
            try {
                block.run();
            }
            finally {
                myCurrentOptions.remove(optionId);
            }
        }

        public Set<String> getAttemptedToAddUnderOptions() {
            return myAttemptedToAddUnderOptions;
        }

        public boolean isAttemptedToAddWithoutOptions() {
            return myAttemptedToAddWithoutOptions;
        }
    }

    private record ProviderWithOptionInfo(String providerId, String optionId, LocalizeValue providerName, boolean providerEnabled) {
    }

    private record ProviderInfo(String providerId, LocalizeValue providerName, boolean providerEnabled) {
    }

    private record SharedCollectorInfo(
        DeclarativeInlayHintsCollector.SharedBypassCollector collector,
        DummyInlayTreeSink sink,
        String providerId,
        LocalizeValue providerName,
        boolean enabled
    ) {
    }

    private record OwnCollectorInfo(
        DeclarativeInlayHintsCollector.OwnBypassCollector collector,
        String providerId,
        LocalizeValue providerName,
        boolean enabled
    ) {
    }

    private static final class Context {
        private final Set<ProviderWithOptionInfo> myProvidersWithOptions;
        private final Set<ProviderInfo> myProvidersToToggle;

        private Context(Set<ProviderWithOptionInfo> providersWithOptions, Set<ProviderInfo> providersToToggle) {
            myProvidersWithOptions = providersWithOptions;
            myProvidersToToggle = providersToToggle;
        }

        public Set<ProviderWithOptionInfo> getProvidersWithOptions() {
            return myProvidersWithOptions;
        }

        public Set<ProviderInfo> getProvidersToToggle() {
            return myProvidersToToggle;
        }

        @RequiredReadAction
        public static @Nullable Context gather(Project project, Editor editor, PsiFile file) {
            List<InlayProviderInfo> providers = DeclarativeInlayHintsPassFactory.getSuitableToFileProviders(file);
            if (providers.isEmpty()) {
                return null;
            }
            List<OwnCollectorInfo> ownBypassCollectors = new ArrayList<>();
            List<SharedCollectorInfo> sharedBypassCollectors = new ArrayList<>();
            DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
            for (InlayProviderInfo provider : providers) {
                DeclarativeInlayHintsCollector collector = provider.provider().createCollector(file, editor);
                if (collector == null) {
                    continue;
                }
                String providerId = provider.providerId();
                boolean enabled = settings.isProviderEnabled(providerId, provider.isEnabledByDefault());
                switch (collector) {
                    case DeclarativeInlayHintsCollector.OwnBypassCollector ownBypassCollector ->
                        ownBypassCollectors.add(new OwnCollectorInfo(ownBypassCollector, providerId, provider.providerName(), enabled));
                    case DeclarativeInlayHintsCollector.SharedBypassCollector sharedBypassCollector -> sharedBypassCollectors.add(
                        new SharedCollectorInfo(
                            sharedBypassCollector,
                            new DummyInlayTreeSink(),
                            providerId,
                            provider.providerName(),
                            enabled
                        )
                    );
                }
            }
            Set<ProviderInfo> providersToToggle = new HashSet<>();
            Set<ProviderWithOptionInfo> providersWithOptionsToToggle = new HashSet<>();
            if (!sharedBypassCollectors.isEmpty()) {
                int offset = editor.getCaretModel().getOffset();
                PsiElement element = file.findElementAt(offset);
                if (element != null) {
                    PsiElement parent = element;
                    while (parent != null) {
                        for (SharedCollectorInfo info : sharedBypassCollectors) {
                            DummyInlayTreeSink sink = info.sink();
                            info.collector().collectFromElementForActions(parent, sink);
                            if (sink.isAttemptedToAddWithoutOptions()) {
                                providersToToggle.add(new ProviderInfo(info.providerId(), info.providerName(), info.enabled()));
                            }
                            if (!sink.getAttemptedToAddUnderOptions().isEmpty()) {
                                for (String optionId : sink.getAttemptedToAddUnderOptions()) {
                                    providersWithOptionsToToggle.add(
                                        new ProviderWithOptionInfo(info.providerId(), optionId, info.providerName(), info.enabled())
                                    );
                                }
                            }
                        }
                        parent = parent instanceof PsiFile ? null : parent.getParent();
                    }
                }
            }
            if (!ownBypassCollectors.isEmpty()) {
                for (OwnCollectorInfo info : ownBypassCollectors) {
                    if (info.collector().shouldSuggestToggling(project, editor, file)) {
                        providersToToggle.add(new ProviderInfo(info.providerId(), info.providerName(), info.enabled()));
                    }
                    Set<String> optionsToToggle = info.collector().getOptionsToToggle(project, editor, file);
                    for (String optionId : optionsToToggle) {
                        providersWithOptionsToToggle.add(
                            new ProviderWithOptionInfo(info.providerId(), optionId, info.providerName(), info.enabled())
                        );
                    }
                }
            }
            if (providersToToggle.isEmpty() && providersWithOptionsToToggle.isEmpty()) {
                return null;
            }

            return new Context(providersWithOptionsToToggle, providersToToggle);
        }
    }
}
