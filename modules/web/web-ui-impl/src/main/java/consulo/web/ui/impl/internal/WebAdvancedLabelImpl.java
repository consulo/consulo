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
package consulo.web.ui.impl.internal;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.Component;
import consulo.ui.AdvancedLabel;
import consulo.ui.TextItemPresentation;
import consulo.ui.image.Image;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import consulo.web.ui.impl.internal.base.VaadinComponentDelegate;
import consulo.web.ui.impl.internal.image.WebImageConverter;
import consulo.web.ui.impl.internal.vaadin.AuraUtility;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A label of several runs of text rather than one, each carrying its own colour and style - what a completion item or
 * a project view row is drawn with.
 * <p/>
 * The runs are kept and rebound. Building a presentation makes new spans, but handing those to the browser means the
 * old ones are detached and the new ones attached - a row of a list which changes on every typed character is then a
 * tree of elements over the wire per line, however carefully the list itself pooled its rows. So what travels here is
 * the text and the colour of runs which already exist.
 *
 * @author VISTALL
 */
public class WebAdvancedLabelImpl extends VaadinComponentDelegate<WebAdvancedLabelImpl.Vaadin> implements AdvancedLabel {
    public class Vaadin extends Span implements FromVaadinComponentWrapper {
        @Override
        public consulo.ui.@Nullable Component toUIComponent() {
            return WebAdvancedLabelImpl.this;
        }
    }

    /**
     * The spans the runs are drawn in, in order. Grown when a presentation has more runs than last time and never
     * shrunk - a span with nothing to say is emptied rather than taken out, so the row keeps its shape.
     */
    private final List<Span> myRuns = new ArrayList<>();

    private @Nullable Image myIcon;
    private @Nullable Component myIconComponent;

    @Override
    public Vaadin createVaadinComponent() {
        return new Vaadin();
    }

    @Override
    public AdvancedLabel updatePresentation(Consumer<TextItemPresentation> consumer) {
        WebItemPresentationImpl presentation = new WebItemPresentationImpl();

        consumer.accept(presentation);

        updateIcon(presentation.getIcon());

        List<WebItemPresentationImpl.Fragment> fragments = presentation.getFragments();
        for (int i = 0; i < fragments.size(); i++) {
            WebItemPresentationImpl.Fragment fragment = fragments.get(i);

            Span run = runAt(i);
            run.setText(fragment.text());
            WebItemPresentationImpl.applyAttribute(run, fragment.attribute());
        }

        // the runs the presentation did not use. emptied and not removed, so nothing detaches - a row bound from an
        // item with a tail onto one without says nothing there instead of still saying the old tail
        for (int i = fragments.size(); i < myRuns.size(); i++) {
            Span run = myRuns.get(i);
            run.setText("");
            WebItemPresentationImpl.applyAttribute(run, null);
        }

        return this;
    }

    private Span runAt(int index) {
        while (myRuns.size() <= index) {
            Span run = new Span();
            myRuns.add(run);
            getVaadinComponent().add(run);
        }
        return myRuns.get(index);
    }

    /**
     * The icon changes far less often than the text, so it is only rebuilt when it is a different one.
     */
    private void updateIcon(@Nullable Image icon) {
        if (myIcon == icon) {
            return;
        }
        myIcon = icon;

        if (myIconComponent != null) {
            getVaadinComponent().remove(myIconComponent);
            myIconComponent = null;
        }

        if (icon != null) {
            Component image = WebImageConverter.getImage(icon);
            image.addClassName(AuraUtility.Margin.Right.SMALL);

            myIconComponent = image;
            getVaadinComponent().addComponentAsFirst(image);
        }
    }
}
