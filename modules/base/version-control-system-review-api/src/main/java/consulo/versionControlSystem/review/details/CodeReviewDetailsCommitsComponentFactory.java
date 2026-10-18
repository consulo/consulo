// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.versionControlSystem.review.details;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.disposer.Disposable;
import consulo.review.details.CommitPresentation;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.versionControlSystem.review.details.model.CodeReviewChangesViewModel;

import java.util.function.Function;

/**
 * The commits row of review details: the title, the commit chooser - every commit or one of them - and the buttons
 * stepping to the next and previous commit.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeReviewDetailsCommitsComponentFactory {
    @RequiredUIAccess
    <T> Component create(Disposable parentDisposable,
                         CodeReviewChangesViewModel<T> changesVm,
                         Function<T, CommitPresentation> commitPresentation);
}
