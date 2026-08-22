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
package consulo.ui.layout.event;

import consulo.ui.event.ComponentEvent;
import consulo.ui.event.details.InputDetails;
import consulo.ui.event.details.ProgrammaticInputDetails;
import consulo.ui.layout.TwoComponentSplitLayout;

/**
 * @author VISTALL
 * @since 2026-08-22
 */
public final class SplitProportionChangedEvent extends ComponentEvent<TwoComponentSplitLayout> {
    private final int myProportion;

    public SplitProportionChangedEvent(TwoComponentSplitLayout component, int proportion) {
        this(component, ProgrammaticInputDetails.INSTANCE, proportion);
    }

    public SplitProportionChangedEvent(TwoComponentSplitLayout component, InputDetails inputDetails, int proportion) {
        super(component, inputDetails);
        myProportion = proportion;
    }

    /**
     * @return percent from 0 to 100
     */
    public int getProportion() {
        return myProportion;
    }
}
