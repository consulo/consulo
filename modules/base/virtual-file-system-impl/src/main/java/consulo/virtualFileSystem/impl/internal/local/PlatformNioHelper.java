// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.virtualFileSystem.impl.internal.local;

import consulo.util.io.NioFiles;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.function.BiPredicate;

public final class PlatformNioHelper {
    private PlatformNioHelper() {
    }

    public static final class Result<V> {
        private final Object value;

        private Result(Object value) {
            this.value = value;
        }

        @SuppressWarnings("unchecked")
        public V get() throws IOException {
            if (value instanceof RuntimeException re) {
                throw re;
            }
            if (value instanceof IOException ioe) {
                throw ioe;
            }
            return (V) value;
        }
    }

    /**
     * The canned exception that {@link Result#get} throws when the attribute read was skipped.
     * See {@link #visitDirectory(Path, Set, boolean, BiPredicate)} with {@code readMissingAttributes=false}.
     */
    public static final class AttributesNotReadException extends IOException {
        private AttributesNotReadException() {
            super("The attribute read was skipped");
        }

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static final Result<BasicFileAttributes> ATTRIBUTES_NOT_READ = new Result<>(new AttributesNotReadException());

    /**
     * A specialized alternative to {@link Files#newDirectoryStream} and {@link Files#walkFileTree}.
     * Only children whose names are in the filter set are passed to consumer.
     * {@code (filter == null)} means 'no filter', i.e., all children must be passed to consumer; {@code filter={}} (an empty set) means 'do nothing'.
     *
     * @see NioFiles#readAttributes
     */
    public static void visitDirectory(
        Path directory,
        @Nullable Set<String> filter,
        BiPredicate<Path, Result<BasicFileAttributes>> consumer
    ) throws IOException, SecurityException {
        visitDirectory(directory, filter, true, consumer);
    }

    /**
     * A variant of {@link #visitDirectory(Path, Set, BiPredicate)} that can skip the attribute read.
     * When {@code readMissingAttributes} is {@code false} and the directory stream did not cache the attributes of an entry,
     * the consumer receives a result that throws {@link AttributesNotReadException}, and no extra read is made for the entry.
     */
    public static void visitDirectory(
        Path directory,
        @Nullable Set<String> filter,
        boolean readMissingAttributes,
        BiPredicate<Path, Result<BasicFileAttributes>> consumer
    ) throws IOException, SecurityException {
        if (filter != null && filter.isEmpty()) {
            return;  // nothing to read
        }

        try (var dirStream = Files.newDirectoryStream(directory)) {
            for (var path : dirStream) {
                if (filter != null && !filter.contains(path.getFileName().toString())) {
                    continue;
                }

                Result<BasicFileAttributes> result;
                if (!readMissingAttributes) {
                    result = ATTRIBUTES_NOT_READ;
                }
                else {
                    try {
                        result = new Result<>(NioFiles.readAttributes(path));
                    }
                    catch (IOException | RuntimeException e) {
                        result = new Result<>(e);
                    }
                }

                if (!consumer.test(path, result)) {
                    break;
                }
            }
        }
    }

    /**
     * Recursively walks the directory tree rooted at {@code root}, driving the given {@link FileVisitor}.
     * Reproduces {@link Files#walkFileTree}'s no-options semantics ({@code NOFOLLOW_LINKS}, unbounded depth).
     */
    public static void walkFileTree(Path root, FileVisitor<Path> visitor) throws IOException {
        Files.walkFileTree(root, visitor);
    }
}
