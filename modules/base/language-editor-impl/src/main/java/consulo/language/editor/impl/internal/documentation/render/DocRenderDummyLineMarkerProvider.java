// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.impl.internal.documentation.render;

import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.editor.gutter.LineMarkerInfo;
import consulo.language.editor.gutter.LineMarkerProviderDescriptor;
import consulo.language.editor.gutter.LineMarkerSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.image.Image;

@ExtensionImpl
public final class DocRenderDummyLineMarkerProvider extends LineMarkerProviderDescriptor {
    private static final DocRenderDummyLineMarkerProvider INSTANCE = new DocRenderDummyLineMarkerProvider();

    @Override
    public Language getLanguage() {
        return Language.ANY;
    }

    @Override
    public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        return null; // this class does not generate line marker info, it exists to add configuration entry in settings
    }

    @Override
    public String getId() {
        return "RenderedDoc";
    }

    @Override
    public LocalizeValue getName() {
        return CodeInsightLocalize.docRenderGutterIconSetting();
    }

    @Override
    public Image getIcon() {
        return PlatformIconGroup.gutterJavadocread();
    }

    static boolean isGutterIconEnabled() {
        return LineMarkerSettings.getInstance().isEnabled(INSTANCE);
    }
}
