// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.dumb.PossiblyDumbAware;
import consulo.codeEditor.Editor;
import consulo.language.Language;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Set;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface DeclarativeInlayHintsProvider extends PossiblyDumbAware {
    Key<LocalizeValue> PROVIDER_NAME = Key.create("declarative.hints.provider.name");
    Key<String> PROVIDER_ID = Key.create("declarative.hints.provider.id");
    Key<Map<String, InlayActionPayload>> INLAY_PAYLOADS = Key.create("declarative.hints.inlay.payload");

    /**
     * Creates collector for given file and editor if it may create inlays, or null otherwise.
     */
    @Nullable
    DeclarativeInlayHintsCollector createCollector(PsiFile file, Editor editor);

    @Nonnull
    Language getLanguage();

    @Nonnull
    String getId();

    @Nonnull
    LocalizeValue getName();

    @Nonnull
    LocalizeValue getDescription();

    @Nonnull
    LocalizeValue getPreviewFileText();

    @Nonnull
    InlayGroup getGroup();

    @Nonnull
    default Set<DeclarativeInlayOptionInfo> getOptions() {
        return Set.of();
    }

    default boolean isEnabledByDefault() {
        return true;
    }
}
