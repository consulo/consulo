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
package consulo.review.reactions;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.Application;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.util.concurrent.coroutine.ObservableValue;

import java.util.function.Consumer;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewReactionComponentFactory {
    static CodeReviewReactionComponentFactory getInstance() {
        return Application.get().getInstance(CodeReviewReactionComponentFactory.class);
    }

    @RequiredUIAccess
    Component createReactionButton(Disposable parentDisposable,
                                   ObservableValue<CodeReviewReactionPillPresentation> presentation,
                                   Runnable toggle);

    @RequiredUIAccess
    Component createNewReactionButton(Consumer<Component> showPicker);

    @RequiredUIAccess
    Component createPickReactionButton(Image emojiIcon, Runnable pick);
}
