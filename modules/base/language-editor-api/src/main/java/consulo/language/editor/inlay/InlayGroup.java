// InlayGroup.java
// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.inlay;

import consulo.language.editor.localize.LanguageEditorLocalize;
import consulo.localize.LocalizeValue;
import org.jspecify.annotations.NonNull;

public enum InlayGroup {
    CODE_VISION_GROUP_NEW(LanguageEditorLocalize.settingsHintsNewGroupCodeVision(), LanguageEditorLocalize.settingsHintsNewGroupCodeVisionDescription()),
    CODE_VISION_GROUP(LanguageEditorLocalize.settingsHintsGroupCodeVision(), LanguageEditorLocalize.settingsHintsNewGroupCodeVisionDescription()),
    PARAMETERS_GROUP(LanguageEditorLocalize.settingsHintsGroupParameters(), LanguageEditorLocalize.settingsHintsGroupParametersDescription()),
    TYPES_GROUP(LanguageEditorLocalize.settingsHintsGroupTypes(), LanguageEditorLocalize.settingsHintsGroupTypesDescription()),
    VALUES_GROUP(LanguageEditorLocalize.settingsHintsGroupValues(), LanguageEditorLocalize.settingsHintsGroupValuesDescription()),
    ANNOTATIONS_GROUP(LanguageEditorLocalize.settingsHintsGroupAnnotations(), LanguageEditorLocalize.settingsHintsGroupAnnotationsDescription()),
    METHOD_CHAINS_GROUP(LanguageEditorLocalize.settingsHintsGroupMethodChains()),
    LAMBDAS_GROUP(LanguageEditorLocalize.settingsHintsGroupLambdas(), LanguageEditorLocalize.settingsHintsGroupLambdasDescription()),
    CODE_AUTHOR_GROUP(LanguageEditorLocalize.settingsHintsGroupCodeAuthor(), LanguageEditorLocalize.settingsHintsGroupCodeAuthorDescription()),
    URL_PATH_GROUP(LanguageEditorLocalize.settingsHintsGroupUrlPath(), LanguageEditorLocalize.settingsHintsGroupUrlPathDescription()),
    OTHER_GROUP(LanguageEditorLocalize.settingsHintsGroupOther());

    private final LocalizeValue key;
    private final LocalizeValue description;

    InlayGroup(LocalizeValue key) {
        this(key, LocalizeValue.of());
    }

    InlayGroup(LocalizeValue key, LocalizeValue description) {
        this.key = key;
        this.description = description;
    }

    public @NonNull LocalizeValue title() {
        return key;
    }

    public @NonNull LocalizeValue description() {
        return description;
    }
}