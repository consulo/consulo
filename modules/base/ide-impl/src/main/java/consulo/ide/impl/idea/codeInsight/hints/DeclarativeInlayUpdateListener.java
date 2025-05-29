// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.TopicAPI;
import consulo.annotation.component.TopicBroadcastDirection;
import consulo.codeEditor.Inlay;

import java.util.EventListener;
import java.util.List;

@TopicAPI(value = ComponentScope.PROJECT, direction = TopicBroadcastDirection.NONE)
public interface DeclarativeInlayUpdateListener extends EventListener {
    Class<DeclarativeInlayUpdateListener> TOPIC = DeclarativeInlayUpdateListener.class;

    /**
     * Both {@code oldModel} and {@code newModel} are the same as the result
     * of calling {@code inlay.getRenderer().toInlayData(false)} before and
     * after the model update respectively.
     */
    void afterModelUpdate(Inlay<? extends DeclarativeInlayRendererBase<?>> inlay,
                          List<InlayData> oldModel,
                          List<InlayData> newModel);
}
