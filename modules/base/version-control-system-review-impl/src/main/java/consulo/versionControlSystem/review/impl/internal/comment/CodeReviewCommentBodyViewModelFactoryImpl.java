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
package consulo.versionControlSystem.review.impl.internal.comment;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.project.Project;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModel;
import consulo.versionControlSystem.review.comment.CodeReviewCommentBodyViewModelFactory;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestedChangeSupport;
import consulo.versionControlSystem.review.comment.CodeReviewSuggestionThreadData;
import consulo.versionControlSystem.review.impl.internal.timeline.TimelineDiffComponentFactoryImpl;
import consulo.versionControlSystem.review.timeline.TimelineDiffComponentFactory;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-19
 */
@Singleton
@ServiceImpl
public class CodeReviewCommentBodyViewModelFactoryImpl implements CodeReviewCommentBodyViewModelFactory {
    private final Project myProject;
    private final TimelineDiffComponentFactoryImpl myDiffComponentFactory;

    @Inject
    public CodeReviewCommentBodyViewModelFactoryImpl(Project project, TimelineDiffComponentFactory diffComponentFactory) {
        myProject = project;
        myDiffComponentFactory = (TimelineDiffComponentFactoryImpl) diffComponentFactory;
    }

    @Override
    public CodeReviewCommentBodyViewModel create(Disposable parentDisposable,
                                                 CodeReviewSuggestedChangeSupport support,
                                                 ObservableValue<@Nullable CodeReviewSuggestionThreadData> thread,
                                                 ObservableValue<String> body) {
        return new CodeReviewCommentBodyViewModelImpl(myProject, parentDisposable, support, thread, body, myDiffComponentFactory);
    }
}
