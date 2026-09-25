/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.language.inject.impl.internal;

import consulo.annotation.access.RequiredReadAction;
import consulo.document.util.ProperTextRange;
import consulo.document.util.Segment;
import consulo.document.util.TextRange;
import consulo.language.impl.internal.psi.pointer.SmartPointerManagerImpl;
import consulo.language.psi.PsiLanguageInjectionHost;
import consulo.language.psi.SmartPointerManager;
import consulo.language.psi.SmartPsiElementPointer;
import consulo.language.psi.SmartPsiFileRange;

import org.jspecify.annotations.Nullable;

class ShredImpl implements PsiLanguageInjectionHost.Shred {
    private final SmartPsiFileRange relevantRangeInHost;
    private final SmartPsiElementPointer<PsiLanguageInjectionHost> hostElementPointer;
    private final TextRange rangeInDecodedPSI;
    private final String prefix;
    private final String suffix;
    private final boolean usePsiRange;
    private final boolean isOneLine;

    @RequiredReadAction
    ShredImpl(
        SmartPsiFileRange relevantRangeInHost,
        SmartPsiElementPointer<PsiLanguageInjectionHost> hostElementPointer,
        String prefix,
        String suffix,
        TextRange rangeInDecodedPSI,
        boolean usePsiRange,
        boolean isOneLine
    ) {
        this.hostElementPointer = hostElementPointer;
        this.relevantRangeInHost = relevantRangeInHost;
        this.prefix = prefix;
        this.suffix = suffix;
        this.rangeInDecodedPSI = rangeInDecodedPSI;
        this.usePsiRange = usePsiRange;
        this.isOneLine = isOneLine;

        assertValid();
    }

    @RequiredReadAction
    private void assertValid() {
        Segment hostRange = relevantRangeInHost.getPsiRange();
        assert hostRange != null : "invalid host range: " + relevantRangeInHost;

        PsiLanguageInjectionHost host = hostElementPointer.getElement();
        assert host != null && host.isValid() : "no host: " + hostElementPointer;
    }

    @RequiredReadAction
    ShredImpl withPsiRange() {
        return new ShredImpl(relevantRangeInHost, hostElementPointer, prefix, suffix, rangeInDecodedPSI, true, isOneLine);
    }

    @RequiredReadAction
    ShredImpl withRange(TextRange rangeInDecodedPSI, TextRange rangeInHostElementPSI, PsiLanguageInjectionHost newHost) {
        SmartPsiFileRange rangeMarker = relevantRangeInHost;
        Segment oldRangeInHostElementPSI = calcRangeInsideHostElement(false);
        SmartPointerManagerImpl pointerManager = (SmartPointerManagerImpl) SmartPointerManager.getInstance(rangeMarker.getProject());
        SmartPsiElementPointer<PsiLanguageInjectionHost> newHostPointer = pointerManager.createSmartPsiElementPointer(
            newHost,
            newHost.getContainingFile(),
            true
        );

        if (!rangeInHostElementPSI.equals(TextRange.create(oldRangeInHostElementPSI))) {
            Segment hostElementRange = newHostPointer.getRange();
            rangeMarker = pointerManager.createSmartPsiFileRangePointer(
                rangeMarker.getContainingFile(),
                rangeInHostElementPSI.shiftRight(hostElementRange.getStartOffset()),
                true
            );
        }
        return new ShredImpl(rangeMarker, newHostPointer, prefix, suffix, rangeInDecodedPSI, usePsiRange, isOneLine);
    }

    SmartPsiElementPointer<PsiLanguageInjectionHost> getSmartPointer() {
        return hostElementPointer;
    }

    /**
     * @return returns null when the host document marker is invalid
     */
    @Override
    public @Nullable Segment getHostRangeMarker() {
        return usePsiRange ? relevantRangeInHost.getPsiRange() : relevantRangeInHost.getRange();
    }

    @Override
    @RequiredReadAction
    public TextRange getRangeInsideHost() {
        return calcRangeInsideHostElement(true);
    }

    @RequiredReadAction
    private TextRange calcRangeInsideHostElement(boolean usePsiRange) {
        PsiLanguageInjectionHost host = getHost();
        Segment psiRange = usePsiRange ? relevantRangeInHost.getPsiRange() : relevantRangeInHost.getRange();
        TextRange textRange = psiRange == null ? null : TextRange.create(psiRange);
        if (host == null) {
            if (textRange != null) {
                return textRange;
            }
            Segment fromSP = usePsiRange ? hostElementPointer.getPsiRange() : hostElementPointer.getRange();
            if (fromSP != null) {
                return TextRange.create(fromSP);
            }
            return new TextRange(0, 0);
        }
        TextRange hostTextRange = host.getTextRange();
        textRange = textRange == null ? null : textRange.intersection(hostTextRange);
        if (textRange == null) {
            return new ProperTextRange(0, hostTextRange.getLength());
        }

        return textRange.shiftLeft(hostTextRange.getStartOffset());
    }

    @Override
    @RequiredReadAction
    @SuppressWarnings("HardCodedStringLiteral")
    public String toString() {
        PsiLanguageInjectionHost host = getHost();
        Segment hostRange = getHostRangeMarker();
        return "Shred " + (host == null ? null : host.getTextRange()) + ": " + host +
            " In host range: " + (hostRange != null ? "(" + hostRange.getStartOffset() + "," + hostRange.getEndOffset() + ");" : "invalid;") +
            " PSI range: " + rangeInDecodedPSI;
    }

    @Override
    @RequiredReadAction
    public boolean isValid() {
        return getHostRangeMarker() != null && getHost() != null;
    }

    @Override
    @RequiredReadAction
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PsiLanguageInjectionHost.Shred that = (PsiLanguageInjectionHost.Shred) o;

        PsiLanguageInjectionHost host = getHost();
        Segment hostRangeMarker = getHostRangeMarker();
        Segment hostRangeMarker2 = that.getHostRangeMarker();
        return host != null
            && host.equals(that.getHost())
            && prefix.equals(that.getPrefix())
            && suffix.equals(that.getSuffix())
            && rangeInDecodedPSI.equals(that.getRange())
            && hostRangeMarker != null
            && hostRangeMarker2 != null
            && TextRange.areSegmentsEqual(hostRangeMarker, hostRangeMarker2);
    }

    @Override
    public int hashCode() {
        return rangeInDecodedPSI.hashCode();
    }

    @Override
    public void dispose() {
    }

    @Override
    @RequiredReadAction
    public @Nullable PsiLanguageInjectionHost getHost() {
        return hostElementPointer.getElement();
    }

    @Override
    public TextRange getRange() {
        return rangeInDecodedPSI;
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String getSuffix() {
        return suffix;
    }

    boolean isOneLine() {
        return isOneLine;
    }
}
