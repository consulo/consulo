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
package consulo.ui.impl;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.logging.Logger;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.LoadingLayout;
import consulo.util.lang.ControlFlowException;

import java.util.concurrent.Future;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author VISTALL
 * @since 2026-09-30
 */
public final class LoadingLayoutLoader {
    private static final Logger LOG = Logger.getInstance(LoadingLayoutLoader.class);

    private LoadingLayoutLoader() {
    }

    @RequiredUIAccess
    public static <L, V> Future<V> startLoading(LoadingLayout<L> layout, Supplier<V> valueGetter, BiConsumer<L, V> uiSetter) {
        UIAccess uiAccess = UIAccess.current();

        layout.startLoading();

        return AppExecutorUtil.getAppScheduledExecutorService().submit(() -> {
            V value;
            try {
                value = valueGetter.get();
            }
            catch (Throwable e) {
                if (!(e instanceof ControlFlowException)) {
                    LOG.error(e);
                }
                uiAccess.give(() -> layout.stopLoading(inner -> {
                }));
                throw e;
            }

            uiAccess.give(() -> layout.stopLoading(inner -> {
                try {
                    uiSetter.accept(inner, value);
                }
                catch (Throwable e) {
                    LOG.error(e);
                }
            }));
            return value;
        });
    }
}
