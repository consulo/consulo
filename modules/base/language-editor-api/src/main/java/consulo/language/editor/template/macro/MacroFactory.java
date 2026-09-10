/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.template.macro;

import consulo.application.Application;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.MultiMap;

import java.util.Collection;
import java.util.List;

public class MacroFactory {
    private static final MultiMap<String, Macro> ourMacroTable = init();

    public static Macro createMacro(String name) {
        return ContainerUtil.getFirstItem(ourMacroTable.get(name));
    }

    public static List<Macro> getMacros(String name) {
        return (List<Macro>) ourMacroTable.get(name);
    }

    public static Macro[] getMacros() {
        Collection<? extends Macro> values = ourMacroTable.values();
        return values.toArray(new Macro[values.size()]);
    }

    private static MultiMap<String, Macro> init() {
        MultiMap<String, Macro> result = MultiMap.create();
        Application.get().getExtensionPoint(Macro.class)
            .forEach(macro -> result.putValue(macro.getName(), macro));
        return result;
    }
}
