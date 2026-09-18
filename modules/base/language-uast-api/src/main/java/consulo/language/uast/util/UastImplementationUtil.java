// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.uast.util;

import consulo.application.Application;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.uast.UElement;
import consulo.language.uast.UastLanguagePlugin;
import consulo.language.uast.internal.ThreadLocalTroubleCollector;
import consulo.language.uast.visitor.UastVisitor;
import consulo.language.util.AttachmentFactoryUtil;
import consulo.logging.Logger;
import consulo.logging.attachment.Attachment;
import consulo.logging.attachment.AttachmentFactory;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Helpers for {@link UastLanguagePlugin} implementations: conversion with error reporting, alternatives
 * and the string/tree helpers (the language-independent parts of {@code implementationUtils.kt} and
 * {@code internalUastUtils.kt}).
 */
public final class UastImplementationUtil {
    /**
     * A lazily created UAST element of a known type, used to pick the right alternative for a requested type.
     */
    public static final class UElementAlternative<U extends UElement> {
        private final Class<U> myUType;
        private final Supplier<@Nullable U> myMake;

        public UElementAlternative(Class<U> uType, Supplier<@Nullable U> make) {
            myUType = uType;
            myMake = make;
        }

        public Class<U> getUType() {
            return myUType;
        }

        public Supplier<@Nullable U> getMake() {
            return myMake;
        }
    }

    private static final ThreadLocal<Boolean> IS_INSIDE_REPORTING = new ThreadLocal<>();

    private static final ThreadLocalTroubleCollector CONVERSION_LOGGER_COLLECTOR = new ThreadLocalTroubleCollector();

    static final ThreadLocalTroubleCollector.Logger CONVERSION_LOGGER = CONVERSION_LOGGER_COLLECTOR.getLogger();

    private UastImplementationUtil() {
    }

    public static final String LINE_SEPARATOR = System.lineSeparator() == null ? "\n" : System.lineSeparator();

    public static String withMargin(String text) {
        return text.lines().map(line -> "    " + line).collect(Collectors.joining(LINE_SEPARATOR));
    }

    public static String times(String text, int n) {
        return text.repeat(n);
    }

    public static String asLogString(List<? extends UElement> elements) {
        return elements.stream().map(element -> withMargin(element.asLogString())).collect(Collectors.joining(LINE_SEPARATOR));
    }

    public static void acceptList(List<? extends UElement> elements, UastVisitor visitor) {
        for (UElement element : elements) {
            element.accept(visitor);
        }
    }

    public static String log(UElement element) {
        return log(element, "");
    }

    public static String log(UElement element, String text) {
        String className = element.getClass().getSimpleName();
        return text.isEmpty() ? className : className + " (" + text + ")";
    }

    @SafeVarargs
    public static <U extends UElement> List<U> accommodate(Class<? extends UElement>[] requiredTypes,
                                                           UElementAlternative<? extends U>... makers) {
        Set<UElementAlternative<? extends U>> matched = new LinkedHashSet<>();
        for (Class<? extends UElement> requiredType : requiredTypes) {
            for (UElementAlternative<? extends U> maker : makers) {
                if (requiredType.isAssignableFrom(maker.getUType())) {
                    matched.add(maker);
                }
            }
        }
        List<U> result = new ArrayList<>();
        for (UElementAlternative<? extends U> maker : matched) {
            U element = maker.getMake().get();
            if (element != null) {
                result.add(element);
            }
        }
        return result;
    }

    public static <U extends UElement> @Nullable U accommodate(Class<? extends UElement> requiredType,
                                                               UElementAlternative<? extends U> a1,
                                                               UElementAlternative<? extends U> a2) {
        if (requiredType.isAssignableFrom(a1.getUType())) {
            return a1.getMake().get();
        }
        else if (requiredType.isAssignableFrom(a2.getUType())) {
            return a2.getMake().get();
        }
        else {
            return null;
        }
    }

    public static <U extends UElement> UElementAlternative<U> alternative(Class<U> uType, Supplier<@Nullable U> make) {
        return new UElementAlternative<>(uType, make);
    }

    private static String safeToString(UElement element) {
        if (!Boolean.TRUE.equals(IS_INSIDE_REPORTING.get())) {
            return withValue(IS_INSIDE_REPORTING, Boolean.TRUE, element::toString);
        }
        return "<recursive `safeToString()` computation " + element.getClass() + ">";
    }

    private static Attachment[] mkAttachments(PsiElement psiElement,
                                              UElement parent,
                                              Class<?> expectedType,
                                              @Nullable Attachment... attachments) {
        AttachmentFactory factory = AttachmentFactory.get();
        List<Attachment> result = new ArrayList<>();
        for (Attachment attachment : attachments) {
            if (attachment != null) {
                result.add(attachment);
            }
        }

        StringBuilder info = new StringBuilder();
        info.append("context: ").append(parent.getClass()).append('\n');
        Boolean valid;
        try {
            valid = psiElement.isValid();
        }
        catch (Throwable e) {
            valid = null;
        }
        info.append("psiElement: ").append(psiElement.getClass()).append(", valid = ").append(valid).append('\n');
        info.append("expectedType: ").append(expectedType).append('\n');
        result.add(factory.create("info.txt", info.toString()));

        String content;
        try {
            String text = psiElement.getText();
            content = text == null ? "<null>" : text;
        }
        catch (Throwable e) {
            content = ExceptionUtil.getThrowableText(e);
        }
        result.add(factory.create("psiElementContent.txt", content));

        String plugins = Application.get().getExtensionList(UastLanguagePlugin.class).stream()
            .map(plugin -> plugin.getClass().toString())
            .collect(Collectors.joining("\n"));
        result.add(factory.create("uast-plugins.list", plugins));

        Attachment fileAttachment;
        try {
            PsiFile containingFile = Objects.requireNonNull(psiElement.getContainingFile());
            VirtualFile virtualFile = Objects.requireNonNull(containingFile.getVirtualFile());
            fileAttachment = AttachmentFactoryUtil.createAttachment(virtualFile);
        }
        catch (Throwable e) {
            fileAttachment = factory.create("containingFile-exception.txt", ExceptionUtil.getThrowableText(e));
        }
        result.add(fileAttachment);

        return result.toArray(Attachment.EMPTY_ARRAY);
    }

    public static <T extends UElement> @Nullable T convertOrReport(PsiElement psiElement, UElement parent, Class<T> expectedType) {
        PsiElement parentSourcePsi = parent.getSourcePsi();
        UastLanguagePlugin plugin = parentSourcePsi != null ? UastLanguagePlugin.byLanguage(parentSourcePsi.getLanguage()) : null;
        if (plugin == null) {
            plugin = UastLanguagePlugin.byLanguage(psiElement.getLanguage());
        }
        if (plugin == null) {
            if (!Boolean.TRUE.equals(IS_INSIDE_REPORTING.get())) {
                Logger.getInstance(parent.getClass())
                    .error("cant get UAST plugin for " + safeToString(parent) + " to convert element " + psiElement,
                        mkAttachments(psiElement, parent, expectedType));
            }
            return null;
        }
        UastLanguagePlugin foundPlugin = plugin;
        Pair<@Nullable T, String> resultAndLog = CONVERSION_LOGGER_COLLECTOR.withCollectingInfo(
            () -> expectedType.cast(foundPlugin.convertElement(psiElement, parent, expectedType))
        );
        T result = resultAndLog.getFirst();
        String log = resultAndLog.getSecond();
        if (result == null && !Boolean.TRUE.equals(IS_INSIDE_REPORTING.get())) {
            Logger.getInstance(parent.getClass())
                .error(
                    "failed to convert element " + psiElement + " (" + psiElement.getClass() + ") in " + safeToString(parent)
                        + ", plugin = " + plugin,
                    mkAttachments(psiElement, parent, expectedType, AttachmentFactory.get().create("conversion-log.txt", log)));
        }
        return result;
    }

    public static <T, R> R withValue(ThreadLocal<T> threadLocal, T value, Supplier<R> block) {
        T old = threadLocal.get();
        if (Objects.equals(old, value)) {
            return block.get();
        }
        try {
            threadLocal.set(value);
            return block.get();
        }
        finally {
            if (old == null) {
                threadLocal.remove();
            }
            else {
                threadLocal.set(old);
            }
        }
    }
}
