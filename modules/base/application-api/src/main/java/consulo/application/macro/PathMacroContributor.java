// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.macro;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;

import java.util.Map;

/**
 * <p>
 * Application-wide Path Macro contributor.
 * </p>
 * </p>
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface PathMacroContributor {
    /**
     * Register path.macros
     * <p>
     * Note: Value will be overridden if key is specified in <code>path.macros.xml</code>.
     */
    void registerPathMacros(Map<String, String> macros, Map<String, String> legacyMacros);

    /**
     * Register path.macros even if key is specified in <code>path.macros.xml</code>.
     */
    default void forceRegisterPathMacros(Map<String, String> macros) {
    }
}
