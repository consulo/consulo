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
package consulo.ai.impl.internal.chat;

import consulo.ai.AIChatAttachment;
import consulo.ai.AIChatContextManager;
import consulo.annotation.component.ServiceImpl;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-11-13
 */
@ServiceImpl
@Singleton
public class AIChatContextManagerImpl implements AIChatContextManager {
    private final AIChatView myView;

    @Inject
    public AIChatContextManagerImpl(AIChatView view) {
        myView = view;
    }

    @RequiredUIAccess
    @Override
    public void attach(AIChatAttachment attachment) {
        myView.attach(attachment);
    }
}
