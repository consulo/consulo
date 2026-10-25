// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.ui.impl.messageList;

import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.ui.image.Image;
import consulo.ui.messageList.AsyncHtmlImageLoader;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BiConsumer;

final class ImageLoader {
    sealed interface State {
        record NotLoaded() implements State {
        }

        record Loading(@Nullable Dimension dimension) implements State {
        }

        record Loaded(Image image, Dimension dimension) implements State {
        }
    }

    static final State NOT_LOADED = new State.NotLoaded();

    private final @Nullable URL myBaseUrl;
    private final @Nullable String mySrc;
    private final @Nullable AsyncHtmlImageLoader myAsyncLoader;
    private final BiConsumer<State, State> myOnStateChange;

    private @Nullable CompletableFuture<?> myLoadingJob;
    private boolean myCancelled;
    private State myState = NOT_LOADED;

    ImageLoader(@Nullable URL baseUrl,
                @Nullable String src,
                @Nullable AsyncHtmlImageLoader asyncLoader,
                BiConsumer<State, State> onStateChange) {
        myBaseUrl = baseUrl;
        mySrc = src;
        myAsyncLoader = asyncLoader;
        myOnStateChange = onStateChange;
    }

    State getState() {
        return myState;
    }

    private void setState(State state) {
        State old = myState;
        myState = state;
        myOnStateChange.accept(old, state);
    }

    void loadImage() {
        if (myLoadingJob == null) {
            myLoadingJob = requestImage();
        }
    }

    private CompletableFuture<?> requestImage() {
        String src = mySrc;
        if (src == null) {
            return CompletableFuture.completedFuture(null);
        }

        CompletableFuture<? extends @Nullable Image> request;
        try {
            if (src.startsWith("data:image") && src.contains("base64")) {
                request = CompletableFuture.completedFuture(tryCreateBase64Image(src));
            }
            else if (myAsyncLoader != null) {
                request = myAsyncLoader.load(myBaseUrl, src);
            }
            else {
                URL url = myBaseUrl != null ? new URL(myBaseUrl, src) : new URL(src);
                request = CompletableFuture.supplyAsync(() -> {
                    try {
                        return Image.fromUrl(url);
                    }
                    catch (IOException e) {
                        throw new CompletionException(e);
                    }
                }, AppExecutorUtil.getAppExecutorService());
            }
        }
        catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        setState(new State.Loading(null));
        return request.whenComplete((image, error) -> SwingUtilities.invokeLater(() -> {
            if (myCancelled) {
                return;
            }
            if (error != null || image == null) {
                setState(NOT_LOADED);
            }
            else {
                setState(new State.Loaded(image, new Dimension(image.getWidth(), image.getHeight())));
            }
        }));
    }

    void cancel() {
        myCancelled = true;
        CompletableFuture<?> job = myLoadingJob;
        if (job != null) {
            job.cancel(false);
        }
    }

    // example: "data:image/png;base64,ENCODED_IMAGE_HERE"
    private static @Nullable Image tryCreateBase64Image(String src) {
        String[] parts = src.split(",");
        if (parts.length != 2) {
            return null;
        }
        try {
            return Image.fromBytes(Image.ImageType.PNG, Base64.getDecoder().decode(parts[1]));
        }
        catch (IOException | IllegalArgumentException e) {
            return null;
        }
    }
}
