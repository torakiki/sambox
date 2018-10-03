/*
 * Created on 28 set 2018 Copyright 2010 by Andrea Vacondio (andrea.vacondio@gmail.com).
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
/**
 * @author Andrea Vacondio
 *
 */
module org.sejda.sambox
{
    exports org.sejda.sambox.pdmodel.documentinterchange.prepress;
    exports org.sejda.sambox.pdmodel.interactive.measurement;
    exports org.sejda.sambox.pdmodel.interactive.documentnavigation.destination;
    exports org.sejda.sambox.rendering;
    exports org.sejda.sambox.pdmodel.encryption;
    exports org.sejda.sambox.pdmodel.common;
    exports org.sejda.sambox.pdmodel;
    exports org.sejda.sambox.pdmodel.graphics.form;
    exports org.sejda.sambox.pdmodel.graphics.image;
    exports org.sejda.sambox.pdmodel.interactive.viewerpreferences;
    exports org.sejda.sambox;
    exports org.sejda.sambox.pdmodel.documentinterchange.markedcontent;
    exports org.sejda.sambox.pdmodel.graphics.color;
    exports org.sejda.sambox.pdmodel.interactive.action;
    exports org.sejda.sambox.pdmodel.interactive.annotation;
    exports org.sejda.sambox.pdmodel.font.encoding;
    exports org.sejda.sambox.pdmodel.interactive.form;
    exports org.sejda.sambox.pdmodel.font;
    exports org.sejda.sambox.printing;
    exports org.sejda.sambox.contentstream.operator.graphics;
    exports org.sejda.sambox.pdmodel.common.function.type4;
    exports org.sejda.sambox.pdmodel.graphics.optionalcontent;
    exports org.sejda.sambox.pdmodel.interactive.pagenavigation;
    exports org.sejda.sambox.contentstream;
    exports org.sejda.sambox.pdmodel.documentinterchange.logicalstructure;
    exports org.sejda.sambox.pdmodel.documentinterchange.taggedpdf;
    exports org.sejda.sambox.pdmodel.graphics.pattern;
    exports org.sejda.sambox.text;
    exports org.sejda.sambox.contentstream.operator.text;
    exports org.sejda.sambox.pdmodel.interactive.documentnavigation.outline;
    exports org.sejda.sambox.output;
    exports org.sejda.sambox.contentstream.operator.color;
    exports org.sejda.sambox.contentstream.operator.markedcontent;
    exports org.sejda.sambox.pdmodel.common.function;
    exports org.sejda.sambox.pdmodel.graphics.shading;
    exports org.sejda.sambox.pdmodel.graphics.state;
    exports org.sejda.sambox.filter;
    exports org.sejda.sambox.input;
    exports org.sejda.sambox.contentstream.operator;
    exports org.sejda.sambox.encryption;
    exports org.sejda.sambox.pdmodel.common.filespecification;
    exports org.sejda.sambox.contentstream.operator.state;
    exports org.sejda.sambox.cos;
    exports org.sejda.sambox.pdmodel.graphics;
    exports org.sejda.sambox.pdmodel.graphics.blend;

    requires fontbox;
    requires java.desktop;
    requires java.xml;
    requires org.bouncycastle.pkix;
    requires org.bouncycastle.provider;
    requires org.sejda.io;
    requires org.sejda.commons;
    requires org.slf4j;
}
