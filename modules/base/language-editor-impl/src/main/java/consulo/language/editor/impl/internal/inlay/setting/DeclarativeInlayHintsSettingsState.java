// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.language.editor.impl.internal.inlay.setting;

import com.dslplatform.json.CompiledJson;

import java.util.HashMap;
import java.util.Map;

@CompiledJson
public class DeclarativeInlayHintsSettingsState {
    private Map<String, Boolean> enabledOptions = new HashMap<>();
    private Map<String, Boolean> providerIdToEnabled = new HashMap<>();


    public Map<String, Boolean> getEnabledOptions() {
        return enabledOptions;
    }

    public void setEnabledOptions(Map<String, Boolean> options) {
        this.enabledOptions = options;
    }

    public Map<String, Boolean> getProviderIdToEnabled() {
        return providerIdToEnabled;
    }

    public void setProviderIdToEnabled(Map<String, Boolean> mapping) {
        this.providerIdToEnabled = mapping;
    }
}
