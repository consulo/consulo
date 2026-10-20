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

import consulo.accountManagement.AccountScope;
import consulo.localize.LocalizeValue;

/**
 * @author VISTALL
 * @since 2026-10-20
 */
public enum SandHubScope implements AccountScope {
    REPO("repo", "Full access to repositories"),
    ISSUES("issues", "Read and write issues"),
    GIST("gist", "Create gists"),
    READ_USER("read:user", "Read the user profile");

    private final String myId;
    private final String myDescription;

    SandHubScope(String id, String description) {
        myId = id;
        myDescription = description;
    }

    @Override
    public String getId() {
        return myId;
    }

    @Override
    public LocalizeValue getDescription() {
        return LocalizeValue.localizeTODO(myDescription);
    }
}
