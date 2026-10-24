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
package consulo.review.impl.internal.details;

import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.review.details.CodeReviewDetailsComponentFactory;
import consulo.review.details.CodeReviewDetailsStatusComponentFactory;
import consulo.review.details.model.CodeReviewDetailsTabViewModel;
import consulo.review.details.model.CodeReviewDetailsViewModel;
import consulo.review.details.model.CodeReviewStatusViewModel;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.layout.VerticalLayout;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-18
 */
@Singleton
@ServiceImpl
public class CodeReviewDetailsComponentFactoryImpl implements CodeReviewDetailsComponentFactory {
    private final CodeReviewDetailsStatusComponentFactory myStatusComponentFactory;

    @Inject
    public CodeReviewDetailsComponentFactoryImpl(CodeReviewDetailsStatusComponentFactory statusComponentFactory) {
        myStatusComponentFactory = statusComponentFactory;
    }

    @RequiredUIAccess
    @Override
    public Component create(Disposable parentDisposable,
                            CodeReviewDetailsTabViewModel tabVm,
                            @Nullable @RequiredUIAccess Runnable showTimelineAction) {
        VerticalLayout root = VerticalLayout.create(Space.NONE);

        VerticalLayout titleSection = VerticalLayout.create(Space.LARGE);
        titleSection.add(createTitle(parentDisposable, tabVm.getDetailsVm()));
        titleSection.add(createDescription(parentDisposable, tabVm.getDetailsVm(), showTimelineAction));
        titleSection.paddingBuilder().topSet(Space.X_LARGE).bottomSet(Space.X_LARGE).horizontalSet(Space.X_LARGE).apply();
        root.add(titleSection);

        CodeReviewStatusViewModel statusVm = tabVm.getStatusVm();
        if (statusVm != null) {
            VerticalLayout statuses = VerticalLayout.create(Space.NONE);
            statuses.add(myStatusComponentFactory.createCiComponent(parentDisposable, statusVm));
            statuses.add(myStatusComponentFactory.createConflictsComponent(parentDisposable, statusVm.getHasConflicts()));
            statuses.paddingBuilder().topSet(Space.MEDIUM).bottomSet(Space.LARGE).horizontalSet(Space.X_LARGE).apply();
            root.add(statuses);
        }
        return ScrollableLayout.create(root);
    }

    @RequiredUIAccess
    @Override
    public Component createTitle(Disposable parentDisposable, CodeReviewDetailsViewModel detailsVm) {
        return CodeReviewDetailsTitleComponentFactory.create(parentDisposable, detailsVm);
    }

    @RequiredUIAccess
    @Override
    public Component createDescription(Disposable parentDisposable,
                                       CodeReviewDetailsViewModel detailsVm,
                                       @Nullable @RequiredUIAccess Runnable showTimelineAction) {
        return CodeReviewDetailsDescriptionComponentFactory.create(parentDisposable, detailsVm, showTimelineAction);
    }
}
