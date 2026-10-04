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
package consulo.ui.ex.grid;

import consulo.component.util.ModificationTracker;
import consulo.ui.grid.GridPagingModel;
import org.jspecify.annotations.Nullable;

import java.time.ZoneId;
import java.util.List;

/**
 * Changes are published on {@link DataGridSettingsListener}.
 */
public interface DataGridSettings {
    int DEFAULT_PAGE_SIZE = 500;

    /**
     * The page size of the settings, or the default one without settings. It is declared here because {@code GridUtilCore} cannot
     * see the settings.
     */
    static int getPageSize(@Nullable DataGridSettings settings) {
        return settings == null
            ? DEFAULT_PAGE_SIZE
            : settings.isLimitPageSize()
            ? settings.getPageSize()
            : GridPagingModel.UNLIMITED_PAGE_SIZE;
    }

    void setEnablePagingInInEditorResultsByDefault(boolean enablePagingInInEditorResultsByDefault);

    boolean isEnablePagingInInEditorResultsByDefault();

    boolean isDetectTextInBinaryColumns();

    boolean isDetectUUIDInBinaryColumns();

    boolean isAddToSortViaAltClick();

    void setAddToSortViaAltClick(boolean value);

    void setAutoTransposeMode(AutoTransposeMode autoTransposeMode);

    AutoTransposeMode getAutoTransposeMode();

    void setEnableLocalFilterByDefault(boolean enableLocalFilterByDefault);

    boolean isEnableLocalFilterByDefault();

    boolean isDisableGridFloatingToolbar();

    void setDisableGridFloatingToolbar(boolean disableGridFloatingToolbar);

    PagingDisplayMode getPagingDisplayMode();

    void setPagingDisplayMode(PagingDisplayMode pagingDisplayMode);

    boolean isEnableImmediateCompletionInGridCells();

    void setEnableImmediateCompletionInGridCells(boolean enableImmediateCompletionInGridCells);

    int getBytesLimitPerValue();

    int getFiltersHistorySize();

    void setBytesLimitPerValue(int value);

    List<String> getDisabledAggregators();

    void setDisabledAggregators(List<String> aggregators);

    @Nullable
    String getWidgetAggregator();

    void setWidgetAggregator(@Nullable String aggregator);

    boolean isNumberGroupingEnabled();

    char getNumberGroupingSeparator();

    char getDecimalSeparator();

    String getInfinity();

    String getNan();

    @Nullable
    String getEffectiveNumberPattern();

    @Nullable
    String getEffectiveDateTimePattern();

    @Nullable
    String getEffectiveZonedDateTimePattern();

    @Nullable
    String getEffectiveTimePattern();

    @Nullable
    String getEffectiveZonedTimePattern();

    @Nullable
    String getEffectiveDatePattern();

    @Nullable
    ZoneId getEffectiveZoneId();

    void fireChanged();

    void setPageSize(int value);

    int getPageSize();

    boolean isLimitPageSize();

    enum AutoTransposeMode {
        NEVER,
        ONE_ROW,
        ALWAYS
    }

    enum PagingDisplayMode {
        DATA_EDITOR_TOOLBAR,
        GRID_CENTER_FLOATING,
        GRID_LEFT_FLOATING,
        GRID_RIGHT_FLOATING
    }

    ModificationTracker getModificationTracker();

    default boolean isOpeningOfHttpsLinksAllowed() {
        return false;
    }

    default void setIsOpeningOfHttpsLinksAllowed(boolean value) {
    }

    default boolean isOpeningOfHttpLinksAllowed() {
        return false;
    }

    default void setIsOpeningOfHttpLinksAllowed(boolean value) {
    }

    default boolean isOpeningOfLocalFileUrlsAllowed() {
        return false;
    }

    default void setIsOpeningOfLocalFileUrlsAllowed(boolean value) {
    }

    default boolean isWebUrlWithoutProtocolAssumedHttp() {
        return false;
    }

    default void setIsWebUrlWithoutProtocolAssumedHttp(boolean value) {
    }

    default boolean isFloatingToolbarCustomizable() {
        return true;
    }

    default void setFloatingToolbarCustomizable(boolean value) {
    }

    default boolean isShowGeoAsBinary() {
        return false;
    }

    default void setShowGeoAsBinary(boolean value) {
    }

    default int getFirstRowIndex() {
        return 1;
    }

    default boolean isEditArrayAsText() {
        return false;
    }

    default void setEditArrayAsText(boolean value) {
    }

    default boolean isHideDeletedInArrayGrid() {
        return false;
    }

    default void setHideDeletedInArrayGrid(boolean value) {
    }
}
