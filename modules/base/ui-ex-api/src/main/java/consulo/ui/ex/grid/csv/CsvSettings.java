// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

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
package consulo.ui.ex.grid.csv;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.messagebus.MessageBus;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import consulo.component.util.ModificationTracker;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import consulo.util.xml.serializer.XmlSerializer;
import consulo.util.xml.serializer.XmlSerializerUtil;
import consulo.util.xml.serializer.annotation.AbstractCollection;
import consulo.util.xml.serializer.annotation.Attribute;
import consulo.util.xml.serializer.annotation.Tag;
import consulo.util.xml.serializer.annotation.Transient;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * The stored CSV formats of the application. Changes are published on {@link CsvFormatsSettingsListener}.
 */
@Singleton
@State(name = "CsvSettings", storages = @Storage(CsvSettings.STATE_NAME + ".xml"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public final class CsvSettings implements PersistentStateComponent<CsvSettings>, ModificationTracker, CsvFormatsSettings {
    private static final int CURRENT_VERSION = 1;

    public static final String STATE_NAME = "csvSettings";

    private final AtomicLong myModificationCount = new AtomicLong();

    @Override
    public long getModificationCount() {
        return myModificationCount.get();
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static CsvSettings getSettings() {
        CsvSettings settings = Application.get().getInstance(CsvSettings.class);
        //loadState not called
        if (settings.myVersion == 0) {
            synchronized (settings) {
                settings.ensureDefaultsSet();
            }
        }
        return settings;
    }

    @Override
    public void fireChanged() {
        fireSettingsChanged();
    }

    public static void fireSettingsChanged() {
        CsvSettings instance = getSettings();
        instance.myModificationCount.incrementAndGet();
        MessageBus messageBus = Application.get().getMessageBus();
        messageBus.syncPublisher(CsvFormatsSettingsListener.class).settingsChanged();
    }

    public static CsvSettings create() {
        CsvSettings settings = new CsvSettings();
        settings.ensureDefaultsSet();
        return settings;
    }

    @Inject
    CsvSettings() {
    }

    @Override
    public CsvSettings getState() {
        return this;
    }

    @TestOnly
    public CsvSettings copy() {
        return Objects.requireNonNull(XmlSerializer.deserialize(Objects.requireNonNull(XmlSerializer.serialize(this)), CsvSettings.class));
    }

    @Override
    public void loadState(CsvSettings state) {
        XmlSerializerUtil.copyBean(state, this);
        ensureDefaultsSet();
    }

    /**
     * The formats are stored through the {@link #csvFormats} field, so this accessor pair is not a serialized property.
     */
    @Override
    @Transient
    public List<CsvFormat> getCsvFormats() {
        List<CsvFormat> formats = getImmutableFormats(csvFormats);
        return formats.isEmpty() ? getDefaultFormats() : formats;
    }

    public static List<CsvFormat> getDefaultFormats() {
        return Arrays.asList(
            CsvFormats.CSV_FORMAT.get(),
            CsvFormats.TSV_FORMAT.get(),
            CsvFormats.PIPE_SEPARATED_FORMAT.get(),
            CsvFormats.SEMICOLON_SEPARATED_FORMAT.get()
        );
    }

    @Override
    @Transient
    public void setCsvFormats(List<CsvFormat> formats) {
        csvFormats = ContainerUtil.map(formats, PersistentCsvFormat::new);
    }

    private void ensureDefaultsSet() {
        if (getImmutableFormats(csvFormats).isEmpty()) {
            setCsvFormats(getDefaultFormats());
            myModificationCount.incrementAndGet();
        }
        myVersion = CURRENT_VERSION;
    }

    public static void addNewFormat(List<PersistentCsvFormat> csvFormats,
                                    CsvFormat newFormat,
                                    @Nullable CsvFormat addAfterThis) {
        if (ContainerUtil.exists(csvFormats, f -> formatsSimilar(newFormat, f))) {
            return;
        }
        int tsvIndex = addAfterThis == null
            ? csvFormats.size() - 1
            : ContainerUtil.indexOf(csvFormats, f -> f.id.equals(addAfterThis.id));
        csvFormats.add(tsvIndex + 1, new PersistentCsvFormat(newFormat));
    }

    public static boolean formatsSimilar(CsvFormat format, PersistentCsvFormat f) {
        if (StringUtil.equals(f.name, format.name)) {
            return true;
        }
        @Nullable CsvFormat immutable = f.immutable();
        return CsvFormatsSettings.formatsSimilar(format, immutable);
    }

    private static List<CsvFormat> getImmutableFormats(List<PersistentCsvFormat> persistentFormats) {
        return ContainerUtil.mapNotNull(persistentFormats, format -> format == null ? null : format.immutable());
    }

    @Attribute("version")
    private int myVersion = 0;

    @Tag("csv-formats")
    @AbstractCollection(surroundWithTag = false, elementTypes = PersistentCsvFormat.class)
    public List<PersistentCsvFormat> csvFormats = new ArrayList<>();
}
