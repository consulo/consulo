/*
 * Copyright 2013-2025 consulo.io
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
package consulo.language.editor.rawHighlight;

import consulo.colorScheme.TextAttributesKey;
import consulo.language.editor.annotation.HighlightSeverity;
import consulo.language.psi.PsiElement;
import consulo.util.xml.serializer.WriteExternalException;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdom.Element;

import java.util.Objects;

public class HighlightInfoTypeImpl implements HighlightInfoType, HighlightInfoType.UpdateOnTypingSuppressible {
    private final HighlightSeverity mySeverity;
    private final TextAttributesKey myAttributesKey;
    private boolean myNeedsUpdateOnTyping;

    //read external only
    public HighlightInfoTypeImpl(@Nonnull Element element) {
        mySeverity = new HighlightSeverity(element);
        myAttributesKey = new TextAttributesKey(element);
    }

    public HighlightInfoTypeImpl(@Nonnull HighlightSeverity severity, TextAttributesKey attributesKey) {
        this(severity, attributesKey, true);
    }

    public HighlightInfoTypeImpl(@Nonnull HighlightSeverity severity, TextAttributesKey attributesKey, boolean needsUpdateOnTyping) {
        mySeverity = severity;
        myAttributesKey = attributesKey;
        myNeedsUpdateOnTyping = needsUpdateOnTyping;
    }

    @Override
    @Nonnull
    public HighlightSeverity getSeverity(@Nullable PsiElement psiElement) {
        return mySeverity;
    }

    @Override
    public TextAttributesKey getAttributesKey() {
        return myAttributesKey;
    }

    @Override
    @SuppressWarnings({"HardCodedStringLiteral"})
    public String toString() {
        return "HighlightInfoTypeImpl[severity=" + mySeverity + ", key=" + myAttributesKey + "]";
    }

    public void writeExternal(Element element) {
        try {
            mySeverity.writeExternal(element);
        }
        catch (WriteExternalException e) {
            throw new RuntimeException(e);
        }
        myAttributesKey.writeExternal(element);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        consulo.language.editor.rawHighlight.HighlightInfoTypeImpl that = (consulo.language.editor.rawHighlight.HighlightInfoTypeImpl) o;

        return Objects.equals(myAttributesKey, that.myAttributesKey)
            && mySeverity.equals(that.mySeverity);
    }

    @Override
    public int hashCode() {
        int result = mySeverity.hashCode();
        return 29 * result + myAttributesKey.hashCode();
    }

    @Override
    public boolean needsUpdateOnTyping() {
        return myNeedsUpdateOnTyping;
    }
}
