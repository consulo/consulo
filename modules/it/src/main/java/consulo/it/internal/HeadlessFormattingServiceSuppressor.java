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
package consulo.it.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.codeStyle.service.FormattingService;
import consulo.language.codeStyle.service.FormattingServiceSuppressor;
import consulo.language.psi.PsiFile;
import org.jspecify.annotations.Nullable;

/**
 * Formatting service suppressor used by the formatting integration tests: suppresses the services of one class while a test sets it.
 *
 * @author VISTALL
 */
@ExtensionImpl
public class HeadlessFormattingServiceSuppressor implements FormattingServiceSuppressor {
    private static volatile @Nullable Class<? extends FormattingService> ourSuppressed;

    public static void setSuppressed(@Nullable Class<? extends FormattingService> serviceClass) {
        ourSuppressed = serviceClass;
    }

    @Override
    @RequiredReadAction
    public boolean isSuppressed(PsiFile file, FormattingService service) {
        Class<? extends FormattingService> suppressed = ourSuppressed;
        return suppressed != null && suppressed.isInstance(service);
    }
}
