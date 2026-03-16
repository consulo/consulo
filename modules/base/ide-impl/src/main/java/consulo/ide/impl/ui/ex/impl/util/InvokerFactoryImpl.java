/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.ui.ex.impl.util;

import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ide.impl.idea.util.concurrency.InvokerImpl;
import consulo.ui.UIAccess;
import consulo.ui.ex.util.Invoker;
import consulo.ui.ex.util.InvokerFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author VISTALL
 * @since 27-Feb-22
 */
@Singleton
@ServiceImpl
public class InvokerFactoryImpl implements InvokerFactory {
    private final Application myApplication;

    @Inject
    public InvokerFactoryImpl(Application application) {
        myApplication = application;
    }

    @Override
    public Invoker forEventDispatchThread(UIAccess uiAccess, Disposable parent) {
        return InvokerImpl.forEventDispatchThread(uiAccess, parent);
    }

    @Override
    public Invoker forBackgroundThreadWithReadAction(Disposable parent) {
        return InvokerImpl.forBackgroundThreadWithReadAction(parent);
    }

    @Override
    public Invoker forBackgroundThreadWithoutReadAction(Disposable parent) {
        return InvokerImpl.forBackgroundThreadWithoutReadAction(parent);
    }
}
