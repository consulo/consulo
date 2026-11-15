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
package consulo.language.codeStyle.service;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.language.psi.PsiFile;

/**
 * Excludes a {@link FormattingService} from selection for a particular file
 * (see {@code FormattingServiceUtil#findService}); the next applicable service is used instead,
 * with {@code CoreFormattingService} as the final fallback.
 * <p>
 * Used to keep external formatters away from files opened in the safe mode:
 * such tools may discover configs and plugins next to the formatted file and execute them.
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface FormattingServiceSuppressor {
    @RequiredReadAction
    boolean isSuppressed(PsiFile file, FormattingService service);
}
