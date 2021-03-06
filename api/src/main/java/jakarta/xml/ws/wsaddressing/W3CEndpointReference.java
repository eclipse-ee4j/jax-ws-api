/*
 * Copyright (c) 2005, 2020 Oracle and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Distribution License v. 1.0, which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * SPDX-License-Identifier: BSD-3-Clause
 */

package jakarta.xml.ws.wsaddressing;


import org.w3c.dom.Element;


import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAnyElement;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.ws.EndpointReference;
import jakarta.xml.ws.WebServiceException;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.Source;



/**
 * This class represents a W3C Addressing EndpointReferece which is
 * a remote reference to a web service endpoint that supports the
 * W3C WS-Addressing 1.0 - Core Recommendation.
 * <p>
 * Developers should use this class in their SEIs if they want to
 * pass/return endpoint references that represent the W3C WS-Addressing
 * recommendation.
 * <p>
 * Jakarta XML Binding will use the Jakarta XML Binding annotations and bind this class to XML infoset
 * that is consistent with that defined by WS-Addressing.  See
 * <a href="http://www.w3.org/TR/2006/REC-ws-addr-core-20060509/">
 * WS-Addressing</a>
 * for more information on WS-Addressing EndpointReferences.
 *
 * @since 1.6, JAX-WS 2.1
 */

// XmlRootElement allows this class to be marshalled on its own
@XmlRootElement(name="EndpointReference",namespace=W3CEndpointReference.NS)
@XmlType(name="EndpointReferenceType",namespace=W3CEndpointReference.NS)
public final class W3CEndpointReference extends EndpointReference {

    private final JAXBContext w3cjc = getW3CJaxbContext();

    // should be changed to package private, keeping original modifier to keep backwards compatibility

    /**
     * Addressing namespace.
     */
    protected static final String NS = "http://www.w3.org/2005/08/addressing";

    // default constructor forbidden ...
    // should be private, keeping original modifier to keep backwards compatibility

    /**
     * Default constructor.
     */
    protected W3CEndpointReference() {
    }

    /**
     * Creates an EPR from infoset representation
     *
     * @param source A source object containing valid XmlInfoset
     * instance consistent with the W3C WS-Addressing Core
     * recommendation.
     *
     * @throws WebServiceException
     *   If the source does NOT contain a valid W3C WS-Addressing
     *   EndpointReference.
     * @throws NullPointerException
     *   If the {@code null} {@code source} value is given
     */
    public W3CEndpointReference(Source source) {
        try {
            W3CEndpointReference epr = w3cjc.createUnmarshaller().unmarshal(source,W3CEndpointReference.class).getValue();
            this.address = epr.address;
            this.metadata = epr.metadata;
            this.referenceParameters = epr.referenceParameters;
            this.elements = epr.elements;
            this.attributes = epr.attributes;
        } catch (JAXBException e) {
            throw new WebServiceException("Error unmarshalling W3CEndpointReference " ,e);
        } catch (ClassCastException e) {
            throw new WebServiceException("Source did not contain W3CEndpointReference", e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeTo(Result result){
        try {
            Marshaller marshaller = w3cjc.createMarshaller();
            marshaller.marshal(this, result);
        } catch (JAXBException e) {
            throw new WebServiceException("Error marshalling W3CEndpointReference. ", e);
        }
    }

    private static JAXBContext getW3CJaxbContext() {
        try {
            return JAXBContext.newInstance(W3CEndpointReference.class);
        } catch (JAXBException e) {
            throw new WebServiceException("Error creating JAXBContext for W3CEndpointReference. ", e);
        }
    }

    // private but necessary properties for databinding
    @XmlElement(name="Address",namespace=NS)
    private Address address;
    @XmlElement(name="ReferenceParameters",namespace=NS)
    private Elements referenceParameters;
    @XmlElement(name="Metadata",namespace=NS)
    private Elements metadata;
    // attributes and elements are not private for performance reasons
    // (JAXB can bypass reflection)
    @XmlAnyAttribute
    Map<QName,String> attributes;
    @XmlAnyElement
    List<Element> elements;


    @XmlType(name="address", namespace=W3CEndpointReference.NS)
    private static class Address {
        protected Address() {}
        @XmlValue
        String uri;
        @XmlAnyAttribute
        Map<QName,String> attributes;
    }


    @XmlType(name="elements", namespace=W3CEndpointReference.NS)
    private static class Elements {
        protected Elements() {}
        @XmlAnyElement
        List<Element> elements;
        @XmlAnyAttribute
        Map<QName,String> attributes;
    }

}
