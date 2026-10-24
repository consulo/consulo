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
package consulo.sandboxPlugin.ui.tab;

import consulo.annotation.component.ExtensionImpl;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.review.reactions.CodeReviewReactionComponentFactory;
import consulo.review.reactions.CodeReviewReactionPillPresentation;
import consulo.review.reactions.CodeReviewReactionsUIUtil;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.tester.UITesterTab;
import consulo.ui.image.Image;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.Space;
import consulo.ui.layout.VerticalLayout;
import consulo.util.concurrent.coroutine.MutableObservableValue;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;

/**
 * @author VISTALL
 * @since 2026-10-24
 */
@ExtensionImpl(id = "reactions", order = "after review")
public class ReactionsUITesterTab implements UITesterTab {
    private record SandReaction(String emoji, LocalizeValue reactionName, List<LocalizeValue> reactors) implements CodeReviewReactionPillPresentation {
        @Override
        public LocalizeValue getReactionName() {
            return reactionName;
        }

        @Override
        public List<LocalizeValue> getReactors() {
            return reactors;
        }

        @Override
        public boolean isOwnReaction() {
            return reactors.contains(YOU);
        }

        @Override
        public Image getIcon(int size) {
            return CodeReviewReactionsUIUtil.createUnicodeEmojiIcon(emoji, size);
        }

        SandReaction toggled() {
            List<LocalizeValue> newReactors = new ArrayList<>(reactors);
            if (!newReactors.remove(YOU)) {
                newReactors.add(YOU);
            }
            return new SandReaction(emoji, reactionName, List.copyOf(newReactors));
        }
    }

    private static final LocalizeValue YOU = LocalizeValue.of("You");

    private final CodeReviewReactionComponentFactory myReactionComponentFactory;

    @Inject
    public ReactionsUITesterTab(CodeReviewReactionComponentFactory reactionComponentFactory) {
        myReactionComponentFactory = reactionComponentFactory;
    }

    @Override
    public LocalizeValue getName() {
        return LocalizeValue.localizeTODO("Reactions");
    }

    @RequiredUIAccess
    @Override
    public Component createComponent(Disposable uiDisposable) {
        HorizontalLayout row = HorizontalLayout.create(Space.MEDIUM);
        addReaction(row, uiDisposable, new SandReaction("👍", LocalizeValue.of("+1"),
            List.of(LocalizeValue.of("alice"), YOU)));
        addReaction(row, uiDisposable, new SandReaction("🎉", LocalizeValue.of("hooray"),
            List.of(LocalizeValue.of("bob"), LocalizeValue.of("carol"), LocalizeValue.of("dave"), LocalizeValue.of("erin"))));
        addReaction(row, uiDisposable, new SandReaction("❤", LocalizeValue.of("heart"), List.of(LocalizeValue.of("frank"))));
        row.add(myReactionComponentFactory.createNewReactionButton(anchor ->
            addReaction(row, uiDisposable, new SandReaction("🚀", LocalizeValue.of("rocket"), List.of(YOU)))));

        HorizontalLayout picker = HorizontalLayout.create(Space.MEDIUM);
        for (String emoji : List.of("👍", "👎", "😄", "🎉", "😕", "❤", "🚀", "👀")) {
            picker.add(myReactionComponentFactory.createPickReactionButton(
                CodeReviewReactionsUIUtil.createUnicodeEmojiIcon(emoji, CodeReviewReactionsUIUtil.ICON_SIZE), () -> {
                }));
        }

        VerticalLayout root = VerticalLayout.create();
        root.add(row);
        root.add(picker);
        return root;
    }

    @RequiredUIAccess
    private void addReaction(HorizontalLayout row, Disposable uiDisposable, SandReaction reaction) {
        MutableObservableValue<CodeReviewReactionPillPresentation> state = MutableObservableValue.of(reaction);
        row.add(myReactionComponentFactory.createReactionButton(uiDisposable, state,
            () -> state.set(((SandReaction) state.get()).toggled())));
    }
}
