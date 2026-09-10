// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.application.util;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.component.persist.PersistentStateComponent;
import consulo.component.persist.State;
import consulo.component.persist.Storage;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author Konstantin Bulenkov
 */
@Singleton
@State(name = "DateTimeFormatter", storages = @Storage("ui-datetime"))
@ServiceAPI(ComponentScope.APPLICATION)
@ServiceImpl
public class DateTimeFormatManager implements PersistentStateComponent<DateTimeFormatManagerState> {
    private static final Logger LOG = LoggerFactory.getLogger(DateTimeFormatManager.class);

    public static DateTimeFormatManager getInstance() {
        return Application.get().getInstance(DateTimeFormatManager.class);
    }

    private DateTimeFormatManagerState myState = new DateTimeFormatManagerState();

    @Override
    public @Nullable DateTimeFormatManagerState getState() {
        return myState;
    }

    @Override
    public void loadState(DateTimeFormatManagerState state) {
        myState = state;
    }

    public boolean isOverrideSystemDateFormat() {
        return myState.overrideSystemDateFormat;
    }

    public void setOverrideSystemDateFormat(boolean overrideSystemDateFormat) {
        myState.overrideSystemDateFormat = overrideSystemDateFormat;
    }

    public boolean isUse24HourTime() {
        return myState.use24HourTime;
    }

    public void setUse24HourTime(boolean use24HourTime) {
        myState.use24HourTime = use24HourTime;
    }

    public void setPrettyFormattingAllowed(boolean prettyFormattingAllowed) {
        myState.prettyFormattingAllowed = prettyFormattingAllowed;
    }

    public boolean isPrettyFormattingAllowed() {
        return myState.prettyFormattingAllowed;
    }

    public @Nullable DateFormat getDateFormat() {
        try {
            return new SimpleDateFormat(myState.pattern);
        }
        catch (IllegalArgumentException e) {
            LOG.warn("Exception while creating date format", e);
        }
        return null;
    }

    public String getDateFormatPattern() {
        return myState.pattern;
    }

    public void setDateFormatPattern(String pattern) {
        try {
            new SimpleDateFormat(pattern);
            myState.pattern = pattern;
        }
        catch (Exception ignored) {
        }
    }
}
