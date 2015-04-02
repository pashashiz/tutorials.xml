package com.ps.tutorial.xml.dom4j;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Dom4jExample {

    public static void main(String[] args) throws DocumentException {
        SAXReader reader = new SAXReader();
        Document document = reader.read(Dom4jExample.class.getClassLoader().getResourceAsStream("policy.xml"));
        Element root = document.getRootElement();
        System.out.println("Root: " + root.getName());
        String name = root.selectSingleNode("//policyName").getName();
        System.out.println("Policy name: " + name);
        System.out.println("Node count with xsi:type attribute: " + root.selectNodes("//*[@xsi:type]").size());
    }

}
