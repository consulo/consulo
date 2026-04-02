// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import io.mockk.MockKKt;
import io.mockk.MockKMatcherScope;
import kotlin.Unit;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.flow.MutableSharedFlow;
import kotlinx.coroutines.flow.SharedFlowKt;
import kotlinx.coroutines.test.TestBuildersKt;
import kotlinx.coroutines.test.UnconfinedTestDispatcher;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class AccountUrlAuthenticationFailuresHolderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void testNotFailedOnTokenChange() {
    TestBuildersKt.runTest(
      new UnconfinedTestDispatcher(null, null),
      0,
      (scope, continuation) -> {
        MutableSharedFlow<String> credsFlow = SharedFlowKt.MutableSharedFlow(0, 0, null);
        AccountManager<Account, String> accountManager = MockKKt.mockk(cls -> {
          io.mockk.MockKKt.every(stub -> {
            stub.getProperty$default(accountManager, "getCredentialsFlow", null, 0, null);
            return Unit.INSTANCE;
          }, answer -> {
            answer.returns(credsFlow);
            return Unit.INSTANCE;
          });
          return Unit.INSTANCE;
        });

        AccountUrlAuthenticationFailuresHolder holder = new AccountUrlAuthenticationFailuresHolder(scope, () -> accountManager);

        Account account = MockKKt.mockk(cls -> Unit.INSTANCE);
        holder.markFailed(account, "123");
        assertTrue(holder.isFailed(account, "123"));

        credsFlow.emit("newToken", continuation);
        assertFalse(holder.isFailed(account, "123"));
        return Unit.INSTANCE;
      }
    );
  }
}
