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
package consulo.application.internal.dateTime;

import consulo.annotation.component.ExtensionImpl;
import consulo.platform.Platform;

import java.text.DateFormat;
import java.util.Locale;

/**
 * @author VISTALL
 * @since 2024-12-26
 */
@ExtensionImpl(order = "last")
public class LCTimeDateTimeFormatProvider implements DateTimeFormatProvider {
    @Override
    public DateFormat[] getFormats(Platform platform) {
        String localeStr = platform.os().getEnvironmentVariable("LC_TIME");
        if (localeStr == null) return null;

        localeStr = localeStr.trim();
        int p = localeStr.indexOf('.');
        if (p > 0) localeStr = localeStr.substring(0, p);
        p = localeStr.indexOf('@');
        if (p > 0) localeStr = localeStr.substring(0, p);

        Locale locale;
        p = localeStr.indexOf('_');
        if (p < 0) {
            locale = new Locale(localeStr);
        }
        else {
            locale = new Locale(localeStr.substring(0, p), localeStr.substring(p + 1));
        }

        DateFormat[] formats = new DateFormat[4];
        formats[0] = DateFormat.getDateInstance(DateFormat.SHORT, locale);
        formats[1] = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
        formats[2] = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
        formats[3] = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale);
        return formats;
    }
}
