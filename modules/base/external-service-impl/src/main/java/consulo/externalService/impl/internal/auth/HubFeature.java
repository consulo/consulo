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
package consulo.externalService.impl.internal.auth;

import consulo.accountManagement.Account;
import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountFeatureAction;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.NoScope;
import consulo.configurable.Settings;
import consulo.dataContext.DataManager;
import consulo.externalService.ExternalService;
import consulo.externalService.localize.ExternalServiceLocalize;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public abstract class HubFeature implements AccountFeature<NoScope> {
    private static final String EXTERNAL_SERVICES_CONFIGURABLE = "externalServices";

    private final ExternalService myService;

    protected HubFeature(ExternalService service) {
        myService = service;
    }

    public ExternalService getService() {
        return myService;
    }

    @Override
    public String getId() {
        return "consuloHub." + myService.name();
    }

    @Override
    public Class<? extends AccountType<NoScope>> getAccountType() {
        return HubAccountType.class;
    }

    @Override
    public LocalizeValue getTitle() {
        return myService.getName();
    }

    @Override
    public Set<NoScope> getRequiredScopes() {
        return Set.of();
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    public List<AccountFeatureAction> getActions() {
        return List.of(new AccountFeatureAction() {
            @Override
            public LocalizeValue getText() {
                return ExternalServiceLocalize.hubFeatureConfigure();
            }

            @RequiredUIAccess
            @Override
            public void perform(Account account, @Nullable Project project, Component source) {
                Settings settings = DataManager.getInstance().getDataContext(source).getData(Settings.KEY);
                if (settings != null) {
                    settings.select(EXTERNAL_SERVICES_CONFIGURABLE);
                }
            }
        });
    }
}
