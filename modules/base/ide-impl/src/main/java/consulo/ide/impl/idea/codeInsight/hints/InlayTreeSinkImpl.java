// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.component.util.PluginExceptionUtil;
import consulo.language.editor.inlay.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class InlayTreeSinkImpl implements InlayTreeSink {
    private final String providerId;
    private final Map<String, Boolean> enabledOptions;
    private final boolean isInPreview;
    private final boolean providerIsDisabled;
    private final Class<?> providerClass;
    private final String sourceId;

    private final List<InlayData> inlayDataToPresentation = new ArrayList<>();
    private final Map<String, Boolean> activeOptions = new HashMap<>();

    public InlayTreeSinkImpl(String providerId,
                             Map<String, Boolean> enabledOptions,
                             boolean isInPreview,
                             boolean providerIsDisabled,
                             Class<?> providerClass,
                             String sourceId) {
        this.providerId = providerId;
        this.enabledOptions = enabledOptions;
        this.isInPreview = isInPreview;
        this.providerIsDisabled = providerIsDisabled;
        this.providerClass = providerClass;
        this.sourceId = sourceId;
    }

    @Override
    public void addPresentation(InlayPosition position,
                                List<InlayPayload> payloads,
                                String tooltip,
                                HintFormat hintFormat,
                                Consumer<PresentationTreeBuilder> builder) {
        PresentationTreeBuilderImpl b = PresentationTreeBuilderImpl.createRoot(position);
        builder.accept(b);
        TinyTree<Object> tree = b.complete();

        if (tree.size() == 0) {
            throw PluginExceptionUtil.createByClass(
                "Provider didn't provide any presentation. It is forbidden - do not try to create it in this case.",
                new RuntimeException(providerClass.getCanonicalName() + " id: " + providerId),
                providerClass
            );
        }

        boolean disabled = providerIsDisabled
            || (!activeOptions.isEmpty() && activeOptions.values().stream().anyMatch(v -> !v));

        inlayDataToPresentation.add(new InlayData(
            position,
            tooltip,
            hintFormat,
            tree,
            providerId,
            disabled,
            payloads,
            providerClass,
            sourceId
        ));
    }

    @Override
    public void whenOptionEnabled(String optionId, Runnable block) {
        Boolean isEnabled = enabledOptions.get(optionId);
        if (isEnabled == null) {
            throw new IllegalStateException("Option " + optionId + " is not found for provider " + providerId);
        }
        if (!isInPreview && !isEnabled) {
            return;
        }
        boolean wasNotEnabledEarlier = activeOptions.put(optionId, isEnabled) != null;
        try {
            block.run();
        }
        finally {
            if (!wasNotEnabledEarlier) {
                activeOptions.remove(optionId);
            }
        }
    }

    /**
     * Returns the collected inlay data.
     */
    public List<InlayData> finish() {
        return inlayDataToPresentation;
    }
}
