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
package consulo.ui.ex.impl.internal.util;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.ui.TreeExecutor;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 2026-08-29
 */
@Singleton
@ServiceImpl
public class ApplicationTreeExecutorFactoryImpl implements ApplicationTreeExecutorFactory {
    @Override
    public TreeExecutor forBackgroundThreadWithReadAction(Disposable parent) {
        return InvokerImpl.forBackgroundThreadWithReadAction(parent);
    }

    @Override
    public TreeExecutor forBackgroundPoolWithReadAction(Disposable parent) {
        return InvokerImpl.forBackgroundPoolWithReadAction(parent);
    }

    @Override
    public TreeExecutor forBackgroundThreadWithoutReadAction(Disposable parent) {
        return InvokerImpl.forBackgroundThreadWithoutReadAction(parent);
    }
}
