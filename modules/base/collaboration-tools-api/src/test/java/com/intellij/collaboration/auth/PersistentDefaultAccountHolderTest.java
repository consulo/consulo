// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.auth;

import com.intellij.openapi.project.Project;
import com.intellij.platform.util.coroutines.ChildScopeKt;
import kotlin.Unit;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.test.TestBuildersKt;
import kotlinx.coroutines.test.UnconfinedTestDispatcher;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;

import static kotlin.test.AssertionsKt.assertEquals;
import static kotlin.test.AssertionsKt.assertNull;

final class PersistentDefaultAccountHolderTest {
  private final TestAccount account1 = new TestAccount("user");

  @Test
  public void defaultAccountIsNotUpdatedIfUnknownByAccountManager() {
    TestBuildersKt.runTest((scope, continuation) -> {
      AccountManagerBase<TestAccount, String> accountManager = setupMockAccountManager(Set.of());

      CoroutineScope dahScope = ChildScopeKt.childScope(scope.getBackgroundScope(), "DefaultAccountHolderScope");
      PersistentDefaultAccountHolder<TestAccount> defaultAccountHolder = setupDefaultAccountHolder(dahScope, accountManager);

      defaultAccountHolder.setAccount(account1);

      assertNull(defaultAccountHolder.getAccount(), null);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void defaultAccountIsUpdatedIfKnownByAccountManager() {
    TestBuildersKt.runTest((scope, continuation) -> {
      AccountManagerBase<TestAccount, String> accountManager = setupMockAccountManager(Set.of(account1));

      CoroutineScope dahScope = ChildScopeKt.childScope(scope.getBackgroundScope(), "DefaultAccountHolderScope");
      PersistentDefaultAccountHolder<TestAccount> defaultAccountHolder = setupDefaultAccountHolder(dahScope, accountManager);

      defaultAccountHolder.setAccount(account1);

      assertEquals(account1, defaultAccountHolder.getAccount(), null);
      return Unit.INSTANCE;
    });
  }

  @Test
  @ExperimentalCoroutinesApi
  public void defaultAccountIsClearedIfNoLongerKnownByAccountManager() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<Set<TestAccount>> accountFlow = StateFlowKt.MutableStateFlow(Set.of(account1));
      AccountManagerBase<TestAccount, String> accountManager = setupMockAccountManager(accountFlow);

      // create the scope with `UnconfinedTestDispatcher` to make sure flow updates are applied immediately
      CoroutineScope dahScope = ChildScopeKt.childScope(
        scope.getBackgroundScope(), "DefaultAccountHolderScope",
        new UnconfinedTestDispatcher(scope.getTestScheduler(), null), true);
      PersistentDefaultAccountHolder<TestAccount> defaultAccountHolder = setupDefaultAccountHolder(dahScope, accountManager);

      // first set the account (should be fine)
      defaultAccountHolder.setAccount(account1);

      // then update the accounts-state (should clear the account value)
      accountFlow.setValue(Set.of());

      assertNull(defaultAccountHolder.getAccount(), null);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void defaultAccountIsClearedIfNoLongerKnownByAccountManagerWhenSaving() {
    TestBuildersKt.runTest((scope, continuation) -> {
      MutableStateFlow<Set<TestAccount>> accountFlow = StateFlowKt.MutableStateFlow(Set.of(account1));
      AccountManagerBase<TestAccount, String> accountManager = setupMockAccountManager(accountFlow);

      CoroutineScope dahScope = ChildScopeKt.childScope(scope.getBackgroundScope(), "DefaultAccountHolderScope");
      PersistentDefaultAccountHolder<TestAccount> defaultAccountHolder = setupDefaultAccountHolder(dahScope, accountManager);

      // first set the account (should be fine), then remove the account from known accounts
      defaultAccountHolder.setAccount(account1);

      accountFlow.setValue(Set.of());
      // do not advance to ensure the timing is: accountFlow updates, dispatches coroutine,
      // but it's not run yet because this body doesn't yield.
      // Hence, the `getState` filtering has to be used.

      assertNull(defaultAccountHolder.getState().getDefaultAccountId(), null);
      return Unit.INSTANCE;
    });
  }

  private PersistentDefaultAccountHolder<TestAccount> setupDefaultAccountHolder(
    CoroutineScope scope, AccountManager<TestAccount, String> accountManager
  ) {
    return new PersistentDefaultAccountHolder<>(Mockito.mock(Project.class), scope) {
      @Override
      protected @Nonnull AccountManager<TestAccount, ?> accountManager() {
        return accountManager;
      }

      @Override
      protected void notifyDefaultAccountMissing() {
      }
    };
  }

  @SuppressWarnings("unchecked")
  private AccountManagerBase<TestAccount, String> setupMockAccountManager(Set<TestAccount> accounts) {
    return setupMockAccountManager(StateFlowKt.MutableStateFlow(accounts));
  }

  @SuppressWarnings("unchecked")
  private AccountManagerBase<TestAccount, String> setupMockAccountManager(StateFlow<Set<TestAccount>> accounts) {
    AccountManagerBase<TestAccount, String> mock = Mockito.mock(AccountManagerBase.class);
    Mockito.when(mock.getAccountsState()).thenReturn((StateFlow)accounts);
    return mock;
  }

  private static final class TestAccount extends Account {
    private final String id;

    TestAccount(@Nonnull String id) {
      this.id = id;
    }

    @Override
    public @Nonnull String getId() {
      return id;
    }

    @Override
    public @Nonnull String getName() {
      return id;
    }

    @Override
    public @Nonnull String toString() {
      return "TestAccount(id=" + id + ")";
    }
  }
}
