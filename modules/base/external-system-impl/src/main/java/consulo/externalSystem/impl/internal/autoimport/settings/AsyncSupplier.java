// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.externalSystem.impl.internal.autoimport.settings;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface AsyncSupplier<R> {
    /**
     * Supply a value to the consumer, when the value available
     * Note: Implementation can call {@code consumer} before returning from the method
     */
    void supply(Consumer<R> consumer);

    static <R> AsyncSupplier<R> blocking(Supplier<R> supplier) {
        return consumer -> consumer.accept(supplier.get());
    }
}
