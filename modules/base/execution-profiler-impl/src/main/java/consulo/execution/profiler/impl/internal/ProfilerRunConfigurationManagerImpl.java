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
package consulo.execution.profiler.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.disposer.Disposable;
import consulo.execution.profiler.configuration.ProfilerConfigurationState;
import consulo.execution.profiler.configuration.ProfilerConfigurationTypeBase;
import consulo.execution.profiler.configuration.ProfilerRunConfigurationManager;
import consulo.logging.Logger;
import consulo.util.xml.serializer.XmlSerializationException;
import consulo.util.xml.serializer.XmlSerializer;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jdom.Element;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Singleton
@State(name = "ProfilerRunConfigurationManager", storages = @Storage("profiler"))
@ServiceImpl
public class ProfilerRunConfigurationManagerImpl implements ProfilerRunConfigurationManager, PersistentStateComponent<Element> {
    private static final Logger LOG = Logger.getInstance(ProfilerRunConfigurationManagerImpl.class);

    private static final String STATE_ELEMENT = "state";
    private static final String CONFIGURATION_ELEMENT = "configuration";
    private static final String SEEDED_TYPE_ELEMENT = "seeded-type";
    private static final String TYPE_ID_ATTRIBUTE = "typeId";
    private static final String ID_ATTRIBUTE = "id";

    private final Application myApplication;
    private final Object myLock = new Object();
    private final List<Runnable> myListeners = new CopyOnWriteArrayList<>();

    private List<ProfilerConfigurationState> myConfigurations = List.of();
    private List<Element> myUnknownConfigurations = List.of();
    private Set<String> mySeededTypes = new LinkedHashSet<>();

    @Inject
    public ProfilerRunConfigurationManagerImpl(Application application) {
        myApplication = application;
    }

    @Override
    public List<ProfilerConfigurationState> getConfigurations() {
        synchronized (myLock) {
            return myConfigurations;
        }
    }

    @Override
    public void setConfigurations(List<? extends ProfilerConfigurationState> configurations) {
        synchronized (myLock) {
            myConfigurations = List.copyOf(configurations);
        }
        fireChanged();
    }

    @Override
    public Disposable addChangeListener(Runnable listener) {
        myListeners.add(listener);
        return () -> myListeners.remove(listener);
    }

    @Override
    public @Nullable Element getState() {
        List<ProfilerConfigurationState> configurations;
        List<Element> unknownConfigurations;
        List<String> seededTypes;
        synchronized (myLock) {
            configurations = myConfigurations;
            unknownConfigurations = myUnknownConfigurations;
            seededTypes = List.copyOf(mySeededTypes);
        }

        Element state = new Element(STATE_ELEMENT);
        for (ProfilerConfigurationState configuration : configurations) {
            Element element = writeConfiguration(configuration);
            if (element != null) {
                state.addContent(element);
            }
        }
        for (Element unknown : unknownConfigurations) {
            state.addContent(unknown.clone());
        }
        for (String typeId : seededTypes) {
            state.addContent(new Element(SEEDED_TYPE_ELEMENT).setAttribute(ID_ATTRIBUTE, typeId));
        }
        return state;
    }

    @Override
    public void loadState(Element state) {
        Map<String, ProfilerConfigurationTypeBase<?>> types = collectTypes();

        List<ProfilerConfigurationState> configurations = new ArrayList<>();
        List<Element> unknownConfigurations = new ArrayList<>();
        for (Element element : state.getChildren(CONFIGURATION_ELEMENT)) {
            String typeId = element.getAttributeValue(TYPE_ID_ATTRIBUTE);
            if (typeId == null || typeId.isEmpty()) {
                continue;
            }

            ProfilerConfigurationTypeBase<?> type = types.get(typeId);
            ProfilerConfigurationState configuration = type == null ? null : readConfiguration(type, element);
            if (configuration == null) {
                unknownConfigurations.add(element.clone());
            }
            else {
                configurations.add(configuration);
            }
        }

        Set<String> seededTypes = new LinkedHashSet<>();
        for (Element element : state.getChildren(SEEDED_TYPE_ELEMENT)) {
            String typeId = element.getAttributeValue(ID_ATTRIBUTE);
            if (typeId != null && !typeId.isEmpty()) {
                seededTypes.add(typeId);
            }
        }

        synchronized (myLock) {
            myConfigurations = List.copyOf(configurations);
            myUnknownConfigurations = List.copyOf(unknownConfigurations);
            mySeededTypes = seededTypes;
        }
    }

    @Override
    public void afterLoad(boolean first) {
        Map<String, ProfilerConfigurationTypeBase<?>> types = collectTypes();

        Set<String> seededTypes;
        synchronized (myLock) {
            seededTypes = Set.copyOf(mySeededTypes);
        }

        List<ProfilerConfigurationState> templates = new ArrayList<>();
        List<String> newTypeIds = new ArrayList<>();
        for (ProfilerConfigurationTypeBase<?> type : types.values()) {
            if (seededTypes.contains(type.getId())) {
                continue;
            }

            ProfilerConfigurationState template = createTemplate(type);
            if (template != null) {
                templates.add(template);
                newTypeIds.add(type.getId());
            }
        }

        if (!newTypeIds.isEmpty()) {
            synchronized (myLock) {
                List<ProfilerConfigurationState> configurations = new ArrayList<>(myConfigurations);
                configurations.addAll(templates);
                myConfigurations = List.copyOf(configurations);
                mySeededTypes.addAll(newTypeIds);
            }
        }

        fireChanged();
    }

    private Map<String, ProfilerConfigurationTypeBase<?>> collectTypes() {
        Map<String, ProfilerConfigurationTypeBase<?>> types = new LinkedHashMap<>();
        myApplication.getExtensionPoint(ProfilerConfigurationTypeBase.class).forEach(type -> types.putIfAbsent(type.getId(), type));
        return types;
    }

    private static @Nullable ProfilerConfigurationState createTemplate(ProfilerConfigurationTypeBase<?> type) {
        try {
            return type.getTemplateState();
        }
        catch (Throwable e) {
            LOG.error("Profiler configuration type " + type.getId() + " failed to create its template state", e);
            return null;
        }
    }

    private static @Nullable Element writeConfiguration(ProfilerConfigurationState configuration) {
        Element element = new Element(CONFIGURATION_ELEMENT);
        element.setAttribute(TYPE_ID_ATTRIBUTE, configuration.getConfigurationTypeId());
        try {
            Element bean = XmlSerializer.serialize(configuration);
            if (bean != null) {
                element.addContent(bean);
            }
            return element;
        }
        catch (XmlSerializationException e) {
            LOG.error("Can't store profiler configuration " + configuration, e);
            return null;
        }
    }

    private static @Nullable ProfilerConfigurationState readConfiguration(ProfilerConfigurationTypeBase<?> type, Element element) {
        ProfilerConfigurationState configuration = createTemplate(type);
        if (configuration == null) {
            return null;
        }

        List<Element> children = element.getChildren();
        if (children.isEmpty()) {
            return configuration;
        }

        try {
            XmlSerializer.deserializeInto(configuration, children.get(0));
            return configuration;
        }
        catch (XmlSerializationException e) {
            LOG.warn("Can't read profiler configuration of " + type.getId(), e);
            return null;
        }
    }

    private void fireChanged() {
        for (Runnable listener : myListeners) {
            try {
                listener.run();
            }
            catch (Throwable e) {
                LOG.error("Profiler configurations listener failed", e);
            }
        }
    }
}
