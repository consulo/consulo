// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.review.editor.CodeReviewInlayModel;
import consulo.util.concurrent.coroutine.ObservableValue;
import consulo.versionControlSystem.review.diff.DiffEditorLineMapper;
import consulo.versionControlSystem.review.diff.DiffLineLocation;
import consulo.versionControlSystem.review.diff.DiffMapped;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

final class DiffMappedInlayModel<VM extends DiffMapped> implements CodeReviewInlayModel {
    private final VM myVm;
    private final Object myKey;
    private final ObservableValue<@Nullable Integer> myLine;

    DiffMappedInlayModel(VM vm, Object key, DiffEditorLineMapper mapper) {
        myVm = vm;
        myKey = key;
        myLine = new ObservableValue<>() {
            @Override
            public @Nullable Integer get() {
                return map(vm.getLocation().get());
            }

            @Override
            public Runnable addListener(Consumer<? super @Nullable Integer> listener) {
                return vm.getLocation().addListener(location -> listener.accept(map(location)));
            }

            private @Nullable Integer map(@Nullable DiffLineLocation location) {
                return location == null ? null : mapper.locationToLine(location);
            }
        };
    }

    VM getVm() {
        return myVm;
    }

    @Override
    public Object getKey() {
        return myKey;
    }

    @Override
    public ObservableValue<@Nullable Integer> getLine() {
        return myLine;
    }

    @Override
    public ObservableValue<Boolean> isVisible() {
        return myVm.isVisible();
    }
}
