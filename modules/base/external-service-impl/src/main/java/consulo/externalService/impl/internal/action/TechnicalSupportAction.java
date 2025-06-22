/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.externalService.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.externalService.impl.internal.WebServiceApi;
import consulo.platform.Platform;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import jakarta.annotation.Nonnull;

/**
 * @author yole
 * @since 2006-09-26
 */
@ActionImpl(id = "TechnicalSupport")
public class TechnicalSupportAction extends DumbAwareAction {
    @RequiredUIAccess
    @Override
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Platform.current().openInBrowser(WebServiceApi.DISCUSSION.buildUrl());
    }
}