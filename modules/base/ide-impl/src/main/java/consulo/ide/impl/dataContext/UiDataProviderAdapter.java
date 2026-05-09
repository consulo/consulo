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
package consulo.ide.impl.dataContext;

import consulo.application.Application;
import consulo.dataContext.DataProvider;
import consulo.dataContext.UiDataProvider;
import consulo.dataContext.UiDataRule;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

/**
 * Adapts a {@link UiDataProvider} to the {@link DataProvider} interface.
 */
public class UiDataProviderAdapter implements DataProvider {
    private final UiDataProvider myProvider;

    public UiDataProviderAdapter(UiDataProvider provider) {
        myProvider = provider;
    }

    public UiDataProvider getProvider() {
        return myProvider;
    }

    @Override
    public @Nullable Object getData(Key<?> dataId) {
        DataSinkImpl sink = new DataSinkImpl();
        sink.collectFromProvider(myProvider, Application.get().getExtensionPoint(UiDataRule.class));
        return sink.resolve(dataId);
    }
}
