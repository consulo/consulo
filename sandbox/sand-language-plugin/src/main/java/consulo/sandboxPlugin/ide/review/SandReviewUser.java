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
package consulo.sandboxPlugin.ide.review;

import consulo.review.list.UserPresentation;
import consulo.review.user.CodeReviewUser;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
public record SandReviewUser(String username, String fullName) implements CodeReviewUser, UserPresentation {
    @Override
    public @Nullable String getAvatarUrl() {
        return null;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getFullName() {
        return fullName;
    }

    @Override
    public Image getAvatarIcon() {
        return SandReviewAvatars.getAvatar(this);
    }
}
