// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
package consulo.review;

import consulo.localize.LocalizeValue;
import consulo.review.localize.ReviewLocalize;

import java.net.ConnectException;
import java.nio.channels.UnresolvedAddressException;

public final class ExceptionUtil {
    private ExceptionUtil() {
    }

    public static LocalizeValue getPresentableMessage(Throwable exception) {
        String localizedMessage = exception.getLocalizedMessage();
        if (localizedMessage != null) {
            return LocalizeValue.of(localizedMessage);
        }

        if (exception instanceof ConnectException) {
            if (exception.getCause() instanceof UnresolvedAddressException) {
                return ReviewLocalize.errorAddressUnresolved();
            }
            return ReviewLocalize.errorConnectionError();
        }
        return ReviewLocalize.errorUnknown();
    }
}
