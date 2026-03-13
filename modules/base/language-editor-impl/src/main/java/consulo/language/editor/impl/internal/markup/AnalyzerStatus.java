/*
 * Copyright 2013-2020 consulo.io
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
package consulo.language.editor.impl.internal.markup;

import consulo.application.util.NotNullLazyValue;
import consulo.codeEditor.internal.EditorAnalyzeStatus;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Container containing all necessary information for rendering TrafficLightRenderer.
 * Instance is created each time <code>ErrorStripeRenderer.getStatus</code> is called.
 */
public class AnalyzerStatus implements EditorAnalyzeStatus {
    public static final NotNullLazyValue<AnalyzerStatus> DEFAULT = NotNullLazyValue.createValue(() -> new AnalyzerStatus(Image.empty(0), null, null, DummyUIController.INSTANCE));

    public static boolean equals(@Nullable AnalyzerStatus a, @Nullable AnalyzerStatus b) {
        if (a == null && b == null) {
            return true;
        }

        if (a == null || b == null) {
            return false;
        }

        return Objects.equals(a.getIcon(), b.getIcon()) &&
            Objects.equals(a.expandedStatus, b.expandedStatus) &&
            Objects.equals(a.getTitle(), b.getTitle()) &&
            Objects.equals(a.getDetails(), b.getDetails()) &&
            a.isShowNavigation() == b.isShowNavigation() &&
            Objects.equals(a.passes, b.passes);
    }

    private final Image myIcon;
    private final String myTitle;
    private final String myDetails;

    private List<StatusItem> expandedStatus = Collections.emptyList();
    private List<PassWrapper> passes = Collections.emptyList();

    private boolean textStatus;

    private AnalyzingType analyzingType = AnalyzingType.COMPLETE;

    private boolean myShowNavigation;

    private UIController myControllerValue;

    public AnalyzerStatus(Image icon, String title, String details, UIController uiController) {
        myIcon = icon;
        myTitle = title;
        myDetails = details;

        myControllerValue = uiController;
    }

    @Nonnull
    public UIController getController() {
        return myControllerValue;
    }

    public List<StatusItem> getExpandedStatus() {
        return expandedStatus;
    }

    public List<PassWrapper> getPasses() {
        return passes;
    }

    public boolean isTextStatus() {
        return textStatus;
    }

    public AnalyzingType getAnalyzingType() {
        return analyzingType;
    }

    public boolean isShowNavigation() {
        return myShowNavigation;
    }

    public AnalyzerStatus withNavigation() {
        myShowNavigation = true;
        return this;
    }

    public AnalyzerStatus withExpandedStatus(List<StatusItem> status) {
        expandedStatus = status;
        return this;
    }

    public AnalyzerStatus withPasses(List<PassWrapper> passes) {
        this.passes = passes;
        return this;
    }

    public AnalyzerStatus withAnalyzingType(AnalyzingType type) {
        analyzingType = type;
        return this;
    }

    public AnalyzerStatus withTextStatus(String status) {
        expandedStatus = Collections.singletonList(new StatusItem(status));
        textStatus = true;
        return this;
    }

    @Override
    public Image getIcon() {
        return myIcon;
    }

    public String getTitle() {
        return myTitle;
    }

    public String getDetails() {
        return myDetails;
    }
}
