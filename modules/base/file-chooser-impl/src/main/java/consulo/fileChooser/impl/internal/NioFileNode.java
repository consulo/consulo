// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A tree node backed by a {@link Path}.
 */
public class NioFileNode {
    private final @Nullable Path myPath;

    private final AtomicReference<@Nullable Image> myIconRef = new AtomicReference<>();
    private final AtomicReference<@Nullable String> myNameRef = new AtomicReference<>();
    private final AtomicReference<@Nullable String> myCommentRef = new AtomicReference<>();
    private final AtomicBoolean myValidRef = new AtomicBoolean();
    private final AtomicBoolean myHiddenRef = new AtomicBoolean();
    private final AtomicBoolean mySpecialRef = new AtomicBoolean();
    private final AtomicBoolean mySymlinkRef = new AtomicBoolean();
    private final AtomicBoolean myWritableRef = new AtomicBoolean();

    NioFileNode(@Nullable Path path) {
        myPath = path;
    }

    public @Nullable Path getPath() {
        return myPath;
    }

    public @Nullable Image getIcon() {
        return myIconRef.get();
    }

    boolean updateIcon(@Nullable Image icon) {
        return !Objects.equals(icon, myIconRef.getAndSet(icon));
    }

    public @Nullable String getName() {
        return myNameRef.get();
    }

    boolean updateName(@Nullable String name) {
        return !Objects.equals(name, myNameRef.getAndSet(name));
    }

    public @Nullable String getComment() {
        return myCommentRef.get();
    }

    boolean updateComment(@Nullable String comment) {
        return !Objects.equals(comment, myCommentRef.getAndSet(comment));
    }

    public boolean isValid() {
        return myValidRef.get();
    }

    boolean updateValid(boolean valid) {
        return valid != myValidRef.getAndSet(valid);
    }

    public boolean isHidden() {
        return myHiddenRef.get();
    }

    boolean updateHidden(boolean hidden) {
        return hidden != myHiddenRef.getAndSet(hidden);
    }

    public boolean isSpecial() {
        return mySpecialRef.get();
    }

    boolean updateSpecial(boolean special) {
        return special != mySpecialRef.getAndSet(special);
    }

    public boolean isSymlink() {
        return mySymlinkRef.get();
    }

    boolean updateSymlink(boolean symlink) {
        return symlink != mySymlinkRef.getAndSet(symlink);
    }

    public boolean isWritable() {
        return myWritableRef.get();
    }

    boolean updateWritable(boolean writable) {
        return writable != myWritableRef.getAndSet(writable);
    }
}
