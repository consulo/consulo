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
package consulo.execution.profiler.configuration;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.application.Application;
import consulo.configurable.UnnamedConfigurable;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.ui.image.Image;
import consulo.util.xml.serializer.XmlSerializationException;
import consulo.util.xml.serializer.XmlSerializer;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

/**
 * A kind of profiler, such as JFR sampling or NYTProf. The user creates any number of named configurations of it, each a
 * state of type {@code S}.
 * <p>
 * Types with the same {@link #getLanguageSettingsGroup()} are edited together on one settings page.
 *
 * @author VISTALL
 * @since 2026-10-03
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class ProfilerConfigurationTypeBase<S extends ProfilerConfigurationState> {
    private static final Logger LOG = Logger.getInstance(ProfilerConfigurationTypeBase.class);

    private volatile @Nullable Class<?> myStateClass;

    /**
     * @return the type with the id, or null when no installed plugin provides it
     */
    public static @Nullable ProfilerConfigurationTypeBase<?> findById(String id) {
        return Application.get().getExtensionPoint(ProfilerConfigurationTypeBase.class).findFirstSafe(type -> id.equals(type.getId()));
    }

    /**
     * @return the id stored in {@link ProfilerConfigurationState#getConfigurationTypeId()} of this type's states
     */
    public abstract String getId();

    public abstract LocalizeValue getDisplayName();

    public @Nullable String getHelpTopic() {
        return null;
    }

    public abstract Image getIcon();

    /**
     * @return the id of the settings page which edits this type's configurations
     */
    public abstract String getLanguageSettingsGroup();

    /**
     * @return a new state with the defaults of a configuration the user adds
     */
    public abstract S getTemplateState();

    /**
     * Creates the editor of one configuration. Its component is built with unified UI through
     * {@link UnnamedConfigurable#createUIComponent(consulo.disposer.Disposable)}, and {@code apply()} writes into the state.
     */
    public abstract UnnamedConfigurable createConfigurable(S state);

    /**
     * @return what launches processes under the configuration, or null when it can only attach
     */
    public abstract @Nullable ProfilerStarter createStarter(S state);

    /**
     * @return what attaches the configuration to running processes, or null when it can only launch
     */
    public abstract @Nullable ProfilerAttacher createAttacher(S state);

    /**
     * Copies a state, so it can be edited without touching the stored one. The default round-trips it through the XML
     * serializer.
     */
    @SuppressWarnings("unchecked")
    public S copyState(S state) {
        try {
            Element element = XmlSerializer.serialize(state);
            if (element != null) {
                Object copy = XmlSerializer.deserialize(element, state.getClass());
                if (copy != null) {
                    return (S) copy;
                }
            }
        }
        catch (XmlSerializationException e) {
            LOG.warn("Can't copy profiler configuration " + state + " of " + getId(), e);
        }

        S copy = getTemplateState();
        copy.setDisplayName(state.getDisplayName());
        return copy;
    }

    /**
     * @return the state typed for this configuration type, or null when it belongs to another type
     */
    @SuppressWarnings("unchecked")
    public final @Nullable S asState(ProfilerConfigurationState state) {
        if (!getId().equals(state.getConfigurationTypeId())) {
            return null;
        }

        Class<?> stateClass = myStateClass;
        if (stateClass == null) {
            stateClass = getTemplateState().getClass();
            myStateClass = stateClass;
        }
        return stateClass.isInstance(state) ? (S) state : null;
    }

    /**
     * {@link #createStarter} for a caller holding the type as a wildcard.
     */
    public final @Nullable ProfilerStarter createStarterFor(ProfilerConfigurationState state) {
        S typed = asState(state);
        return typed == null ? null : createStarter(typed);
    }

    /**
     * {@link #createAttacher} for a caller holding the type as a wildcard.
     */
    public final @Nullable ProfilerAttacher createAttacherFor(ProfilerConfigurationState state) {
        S typed = asState(state);
        return typed == null ? null : createAttacher(typed);
    }

    /**
     * {@link #createConfigurable} for a caller holding the type as a wildcard.
     */
    public final @Nullable UnnamedConfigurable createConfigurableFor(ProfilerConfigurationState state) {
        S typed = asState(state);
        return typed == null ? null : createConfigurable(typed);
    }

    /**
     * {@link #copyState} for a caller holding the type as a wildcard.
     */
    public final @Nullable ProfilerConfigurationState copyStateFor(ProfilerConfigurationState state) {
        S typed = asState(state);
        return typed == null ? null : copyState(typed);
    }
}
