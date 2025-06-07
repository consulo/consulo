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
package consulo.desktop.awt.os.mac.internal;

import consulo.annotation.component.ExtensionImpl;
import consulo.application.internal.dateTime.DateTimeFormatProvider;
import consulo.application.util.mac.foundation.Foundation;
import consulo.application.util.mac.foundation.ID;
import consulo.platform.Platform;
import consulo.util.jna.JnaLoader;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * @author VISTALL
 * @since 2024-12-26
 */
@ExtensionImpl
public class MacDateTimeFormatProvider implements DateTimeFormatProvider {
    @Override
    public DateFormat[] getFormats(Platform platform) {
        return platform.os().isMac() && JnaLoader.isLoaded() ? getMacFormats() : null;
    }

    private static DateFormat[] getMacFormats() {
        final int MacFormatterNoStyle = 0;
        final int MacFormatterShortStyle = 1;
        final int MacFormatterMediumStyle = 2;
        final int MacFormatterBehavior_10_4 = 1040;

        ID autoReleasePool = Foundation.invoke("NSAutoreleasePool", "new");
        try {
            ID dateFormatter = Foundation.invoke("NSDateFormatter", "new");
            Foundation.invoke(dateFormatter, Foundation.createSelector("setFormatterBehavior:"), MacFormatterBehavior_10_4);

            DateFormat[] formats = new DateFormat[4];
            formats[0] = invokeFormatter(dateFormatter, MacFormatterNoStyle, MacFormatterShortStyle);  // short date
            formats[1] = invokeFormatter(dateFormatter, MacFormatterShortStyle, MacFormatterNoStyle);  // short time
            formats[2] = invokeFormatter(dateFormatter, MacFormatterMediumStyle, MacFormatterNoStyle);  // medium time
            formats[3] = invokeFormatter(dateFormatter, MacFormatterShortStyle, MacFormatterShortStyle);  // short date/time
            return formats;
        }
        finally {
            Foundation.invoke(autoReleasePool, Foundation.createSelector("release"));
        }
    }

    private static DateFormat invokeFormatter(ID dateFormatter, int timeStyle, int dateStyle) {
        Foundation.invoke(dateFormatter, Foundation.createSelector("setTimeStyle:"), timeStyle);
        Foundation.invoke(dateFormatter, Foundation.createSelector("setDateStyle:"), dateStyle);
        String format = Foundation.toStringViaUTF8(Foundation.invoke(dateFormatter, Foundation.createSelector("dateFormat")));
        assert format != null;
        return new SimpleDateFormat(format.trim());
    }
}
