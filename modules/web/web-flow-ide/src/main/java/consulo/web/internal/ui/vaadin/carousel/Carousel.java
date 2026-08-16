/*-
 * #%L
 * Carousel Addon
 * %%
 * Copyright (C) 2018 - 2024 Flowing Code
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package consulo.web.internal.ui.vaadin.carousel;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.HasTheme;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.shared.Registration;

/**
 * A component that displays a slideshow of slides. By default the slides will be displayed with
 * navigation buttons, autoplay and swipe enabled. You can configure the slideshow by adding custom
 * parameters, such as duration of transition, start position, maximum height and disabling swipe.
 * <p>
 * Vendored from <a href="https://github.com/FlowingCode/CarouselAddon">FlowingCode CarouselAddon</a>.
 */
@SuppressWarnings("serial")
@Tag("fc-l2t-paper-slider")
@NpmPackage(value = "@polymer/iron-a11y-keys-behavior", version = "3.0.1")
@JsModule("./paper-slider/fc-l2t-paper-slider.js")
public class Carousel extends Component implements HasSize, HasTheme {

    private static final String HIDE_NAV = "hideNav";
    private static final String DISABLE_SWIPE = "disableSwipe";
    private static final String POSITION = "position";
    private static final String SLIDE_DURATION = "slideDuration";
    private static final String AUTO_PROGRESS = "autoProgress";
    private static final int DEFAULT_SLIDE_DURATION = 2;

    private Slide[] slides;

    public Carousel(Slide... paperSlides) {
        setSlides(paperSlides);
        initProperties();
    }

    private void updateSlides(Slide... paperSlides) {
        for (Slide slide : paperSlides) {
            getElement().appendChild(slide.getElement());
        }
    }

    private void initProperties() {
        setAutoProgress(false);
        setSlideDuration(DEFAULT_SLIDE_DURATION);
        setStartPosition(0);
        setDisableSwipe(false);
        setHideNavigation(false);
    }

    // PROPERTIES
    public Slide[] getSlides() {
        return slides;
    }

    public void setSlides(Slide[] slides) {
        this.slides = slides;
        updateSlides(slides);
    }

    public boolean isAutoProgress() {
        return getElement().getProperty(AUTO_PROGRESS, false);
    }

    public void setAutoProgress(boolean autoProgress) {
        getElement().setProperty(AUTO_PROGRESS, autoProgress);
    }

    public int getSlideDuration() {
        return getElement().getProperty(SLIDE_DURATION, 0);
    }

    public void setSlideDuration(int slideDuration) {
        getElement().setProperty(SLIDE_DURATION, slideDuration);
    }

    public int getStartPosition() {
        return getElement().getProperty(POSITION, 0);
    }

    public void setStartPosition(int startPosition) {
        getElement().setProperty(POSITION, startPosition);
    }

    public boolean isDisableSwipe() {
        return getElement().getProperty(DISABLE_SWIPE, false);
    }

    public void setDisableSwipe(boolean disableSwipe) {
        getElement().setProperty(DISABLE_SWIPE, disableSwipe);
    }

    public boolean isHideNavigation() {
        return getElement().getProperty(HIDE_NAV, false);
    }

    public void setHideNavigation(boolean hideNavigation) {
        getElement().setProperty(HIDE_NAV, hideNavigation);
    }

    // FLUENT API
    public Carousel withAutoProgress() {
        setAutoProgress(true);
        return this;
    }

    public Carousel withoutSwipe() {
        setDisableSwipe(true);
        return this;
    }

    public Carousel withoutNavigation() {
        setHideNavigation(true);
        return this;
    }

    public Carousel withSlideDuration(int slideDuration) {
        setSlideDuration(slideDuration);
        return this;
    }

    public Carousel withStartPosition(int startPosition) {
        setStartPosition(startPosition);
        return this;
    }

    // SIZING
    @Override
    public void setHeight(String height) {
        HasSize.super.setHeight(height);
        getElement().getStyle().set("--paper-slide-height", height);
    }

    @Override
    public String getHeight() {
        return getElement().getStyle().get("--paper-slide-height");
    }

    // METHODS
    /** Move to the next slide */
    public void moveNext() {
        getElement().callJsFunction("moveNext");
    }

    /** Move to the previous slide */
    public void movePrev() {
        getElement().callJsFunction("movePrev");
    }

    /**
     * Move to a specific slide
     *
     * @param slide
     */
    public void movePos(int slide) {
        getElement().callJsFunction("movePos", "" + slide);
    }

    public void setTotalSlides(int totalSlides) {
        getElement().setProperty("totalSlides", totalSlides);
    }

    // EVENTS
    @DomEvent("position-changed")
    public static class SlideChangeEvent extends ComponentEvent<Carousel> {
        private String position;

        public SlideChangeEvent(
            Carousel source, boolean fromClient, @EventData("event.detail.value") String position) {
            super(source, fromClient);
            this.position = position;
        }

        public String getPosition() {
            return position;
        }
    }

    public Registration addChangeListener(ComponentEventListener<SlideChangeEvent> listener) {
        return addListener(SlideChangeEvent.class, listener);
    }
}
