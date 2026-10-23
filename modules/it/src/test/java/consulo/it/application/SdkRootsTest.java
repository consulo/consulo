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
package consulo.it.application;

import consulo.application.AccessToken;
import consulo.application.Application;
import consulo.application.content.impl.internal.bundle.SdkImpl;
import consulo.application.internal.SlowOperations;
import consulo.content.base.BinariesOrderRootType;
import consulo.content.base.SourcesOrderRootType;
import consulo.content.bundle.Sdk;
import consulo.content.bundle.SdkModificator;
import consulo.content.bundle.SdkTable;
import consulo.it.HeadlessApplicationExtension;
import consulo.it.internal.HeadlessSdkType;
import consulo.ui.UIAccess;
import consulo.virtualFileSystem.LocalFileSystem;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SDK roots are stored as urls: loading an SDK must not look anything up in the VFS, files are resolved on demand.
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class SdkRootsTest {
    @Test
    public void loadingAnSdkOnTheUiThreadIsNoSlowOperation(Application application) throws Exception {
        Path home = Files.createTempDirectory("consulo-it-sdk-roots");
        Path classes = Files.createDirectories(home.resolve("deep").resolve("classes"));
        Element state = rawSdkState(home, classes);

        CompletableFuture<Throwable> failure = new CompletableFuture<>();
        CompletableFuture<Boolean> ranOnUiThread = new CompletableFuture<>();
        String previous = System.getProperty(SlowOperations.FORBID_SLOW_OPS_PROPERTY);
        System.setProperty(SlowOperations.FORBID_SLOW_OPS_PROPERTY, "true");
        try {
            application.getLastUIAccess().give(() -> {
                ranOnUiThread.complete(UIAccess.isUIThread());
                try (AccessToken forceAssert = SlowOperations.startSection(SlowOperations.FORCE_ASSERT);
                     AccessToken forceThrow = SlowOperations.startSection(SlowOperations.FORCE_THROW)) {
                    newSdk().readExternal(state, SdkTable.getInstance());
                    failure.complete(null);
                }
                catch (Throwable t) {
                    failure.complete(t);
                }
            });

            assertThat(ranOnUiThread.get(30, TimeUnit.SECONDS)).isTrue();
            assertThat(failure.get(30, TimeUnit.SECONDS)).isNull();
        }
        finally {
            if (previous == null) {
                System.clearProperty(SlowOperations.FORBID_SLOW_OPS_PROPERTY);
            }
            else {
                System.setProperty(SlowOperations.FORBID_SLOW_OPS_PROPERTY, previous);
            }
        }
    }

    @Test
    public void rootsResolveOnDemand(Application application) throws Exception {
        Path home = Files.createTempDirectory("consulo-it-sdk-roots");
        Path classes = Files.createDirectories(home.resolve("classes"));
        Path missing = home.resolve("missing");
        Element state = sdkState(home, classes, missing);

        SdkImpl sdk = newSdk();
        sdk.readExternal(state, SdkTable.getInstance());

        assertThat(sdk.getRootProvider().getUrls(BinariesOrderRootType.ID)).containsExactly(VirtualFileUtil.pathToUrl(classes.toString()));
        assertThat(sdk.getRootProvider().getUrls(SourcesOrderRootType.ID)).containsExactly(VirtualFileUtil.pathToUrl(missing.toString()));

        VirtualFile classesFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(classes);
        assertThat(classesFile).isNotNull();
        assertThat(sdk.getRootProvider().getFiles(BinariesOrderRootType.ID)).containsExactly(classesFile);
        assertThat(sdk.getRootProvider().getFiles(SourcesOrderRootType.ID)).isEmpty();

        Files.createDirectories(missing);
        VirtualFile createdFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(missing);
        assertThat(createdFile).isNotNull();
        assertThat(sdk.getRootProvider().getFiles(SourcesOrderRootType.ID)).containsExactly(createdFile);
    }

    @Test
    public void writeExternalKeepsTheFormat(Application application) throws Exception {
        Path home = Files.createTempDirectory("consulo-it-sdk-roots");
        Path classes = Files.createDirectories(home.resolve("classes"));
        Element state = sdkState(home, classes, home.resolve("missing"));

        SdkImpl sdk = newSdk();
        sdk.readExternal(state, SdkTable.getInstance());

        Element written = new Element("sdk");
        sdk.writeExternal(written);

        Element composite = written.getChild("roots").getChild(BinariesOrderRootType.ID).getChild("root");
        assertThat(composite.getAttributeValue("type")).isEqualTo("composite");
        assertThat(composite.getChildren("root")).hasSize(1);
        assertThat(composite.getChildren("root").get(0).getAttributeValue("type")).isEqualTo("simple");
        assertThat(composite.getChildren("root").get(0).getAttributeValue("url")).isEqualTo(VirtualFileUtil.pathToUrl(classes.toString()));
    }

    private static SdkImpl newSdk() {
        return (SdkImpl) SdkTable.getInstance().createSdk("sdk-roots-test", new HeadlessSdkType());
    }

    private static Element rawSdkState(Path home, Path classes) {
        Element root = new Element("root").setAttribute("type", "simple").setAttribute("url", VirtualFileUtil.pathToUrl(classes.toString()));
        Element composite = new Element("root").setAttribute("type", "composite").addContent(root);

        Element sdk = new Element("sdk");
        sdk.addContent(new Element("name").setAttribute("value", "sdk-roots-raw"));
        sdk.addContent(new Element("type").setAttribute("value", HeadlessSdkType.ID));
        sdk.addContent(new Element("homePath").setAttribute("value", home.toString()));
        sdk.addContent(new Element("roots").addContent(new Element(BinariesOrderRootType.ID).addContent(composite)));
        return sdk;
    }

    private static Element sdkState(Path home, Path classes, Path sources) {
        Sdk sdk = newSdk();
        SdkModificator modificator = sdk.getSdkModificator();
        modificator.setHomePath(home.toString());
        modificator.addRoot(VirtualFileUtil.pathToUrl(classes.toString()), BinariesOrderRootType.ID);
        modificator.addRoot(VirtualFileUtil.pathToUrl(sources.toString()), SourcesOrderRootType.ID);
        modificator.commitChanges();

        Element element = new Element("sdk");
        ((SdkImpl) sdk).writeExternal(element);
        return element;
    }
}
