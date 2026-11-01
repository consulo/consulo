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
package consulo.it.codeStyle;

import consulo.application.Application;
import consulo.component.persist.RoamingType;
import consulo.component.persist.scheme.BaseSchemeProcessor;
import consulo.component.store.impl.internal.scheme.SchemeManagerImpl;
import consulo.it.HeadlessApplicationExtension;
import consulo.language.codeStyle.CodeStyleScheme;
import consulo.language.codeStyle.CodeStyleSchemes;
import consulo.language.codeStyle.impl.internal.CodeStyleSchemeImpl;
import consulo.util.jdom.JDOMUtil;
import consulo.util.xml.serializer.WriteExternalException;
import consulo.virtualFileSystem.internal.VirtualFileTracker;
import org.jdom.Element;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A scheme read from {@code codestyles/*.xml} is added to the scheme list before its processor initializes it.
 * An application save landing in between must neither fail on the missing settings nor lose the file.
 *
 * @author VISTALL
 */
@ExtendWith(HeadlessApplicationExtension.class)
public class CodeStyleSchemeSaveRaceTest {
    private static final String SCHEME_NAME = "Default (2)";
    private static final String SCHEME_XML = """
        <code_scheme name="Default (2)" version="173">
          <option name="RIGHT_MARGIN" value="200" />
        </code_scheme>
        """;

    @Test
    public void saveWhileLoadingIsSkipped(Application application) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-codestyles-load");
        Path file = directory.resolve("Default _2_.xml");
        Files.writeString(file, SCHEME_XML);

        CountDownLatch initEntered = new CountDownLatch(1);
        CountDownLatch saveDone = new CountDownLatch(1);
        SchemeManagerImpl<CodeStyleScheme, CodeStyleSchemeImpl> manager = newManager(application, directory, scheme -> {
            initEntered.countDown();
            try {
                assertThat(saveDone.await(30, TimeUnit.SECONDS)).as("save finished").isTrue();
            }
            catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            scheme.init();
        });

        CompletableFuture<Collection<CodeStyleSchemeImpl>> load = CompletableFuture.supplyAsync(manager::loadSchemes);
        assertThat(initEntered.await(30, TimeUnit.SECONDS)).as("loadSchemes reached initScheme").isTrue();
        try {
            assertThat(manager.findSchemeByName(SCHEME_NAME)).as("scheme is listed before it is initialized").isNotNull();

            manager.save();
        }
        finally {
            saveDone.countDown();
        }

        Collection<CodeStyleSchemeImpl> result = load.get(30, TimeUnit.SECONDS);
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getCodeStyleSettings().getDefaultRightMargin()).isEqualTo(200);
        assertThat(Files.readString(file)).isEqualTo(SCHEME_XML);
    }

    /**
     * The file tracker re-reads a scheme changed on disk the same way - added first, initialized after - but outside
     * {@code loadSchemes}, so a save there writes the scheme straight from its file content.
     */
    @Test
    public void schemeIsWrittenBeforeInit(Application application) throws Exception {
        Path directory = Files.createTempDirectory("consulo-it-codestyles-tracker");
        SchemeManagerImpl<CodeStyleScheme, CodeStyleSchemeImpl> manager = newManager(application, directory, CodeStyleSchemeImpl::init);
        manager.loadSchemes();

        CodeStyleSchemeImpl scheme = CodeStyleSchemeImpl.readScheme(JDOMUtil.load(new StringReader(SCHEME_XML)), CodeStyleSchemes.getInstance());
        manager.addNewScheme(scheme, true);

        manager.save();

        assertThat(scheme.getCodeStyleSettings().getDefaultRightMargin()).isEqualTo(200);
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> written = files.toList();
            assertThat(written).hasSize(1);
            assertThat(Files.readString(written.get(0))).contains("RIGHT_MARGIN").contains("200");
        }
    }

    private static SchemeManagerImpl<CodeStyleScheme, CodeStyleSchemeImpl> newManager(Application application,
                                                                                      Path directory,
                                                                                      Consumer<CodeStyleSchemeImpl> initScheme) {
        CodeStyleSchemes schemes = CodeStyleSchemes.getInstance();
        BaseSchemeProcessor<CodeStyleScheme, CodeStyleSchemeImpl> processor = new BaseSchemeProcessor<>() {
            @Override
            public CodeStyleSchemeImpl readScheme(Element element) {
                return CodeStyleSchemeImpl.readScheme(element, schemes);
            }

            @Override
            public Element writeScheme(CodeStyleSchemeImpl scheme) throws WriteExternalException {
                return scheme.saveToDocument();
            }

            @Override
            public boolean shouldBeSaved(CodeStyleSchemeImpl scheme) {
                return !scheme.isDefault();
            }

            @Override
            public void initScheme(CodeStyleSchemeImpl scheme) {
                initScheme.accept(scheme);
            }

            @Override
            public String getName(CodeStyleScheme immutableElement) {
                return immutableElement.getName();
            }
        };

        return new SchemeManagerImpl<>(
            "$ROOT_CONFIG$/codestyles",
            application.getInstance(VirtualFileTracker.class),
            processor,
            RoamingType.DEFAULT,
            null,
            directory.toFile()
        );
    }
}
