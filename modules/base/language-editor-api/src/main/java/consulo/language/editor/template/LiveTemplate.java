/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.template;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.localize.LocalizeValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-09-09
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class LiveTemplate {
    public record Variable(String name, String expression, String defaultValue, boolean alwaysStopAt) {
    }

    private final LiveTemplateGroupId myGroupId;
    private final String myId;
    private final String myAbbreviation;
    private final String myValue;
    private final LocalizeValue myDescription;

    private boolean myReformat;

    private Map<String, Variable> myVariables = Map.of();

    public LiveTemplate(LiveTemplateGroupId groupId, String id, String abbreviation, String value, LocalizeValue description) {
        myGroupId = groupId;
        myId = id;
        myAbbreviation = abbreviation;
        myValue = value;
        myDescription = description;
    }

    protected final void withReformat() {
        myReformat = true;
    }

    protected final void withVariable(String name, String expression, String defaultValue, boolean alwaysStopAt) {
        Variable variable = new Variable(name, expression, defaultValue, alwaysStopAt);
        if (myVariables.isEmpty()) {
            myVariables = new LinkedHashMap<>();
        }
        myVariables.put(name, variable);
    }

    public final String getId() {
        return myId;
    }

    public final String getAbbreviation() {
        return myAbbreviation;
    }

    public final String getValue() {
        return myValue;
    }

    public final LocalizeValue getDescription() {
        return myDescription;
    }

    public final LiveTemplateGroupId getGroupId() {
        return myGroupId;
    }

    public final boolean isReformat() {
        return myReformat;
    }
}
