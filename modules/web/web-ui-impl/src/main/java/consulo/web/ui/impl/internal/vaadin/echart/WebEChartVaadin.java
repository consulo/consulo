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
package consulo.web.ui.impl.internal.vaadin.echart;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import consulo.web.ui.impl.internal.base.FromVaadinComponentWrapper;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;

/**
 * @author VISTALL
 * @since 2026-10-03
 */
@Tag("consulo-echart")
@NpmPackage(value = "echarts", version = "6.1.0")
@JsModule("./consulo-echart/consulo-echart.ts")
public class WebEChartVaadin extends Component implements FromVaadinComponentWrapper, HasSize {
    private final consulo.ui.Component myOwner;

    public WebEChartVaadin(consulo.ui.Component owner) {
        myOwner = owner;
        setSizeFull();
    }

    public void update(JsonNode model) {
        getElement().callJsFunction("update", model.toString());
    }

    @Override
    public consulo.ui.@Nullable Component toUIComponent() {
        return myOwner;
    }
}
