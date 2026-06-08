/*
 * Copyright 2013-2016 consulo.io
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
package consulo.ui.layout;

import consulo.ui.Component;
import consulo.ui.PseudoComponent;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.internal.UIInternal;

/**
 * @author VISTALL
 * @since 2016-06-13
 */
public interface TwoComponentSplitLayout extends Layout<LayoutConstraint> {
    static TwoComponentSplitLayout create(SplitLayoutPosition position) {
        return UIInternal.get()._TwoComponentSplitLayout_create(position);
    }

    /**
     * @param percent from 0 to 100
     */
    default TwoComponentSplitLayout withProportion(int percent) {
        setProportion(percent);
        return this;
    }

    /**
     * @param percent from 0 to 100
     */
    void setProportion(int percent);

    @RequiredUIAccess
    default TwoComponentSplitLayout withFirstComponent(PseudoComponent component) {
        return withFirstComponent(component.getComponent());
    }

    @RequiredUIAccess
    default TwoComponentSplitLayout withFirstComponent(Component component) {
        setFirstComponent(component);
        return this;
    }

    @RequiredUIAccess
    void setFirstComponent(Component component);

    @RequiredUIAccess
    default TwoComponentSplitLayout withSecondComponent(PseudoComponent component) {
        return withSecondComponent(component.getComponent());
    }

    @RequiredUIAccess
    default TwoComponentSplitLayout withSecondComponent(Component component) {
        setSecondComponent(component);
        return this;
    }

    @RequiredUIAccess
    void setSecondComponent(Component component);
}
