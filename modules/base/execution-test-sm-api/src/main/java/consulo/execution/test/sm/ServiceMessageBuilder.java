/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.execution.test.sm;

import jetbrains.buildServer.messages.serviceMessages.MapSerializerUtil;
import jetbrains.buildServer.messages.serviceMessages.ServiceMessageTypes;
import jakarta.annotation.Nonnull;

/**
 * @author gregsh
 */
public class ServiceMessageBuilder {
    private final StringBuilder myText = new StringBuilder("##teamcity[");

    public ServiceMessageBuilder(String command) {
        myText.append(command);
    }

    public ServiceMessageBuilder addAttribute(String name, String value) {
        myText.append(' ').append(name).append("='").append(replaceEscapeSymbols(value)).append('\'');
        return this;
    }

    @Override
    public String toString() {
        return myText.toString() + ']';
    }

    private static String replaceEscapeSymbols(String text) {
        return MapSerializerUtil.escapeStr(text, MapSerializerUtil.STD_ESCAPER);
    }

    public static ServiceMessageBuilder testSuiteStarted(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_SUITE_STARTED).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testSuiteFinished(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_SUITE_FINISHED).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testStarted(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_STARTED).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testFinished(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_FINISHED).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testStdOut(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_STD_OUT).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testStdErr(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_STD_ERR).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testFailed(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_FAILED).addAttribute("name", name);
    }

    public static ServiceMessageBuilder testIgnored(@Nonnull String name) {
        return new ServiceMessageBuilder(ServiceMessageTypes.TEST_IGNORED).addAttribute("name", name);
    }
}
