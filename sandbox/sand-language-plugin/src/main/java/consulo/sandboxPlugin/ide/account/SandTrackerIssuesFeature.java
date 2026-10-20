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

import consulo.accountManagement.AccountFeature;
import consulo.accountManagement.AccountType;
import consulo.accountManagement.NoScope;
import consulo.annotation.component.ExtensionImpl;
import consulo.localize.LocalizeValue;

import java.util.Set;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
@ExtensionImpl
public class SandTrackerIssuesFeature implements AccountFeature<NoScope> {
    @Override
    public String getId() {
        return "sandTracker.issues";
    }

    @Override
    public Class<? extends AccountType<NoScope>> getAccountType() {
        return SandTrackerAccountType.class;
    }

    @Override
    public LocalizeValue getTitle() {
        return LocalizeValue.localizeTODO("Issues");
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO("Open tasks from Sand Tracker");
    }

    @Override
    public Set<NoScope> getRequiredScopes() {
        return Set.of();
    }
}
