package com.ps.tutorial.xml.sax;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

public class SaxExample {

    public static void main(String[] args) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parser = factory.newSAXParser();
        parser.parse(SaxExample.class.getClassLoader().getResourceAsStream("policy.xml"), new PolicyHandler());
    }

    private static class PolicyHandler extends DefaultHandler {

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
            System.out.printf("Start element {uri: %s, localName: %s, qName: %s, attrs: %s}%n",
                    uri, localName, qName, attributesToString(attributes));
        }

        @Override
        public void endElement(String uri, String localName, String qName) throws SAXException {
            System.out.printf("End element {qName: %s}%n", qName);
        }

        @Override
        public void characters(char[] ch, int start, int length) throws SAXException {
            System.out.printf("Text: %s%n", new String(ch, start, length));
        }

        private String attributesToString(Attributes attributes) {
            String data = "";
            for (int i = 0; i < attributes.getLength(); i++) {
                data += attributes.getQName(i) + ": " + attributes.getValue(i);
                if (i + 1 < attributes.getLength())
                    data += ", ";
            }
            return "{" + data + "}";
        }

    }

}
