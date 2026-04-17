// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.collaboration.auth;

import org.jspecify.annotations.Nullable;

/**
 * Holds default account.
 *
 * @param <A> account type
 */
public interface DefaultAccountHolder<A extends Account> {
    /**
     * Default account.
     */
    @Nullable A getAccount();

    void setAccount(@Nullable A account);
}
