// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.collaboration.auth;

import io.mockk.MockKKt;
import kotlin.Unit;
import kotlinx.coroutines.flow.MutableStateFlow;
import kotlinx.coroutines.flow.StateFlow;
import kotlinx.coroutines.flow.StateFlowKt;
import kotlinx.coroutines.test.TestBuildersKt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static kotlin.test.AssertionsKt.assertEquals;
import static kotlin.test.AssertionsKt.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(JUnit4.class)
public final class AccountManagerBaseTest {

  private final TestAccountsRepository accountsRepository = new TestAccountsRepository();

  private final TestCredentialsRepository credentialsRepository = new TestCredentialsRepository();

  private AccountManagerBase<MockAccount, String> manager;

  private final MockAccount account = new MockAccount();
  private final MockAccount account2 = new MockAccount();
  private final MockAccount account3 = new MockAccount();

  @Before
  public void setUp() {
    manager = new AccountManagerBase<>(MockKKt.mockk(cls -> {
      cls.relaxUnitFun(true);
      return Unit.INSTANCE;
    })) {
      @Override
      protected @Nonnull ObservableAccountsRepository<MockAccount> accountsRepository() {
        return accountsRepository;
      }

      @Override
      protected @Nonnull CredentialsRepository<MockAccount, String> credentialsRepository() {
        return credentialsRepository;
      }
    };
  }

  @Test
  public void testListState() {
    TestBuildersKt.runTest((scope, continuation) -> {
      StateFlow<Set<MockAccount>> state = manager.getAccountsState();
      manager.updateAccount(account, "test", continuation);
      assertEquals(1, state.getValue().size(), null);
      manager.updateAccount(account, "test", continuation);
      assertEquals(1, state.getValue().size(), null);
      manager.removeAccount(account, continuation);
      assertTrue(state.getValue().isEmpty());

      manager.updateAccounts(Map.of(account, "test", account2, "test"), continuation);
      assertEquals(2, state.getValue().size(), null);
      manager.updateAccounts(Map.of(account, "test", account2, "test"), continuation);
      assertEquals(2, state.getValue().size(), null);
      manager.updateAccount(account, "test", continuation);
      assertEquals(2, state.getValue().size(), null);
      manager.removeAccount(account, continuation);
      assertEquals(1, state.getValue().size(), null);
      manager.removeAccount(account2, continuation);
      assertTrue(state.getValue().isEmpty());
      return Unit.INSTANCE;
    });
  }

  @Test
  public void testAccountUsableAfterAddition() {
    TestBuildersKt.runTest((scope, continuation) -> {
      manager.updateAccount(account, "test", continuation);
      assertNotNull(manager.findCredentials(account, continuation), null);
      return Unit.INSTANCE;
    });
  }

  @Test
  public void testExternalAccountChangesAvailableInManager() {
    TestBuildersKt.runTest((scope, continuation) -> {
      StateFlow<Set<MockAccount>> state = manager.getAccountsState();

      manager.updateAccount(account, "test", continuation);
      assertEquals(1, state.getValue().size(), null);

      accountsRepository.changeAccountsExternally(Set.of(account, account2));
      assertEquals(2, state.getValue().size(), null);

      manager.updateAccount(account3, "test", continuation);
      assertEquals(3, state.getValue().size(), null);

      accountsRepository.changeAccountsExternally(Set.of());
      assertTrue(state.getValue().isEmpty());
      return Unit.INSTANCE;
    });
  }

  private static final class MockAccount extends Account {
    private final String id;

    MockAccount() {
      this.id = Account.generateId();
    }

    @Override
    public @Nonnull String getId() {
      return id;
    }

    @Override
    public @Nonnull String getName() {
      return "";
    }
  }

  private static final class TestAccountsRepository implements ObservableAccountsRepository<MockAccount> {
    private final MutableStateFlow<Set<MockAccount>> myAccountsFlow = StateFlowKt.MutableStateFlow(Set.of());

    @Override
    public @Nonnull Set<MockAccount> getAccounts() {
      return myAccountsFlow.getValue();
    }

    @Override
    public void setAccounts(@Nonnull Set<MockAccount> accounts) {
      myAccountsFlow.setValue(accounts);
    }

    @Override
    public @Nonnull StateFlow<Set<MockAccount>> getAccountsFlow() {
      return myAccountsFlow;
    }

    void changeAccountsExternally(@Nonnull Set<MockAccount> newAccounts) {
      myAccountsFlow.setValue(newAccounts);
    }
  }

  private static final class TestCredentialsRepository implements CredentialsRepository<MockAccount, String> {
    private final ConcurrentHashMap<MockAccount, String> map = new ConcurrentHashMap<>();

    @Override
    public @Nullable Object persistCredentials(@Nonnull MockAccount account,
                                                @Nullable String credentials,
                                                @Nonnull kotlin.coroutines.Continuation<? super Unit> continuation) {
      if (credentials != null) {
        map.put(account, credentials);
      }
      else {
        map.remove(account);
      }
      return Unit.INSTANCE;
    }

    @Override
    public @Nullable Object retrieveCredentials(@Nonnull MockAccount account,
                                                 @Nonnull kotlin.coroutines.Continuation<? super String> continuation) {
      return map.get(account);
    }

    @Override
    public @Nonnull StateFlow<Boolean> getCanPersistCredentials() {
      return StateFlowKt.MutableStateFlow(false);
    }
  }
}
