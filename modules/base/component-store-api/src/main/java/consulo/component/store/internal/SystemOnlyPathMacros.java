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
package consulo.component.store.internal;

import consulo.application.macro.PathMacros;
import consulo.component.macro.ExpandMacroToPathMap;
import consulo.component.macro.PathMacroUtil;
import consulo.component.macro.ReplacePathToMacroMap;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-04-26
 */
public class SystemOnlyPathMacros implements PathMacros {
    private final Map<String, String> mySystemMacros;

    public SystemOnlyPathMacros() {
        mySystemMacros = PathMacroUtil.getGlobalSystemMacros();
    }

    @Override
    public Set<String> getSystemMacroNames() {
        return mySystemMacros.keySet();
    }

    @Override
    public Set<String> getAllMacroNames() {
        return getSystemMacroNames();
    }

    @Override
    public @Nullable String getValue(String name) {
        return mySystemMacros.get(name);
    }

    @Override
    public void setMacro(String name, String value) {

    }

    @Override
    public void removeMacro(String name) {

    }

    @Override
    public Set<String> getUserMacroNames() {
        return Set.of();
    }

    @Override
    public Collection<String> getIgnoredMacroNames() {
        return Set.of();
    }

    @Override
    public void setIgnoredMacroNames(Collection<String> names) {

    }

    @Override
    public void addIgnoredMacro(String name) {
    }

    @Override
    public boolean isIgnoredMacroName(String macro) {
        return false;
    }

    @Override
    public void removeAllMacros() {
    }

    @Override
    public void addMacroReplacements(ReplacePathToMacroMap result) {

    }

    @Override
    public void addMacroExpands(ExpandMacroToPathMap result) {

    }
}
