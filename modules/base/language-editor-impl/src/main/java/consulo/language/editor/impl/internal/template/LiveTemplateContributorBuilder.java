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
package consulo.language.editor.impl.internal.template;

import consulo.language.editor.internal.TemplateConstants;
import consulo.language.editor.template.LiveTemplateContributor;
import consulo.language.editor.template.context.TemplateContextType;
import consulo.localize.LocalizeValue;
import consulo.util.dataholder.KeyWithDefaultValue;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 2024-09-15
 */
public abstract class LiveTemplateContributorBuilder implements LiveTemplateContributor.Builder {
    public record Variable(String name, String expression, String defaultValue, boolean alwaysStopAt) {

    }

    private List<Variable> myVariables = new ArrayList<>();

    private final String myGroupId;
    private final LocalizeValue myGroupName;
    private final String myId;
    private final String myAbbreviation;
    private final String myValue;
    private final LocalizeValue myDescription;

    protected boolean myWantReformat;

    protected char myShortcut = TemplateConstants.DEFAULT_CHAR;

    protected Map<Class<? extends TemplateContextType>, Boolean> myStrictContextTypes = new LinkedHashMap<>();

    protected Map<Class<? extends TemplateContextType>, Boolean> myContextTypes = new LinkedHashMap<>();

    protected Map<String, Boolean> myOptions = new LinkedHashMap<>();

    public LiveTemplateContributorBuilder(String groupId,
                                          LocalizeValue groupName,
                                          String id,
                                          String abbreviation,
                                          String value,
                                          LocalizeValue description) {
        myGroupId = groupId;
        myGroupName = groupName;
        myId = id;
        myAbbreviation = abbreviation;
        myValue = value;
        myDescription = description;
    }

    public List<Variable> getVariables() {
        return myVariables;
    }

    public String getGroupId() {
        return myGroupId;
    }

    public LocalizeValue getGroupName() {
        return myGroupName;
    }

    public String getId() {
        return myId;
    }

    public String getAbbreviation() {
        return myAbbreviation;
    }

    public String getValue() {
        return myValue;
    }

    public LocalizeValue getDescription() {
        return myDescription;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withContext(Class<? extends TemplateContextType> context, boolean enabled) {
        myStrictContextTypes.put(context, enabled);
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withContextsOf(Class<? extends TemplateContextType> context, boolean enabled) {
        myContextTypes.put(context, enabled);
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withVariable(String name, String expression, String defaultValue, boolean alwaysStopAt) {
        myVariables.add(new Variable(name, expression, defaultValue, alwaysStopAt));
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withReformat() {
        myWantReformat = true;
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withTabShortcut() {
        myShortcut = TemplateConstants.TAB_CHAR;
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withEnterShortcut() {
        myShortcut = TemplateConstants.ENTER_CHAR;
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withSpaceShortcut() {
        myShortcut = TemplateConstants.SPACE_CHAR;
        return this;
    }

    @Nonnull
    @Override
    public LiveTemplateContributor.Builder withOption(@Nonnull KeyWithDefaultValue<Boolean> key, boolean value) {
        myOptions.put(key.toString(), value);
        return this;
    }
}
