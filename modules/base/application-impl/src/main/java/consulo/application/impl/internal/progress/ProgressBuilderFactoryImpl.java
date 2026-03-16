/*
 * Copyright 2013-2025 consulo.io
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
package consulo.application.impl.internal.progress;

import consulo.annotation.component.ServiceImpl;
import consulo.application.internal.ProgressManagerEx;
import consulo.application.progress.ProgressBuilder;
import consulo.application.progress.ProgressBuilderFactory;
import consulo.application.progress.ProgressManager;
import consulo.component.ComponentManager;
import consulo.localize.LocalizeValue;
import consulo.ui.UIAccess;
import consulo.util.concurrent.coroutine.Coroutine;
import org.jspecify.annotations.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2025-05-06
 */
@ServiceImpl
@Singleton
public class ProgressBuilderFactoryImpl implements ProgressBuilderFactory {
    private final ProgressManagerEx myProgressManager;

    @Inject
    public ProgressBuilderFactoryImpl(ProgressManager progressManager) {
        myProgressManager = (ProgressManagerEx) progressManager;
    }

    @Override
    public ProgressBuilder newProgressBuilder(@Nullable ComponentManager project, LocalizeValue title) {
        return new BaseProgressBuilderImpl(project, title) {
            @Override
            public <V> CompletableFuture<V> execute(UIAccess uiAccess,
                                                    Supplier<Coroutine<?, V>> supplier) {
                assertCreated();
                return myProgressManager.executeTask(uiAccess, myProject, myTitle, myModal, myCancelable, supplier);
            }
        };
    }
}
