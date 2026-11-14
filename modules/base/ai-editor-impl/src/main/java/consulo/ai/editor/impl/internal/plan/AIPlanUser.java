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
package consulo.ai.editor.impl.internal.plan;

import consulo.ai.editor.localize.AIEditorLocalize;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.review.list.UserPresentation;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

/**
 * The author of the comments on a plan - the user, writing to the agent.
 *
 * @author VISTALL
 * @since 2026-11-14
 */
final class AIPlanUser implements UserPresentation {
    static final AIPlanUser INSTANCE = new AIPlanUser();

    private AIPlanUser() {
    }

    @Override
    public String getUsername() {
        return AIEditorLocalize.labelPlanReviewYou().get();
    }

    @Override
    public @Nullable String getFullName() {
        return null;
    }

    @Override
    public Image getAvatarIcon() {
        return PlatformIconGroup.actionsLoginavatar();
    }
}
