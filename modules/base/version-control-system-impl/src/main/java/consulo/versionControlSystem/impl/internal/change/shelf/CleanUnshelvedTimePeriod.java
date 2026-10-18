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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.localize.LocalizeValue;
import consulo.versionControlSystem.localize.VcsLocalize;

import java.util.Calendar;

/**
 * How old an unshelved change list must be to be cleaned.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
enum CleanUnshelvedTimePeriod {
    WEEK(Calendar.DAY_OF_MONTH, -7, VcsLocalize.shelveCleanUnshelvedPeriodWeek()),
    MONTH(Calendar.MONTH, -1, VcsLocalize.shelveCleanUnshelvedPeriodMonth()),
    YEAR(Calendar.YEAR, -1, VcsLocalize.shelveCleanUnshelvedPeriodYear());

    private final int myField;
    private final int myAmount;
    private final LocalizeValue myText;

    CleanUnshelvedTimePeriod(int field, int amount, LocalizeValue text) {
        myField = field;
        myAmount = amount;
        myText = text;
    }

    long getTimeLimitInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(myField, myAmount);
        return calendar.getTimeInMillis();
    }

    LocalizeValue getText() {
        return myText;
    }
}
