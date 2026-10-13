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
package consulo.it.platform;

import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.content.platform.PlatformTarget;
import consulo.content.platform.PlatformTargetManager;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessSdkType;
import consulo.platform.Platform;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The machine run configurations and SDKs live on: the local target, and how an SDK keeps and resolves its target.
 *
 * @author VISTALL
 * @since 2026-10-13
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class PlatformTargetTest {
    @Test
    public void localTargetComesFirst(Application application) {
        PlatformTargetManager manager = application.getInstance(PlatformTargetManager.class);

        List<PlatformTarget> targets = manager.getTargets();
        assertThat(targets).isNotEmpty();
        assertThat(targets.getFirst()).isSameAs(manager.getLocalTarget());
        assertThat(manager.findTarget(null)).isSameAs(manager.getLocalTarget());
        assertThat(manager.findTarget(manager.getLocalTarget().getId())).isSameAs(manager.getLocalTarget());
        assertThat(manager.findTarget("no-such-target")).isNull();
        assertThat(manager.getLocalTarget().getPlatform()).isSameAs(Platform.current());
    }

    @Test
    public void localSdkUsesTheLocalFileSystem() {
        SdkImpl sdk = (SdkImpl) SdkTable.getInstance().createSdk(Path.of("/opt/headless-sdk"), "local-sdk", new HeadlessSdkType());

        assertThat(sdk.getPlatformTargetId()).isNull();
        assertThat(sdk.getPlatform()).isSameAs(Platform.current());
        assertThat(sdk.getHomeNioPath().getFileSystem()).isSameAs(FileSystems.getDefault());
    }

    @Test
    public void sdkKeepsItsTargetThroughModificatorAndXml() {
        SdkImpl sdk = (SdkImpl) SdkTable.getInstance().createSdk(Path.of("/opt/remote-sdk"), "remote-sdk", new HeadlessSdkType());

        SdkModificator modificator = sdk.getSdkModificator();
        modificator.setPlatformTargetId("remote-server:test:gone");
        modificator.commitChanges();
        assertThat(sdk.getPlatformTargetId()).isEqualTo("remote-server:test:gone");

        Element element = new Element("sdk");
        sdk.writeExternal(element);
        assertThat(element.getChild("platformTarget")).isNotNull();

        SdkImpl restored = (SdkImpl) SdkTable.getInstance().createSdk(Path.of("/opt/restored"), "restored", new HeadlessSdkType());
        restored.readExternal(element);
        assertThat(restored.getPlatformTargetId()).isEqualTo("remote-server:test:gone");
    }

    @Test
    public void unreachableSdkNeverFallsBackToLocal() {
        SdkImpl sdk = (SdkImpl) SdkTable.getInstance().createSdk(Path.of("/opt/gone-sdk"), "gone-sdk", new HeadlessSdkType());
        sdk.setPlatformTargetId("remote-server:test:gone");

        assertThatThrownBy(sdk::getPlatform).isInstanceOf(IllegalStateException.class);

        sdk.setPlatformTargetId(null);
        assertThat(sdk.getPlatform()).isSameAs(Platform.current());

        Element element = new Element("sdk");
        sdk.writeExternal(element);
        assertThat(element.getChild("platformTarget")).isNull();
    }
}
