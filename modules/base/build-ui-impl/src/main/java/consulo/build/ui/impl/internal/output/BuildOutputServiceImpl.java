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
package consulo.build.ui.impl.internal.output;

import consulo.annotation.component.ServiceImpl;
import consulo.build.ui.output.BuildOutputInstantReader;
import consulo.build.ui.output.BuildOutputParser;
import consulo.build.ui.output.BuildOutputService;
import consulo.build.ui.progress.BuildProgressListener;
import jakarta.inject.Singleton;

import java.util.List;

/**
 * @author VISTALL
 * @since 2026-02-22
 */
@ServiceImpl
@Singleton
public class BuildOutputServiceImpl implements BuildOutputService {
    @Override
    public BuildOutputInstantReader.Primary createBuildOutputInstantReader(Object buildId, Object parentEventId, BuildProgressListener buildProgressListener, List<BuildOutputParser> parsers, int pushBackBufferSize, int channelBufferCapacity) {
        return new BuildOutputInstantReaderImpl(buildId, parentEventId, buildProgressListener, parsers, pushBackBufferSize, channelBufferCapacity);
    }
}
