package org.sejda.sambox.pdmodel.xmp;
/*
 * Copyright 2025 Sober Lemur S.r.l.
 * Copyright 2025 Sejda BV
 *
 * Created 27/11/25
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.GregorianCalendar;
import java.util.UUID;

import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.AdobePDFSchema;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.XMPBasicSchema;
import org.apache.xmpbox.xml.DomXmpParser;
import org.apache.xmpbox.xml.XmpParsingException;
import org.apache.xmpbox.xml.XmpSerializer;
import org.sejda.sambox.pdmodel.PDDocument;
import org.sejda.sambox.pdmodel.PDDocumentInformation;
import org.sejda.sambox.pdmodel.common.PDMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.transform.TransformerException;

/**
 * Default implementation using XMPBox
 *
 * @author Andrea Vacondio
 */
public class DefaultDocumentXmpMetadataProvider implements DocumentXmpMetadataProvider
{
    private static final Logger LOG = LoggerFactory.getLogger(
            DefaultDocumentXmpMetadataProvider.class);

    public PDMetadata xmpMetadataFor(PDDocument document)
    {
        var metadataStream = new PDMetadata();
        try (var metadataOutputStream = new BufferedOutputStream(
                metadataStream.getCOSObject().createUnfilteredStream()))
        {
            new XmpSerializer().serialize(infoToXMPMetadata(document), metadataOutputStream, true);
            return metadataStream;
        }
        catch (IOException | TransformerException e)
        {
            LOG.warn("Unable to create xmp document metadata", e);
        }
        return null;
    }

    private XMPMetadata infoToXMPMetadata(PDDocument document)
    {
        XMPMetadata metadata = getOrCreateXmpMetadata(document);
        PDDocumentInformation documentInformation = document.getDocumentInformation();
        AdobePDFSchema pdfSchema = ofNullable(metadata.getAdobePDFSchema()).orElseGet(
                metadata::createAndAddAdobePDFSchema);
        ofNullable(documentInformation.getKeywords()).ifPresent(pdfSchema::setKeywords);
        ofNullable(documentInformation.getProducer()).map(
                p -> pdfSchema.getMetadata().getTypeMapping()
                        .createAgentName(pdfSchema.getNamespace(), pdfSchema.getPrefix(),
                                "Producer", p)).ifPresent(pdfSchema::addProperty);
        pdfSchema.setPDFVersion(document.getVersion());

        XMPBasicSchema basicSchema = ofNullable(metadata.getXMPBasicSchema()).orElseGet(
                metadata::createAndAddXMPBasicSchema);
        basicSchema.addIdentifier(UUID.randomUUID().toString());
        basicSchema.setMetadataDate(new GregorianCalendar());
        ofNullable(documentInformation.getCreator()).ifPresent(basicSchema::setCreatorTool);
        ofNullable(documentInformation.getModificationDate()).ifPresent(modDate -> {
            basicSchema.setModifyDate(modDate);
            documentInformation.setModificationDate(modDate.toInstant());
        });
        ofNullable(documentInformation.getCreationDate()).ifPresent(creationDate -> {
            basicSchema.setCreateDate(creationDate);
            documentInformation.setCreationDate(creationDate.toInstant());
        });

        DublinCoreSchema dcSchema = ofNullable(metadata.getDublinCoreSchema()).orElseGet(
                metadata::createAndAddDublinCoreSchema);
        dcSchema.setFormat("application/pdf");
        ofNullable(documentInformation.getTitle()).ifPresent(dcSchema::setTitle);
        ofNullable(documentInformation.getSubject()).ifPresent(dcSchema::setDescription);
        ofNullable(documentInformation.getAuthor()).filter(
                        a -> ofNullable(dcSchema.getCreators()).map(l -> !l.contains(a)).orElse(true))
                .ifPresent(dcSchema::addCreator);
        return metadata;
    }

    private XMPMetadata getOrCreateXmpMetadata(PDDocument document)
    {
        var metadata = document.getDocumentCatalog().getMetadata();
        if (nonNull(metadata))
        {
            try
            {
                var parser = new DomXmpParser();
                parser.setStrictParsing(false);
                return parser.parse(new BufferedInputStream(metadata.createInputStream()));
            }
            catch (XmpParsingException | IOException e)
            {
                LOG.warn("Unable to parse existing document level xmp metadata", e);
            }
            finally
            {
                metadata.getCOSObject().unDecode();
            }
        }
        return XMPMetadata.createXMPMetadata();
    }
}
