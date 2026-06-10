/*
 * Logback: the reliable, generic, fast and flexible logging framework.
 * Copyright (C) 1999-2023, QOS.ch. All rights reserved.
 *
 * This program and the accompanying materials are dual-licensed under
 * either the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation
 *
 *   or (per the licensee's choosing)
 *
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation.
 */

package ch.qos.logback.classic.joran.serializedModel;

import ch.qos.logback.classic.model.*;
import ch.qos.logback.core.model.*;
import ch.qos.logback.core.model.conditional.ElseModel;
import ch.qos.logback.core.model.conditional.IfModel;
import ch.qos.logback.core.model.conditional.ThenModel;
import ch.qos.logback.core.net.HardenedObjectInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class HardenedModelInputStream extends HardenedObjectInputStream {
    static public List<String> getWhitelist() {
        List<String> whitelist = new ArrayList<String>();
        whitelist.add(Model.class.getName());
        whitelist.add(Model.class.getName());
        whitelist.add(IncludeModel.class.getName());
        whitelist.add(InsertFromJNDIModel.class.getName());
        whitelist.add(RootLoggerModel.class.getName());
        whitelist.add(ImportModel.class.getName());
        whitelist.add(AppenderRefModel.class.getName());
        whitelist.add(ComponentModel.class.getName());
        whitelist.add(StatusListenerModel.class.getName());
        whitelist.add(ShutdownHookModel.class.getName());
        whitelist.add(NamedComponentModel.class.getName());
        whitelist.add(AppenderModel.class.getName());
        whitelist.add(EventEvaluatorModel.class.getName());
        whitelist.add(DefineModel.class.getName());
        whitelist.add(SequenceNumberGeneratorModel.class.getName());
        whitelist.add(ImplicitModel.class.getName());
        whitelist.add(ReceiverModel.class.getName());
        whitelist.add(LoggerContextListenerModel.class.getName());
        whitelist.add(ThenModel.class.getName());
        whitelist.add(IfModel.class.getName());
        whitelist.add(NamedModel.class.getName());
        whitelist.add(ContextNameModel.class.getName());
        whitelist.add(ParamModel.class.getName());
        whitelist.add(TimestampModel.class.getName());
        whitelist.add(PropertyModel.class.getName());
        whitelist.add(ElseModel.class.getName());
        whitelist.add(ConfigurationModel.class.getName());
        whitelist.add(SiftModel.class.getName());
        whitelist.add(LoggerModel.class.getName());
        whitelist.add(SerializeModelModel.class.getName());

        return whitelist;
    }

    public HardenedModelInputStream(InputStream is) throws IOException {
        super(is, getWhitelist());
    }
}
