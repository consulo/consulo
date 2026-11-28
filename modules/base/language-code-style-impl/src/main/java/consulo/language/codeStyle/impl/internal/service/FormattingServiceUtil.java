// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
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
package consulo.language.codeStyle.impl.internal.service;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.document.util.TextRange;
import consulo.language.codeStyle.FormatTextRanges;
import consulo.language.codeStyle.service.FormattingService;
import consulo.language.codeStyle.service.FormattingService.Feature;
import consulo.language.codeStyle.service.FormattingServiceSuppressor;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.util.collection.ContainerUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public final class FormattingServiceUtil {
    private static final Logger LOG = Logger.getInstance(FormattingServiceUtil.class);

    private FormattingServiceUtil() {
    }

    @RequiredReadAction
    public static FormattingService findService(PsiFile file, boolean isExplicit, boolean isCompleteFile) {
        ArrayList<Feature> features = new ArrayList<>();
        if (!isExplicit) {
            features.add(Feature.AD_HOC_FORMATTING);
        }
        if (!isCompleteFile) {
            features.add(Feature.FORMAT_FRAGMENTS);
        }

        FormattingService formattingService = ContainerUtil.find(
            Application.get().getExtensionList(FormattingService.class),
            s -> (isExplicit || s.getFeatures().contains(Feature.AD_HOC_FORMATTING)) &&
                (isCompleteFile || s.getFeatures().contains(Feature.FORMAT_FRAGMENTS)) &&
                !isSuppressed(file, s) &&
                s.canFormat(file, features.toArray(new Feature[0]))
        );
        LOG.assertTrue(
            formattingService != null,
            "At least 1 formatting service which can handle PsiFile " + file.getName() + " should be registered."
        );
        return formattingService;
    }

    public static @Nullable FormattingService findService(Class<? extends FormattingService> serviceClass) {
        return ContainerUtil.find(Application.get().getExtensionList(FormattingService.class), s -> s.getClass().equals(serviceClass));
    }

    @RequiredReadAction
    private static boolean isSuppressed(PsiFile file, FormattingService service) {
        for (FormattingServiceSuppressor suppressor : Application.get().getExtensionList(FormattingServiceSuppressor.class)) {
            if (suppressor.isSuppressed(file, service)) {
                return true;
            }
        }
        return false;
    }

    private static List<FormattingService> getChainedServices(FormattingService formattingService) {
        List<FormattingService> serviceList = new ArrayList<>();
        FormattingService currService = formattingService;
        while (currService != null) {
            if (serviceList.contains(currService)) {
                break;
            }
            serviceList.add(0, currService);
            Class<? extends FormattingService> serviceClass = currService.runAfter();
            currService = serviceClass != null ? findService(serviceClass) : null;
        }
        return serviceList;
    }

    @RequiredReadAction
    public static PsiElement formatElement(PsiElement element, boolean canChangeWhiteSpaceOnly) {
        PsiFile file = element.getContainingFile();
        PsiElement contextElement = element;
        FormattingService mainService = findService(file, true, element.getTextRange().equals(file.getTextRange()));
        for (FormattingService service : getChainedServices(mainService)) {
            contextElement = service.formatElement(contextElement, canChangeWhiteSpaceOnly);
        }
        return contextElement;
    }

    @RequiredReadAction
    public static PsiElement formatElement(PsiElement element, TextRange range, boolean canChangeWhiteSpacesOnly) {
        PsiFile file = element.getContainingFile();
        boolean isFullRange = range.equals(file.getTextRange());
        PsiElement contextElement = element;
        FormattingService mainService = findService(element.getContainingFile(), true, isFullRange);
        if (isFullRange) {
            for (FormattingService service : getChainedServices(mainService)) {
                contextElement = service.formatElement(contextElement, range, canChangeWhiteSpacesOnly);
            }
            return contextElement;
        }
        else {
            return mainService.formatElement(element, range, canChangeWhiteSpacesOnly);
        }
    }

    @RequiredReadAction
    public static void formatRanges(PsiFile file, FormatTextRanges ranges, boolean canChangeWhiteSpaceOnly, boolean isFullRange) {
        FormattingService mainService = findService(file, true, isFullRange);
        if (isFullRange) {
            for (FormattingService service : getChainedServices(mainService)) {
                service.formatRanges(file, ranges, canChangeWhiteSpaceOnly, false);
            }
        }
        else {
            // Range formatting can use only one service since the ranges become invalid after reformat.
            mainService.formatRanges(file, ranges, canChangeWhiteSpaceOnly, false);
        }
    }
}
