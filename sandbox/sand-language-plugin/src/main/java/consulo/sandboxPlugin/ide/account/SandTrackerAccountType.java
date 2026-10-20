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
package consulo.sandboxPlugin.ide.account;

import consulo.accountManagement.AccountAuthorizeConfig;
import consulo.accountManagement.AccountConnection;
import consulo.accountManagement.AccountDetails;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.NoScope;
import consulo.accountManagement.authorize.ApiKeyAuthorizeConfig;
import consulo.accountManagement.authorize.BasicAuthorizeConfig;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;
import consulo.task.icon.TaskIconGroup;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class SandTrackerAccountType implements AccountType<NoScope> {
    @Override
    public String getId() {
        return "sandTracker";
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Sand Tracker");
    }

    @Override
    public Image getIcon() {
        return TaskIconGroup.bug();
    }

    @Override
    public Class<NoScope> getScopeClass() {
        return NoScope.class;
    }

    @Override
    public String getDefaultServerUrl() {
        return "https://tracker.sand.example";
    }

    @Override
    public boolean isServerEditable() {
        return true;
    }

    @Override
    public List<? extends AccountAuthorizeConfig> getAuthorizeConfigs() {
        return List.of(
            new ApiKeyAuthorizeConfig() {
                @Override
                public boolean isDefault() {
                    return true;
                }

                @Override
                public @Nullable String getQueryParameterName() {
                    return "key";
                }
            },
            new BasicAuthorizeConfig() {
            }
        );
    }

    @Override
    public CompletableFuture<AccountDetails> loadDetails(AccountConnection connection) {
        return CompletableFuture.supplyAsync(() -> {
            String userName = connection.getUserName();
            if (userName != null) {
                return new AccountDetails(userName, userName, null);
            }
            String key = SandAccountRequest.of(connection).getParameters().get("key");
            if (key != null && key.contains("fail")) {
                throw new IllegalStateException("403 Forbidden - invalid API key");
            }
            return new AccountDetails("API key " + (key == null || key.length() < 4 ? "" : key.substring(0, 4) + "…"), null, null);
        }, CompletableFuture.delayedExecutor(400, TimeUnit.MILLISECONDS));
    }
}
