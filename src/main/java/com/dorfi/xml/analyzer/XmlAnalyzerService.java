package com.dorfi.xml.analyzer;

import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class XmlAnalyzerService {

    private static Logger LOGGER = LoggerFactory.getLogger(XmlAnalyzerService.class);
    private static String CHARSET_NAME = "utf8";

    private final String elementId;

    public XmlAnalyzerService(@Value("${element.id}") String elementId) {
        this.elementId = elementId;
    }

    private static Optional<Element> findElementById(File htmlFile, String targetElementId) {
        try {
            Document doc = Jsoup.parse(
                    htmlFile,
                    CHARSET_NAME,
                    htmlFile.getAbsolutePath());

            return Optional.of(doc.getElementById(targetElementId));

        } catch (IOException e) {
            LOGGER.error("Error reading [{}] file", htmlFile.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    private static Optional<Elements> findElementsByQuery(File htmlFile, String cssQuery) {
        try {
            Document doc = Jsoup.parse(
                    htmlFile,
                    CHARSET_NAME,
                    htmlFile.getAbsolutePath());

            return Optional.of(doc.select(cssQuery));

        } catch (IOException e) {
            LOGGER.error("Error reading [{}] file", htmlFile.getAbsolutePath(), e);
            return Optional.empty();
        }
    }

    private static Optional<Elements> findParentNode(File file, Element originalElement) {
        // Go up 2 levels
        var parentNode = originalElement.parentNode().parentNode();
        String tag = parentNode.nodeName();
        List<String> attributes = getAttributesAsKeyValue(parentNode);
        String query = tag + StringUtil.join(attributes, "");
        Optional<Elements> optionalElements = findElementsByQuery(file, query);
        return optionalElements;
    }

    private static Optional<Element> findBestMatchElement(Element originalElement, List<Node> nodes) {
        var attributesAsKeyValue = getAttributesAsKeyValue(originalElement);
        var originalTextNode = originalElement.childNode(0);
        Node matchElement = null;
        int attributesMatches = 0;
        for (Node node : nodes) {
            int count = attributesAsKeyValue.stream().mapToInt(originalAttribute ->
                    getAttributesAsKeyValue(node).contains(originalAttribute) ? 1 : 0).sum();
            if (node.childNodeSize() > 0 && node.childNode(0).hasSameValue(originalTextNode)) {
                count++;
            }
            LOGGER.debug("Element {} matches {} attributes", node.nodeName(), count);
            if (count > attributesMatches) {
                matchElement = node;
                attributesMatches = count;
                LOGGER.debug("Possible match {}", node.nodeName());
            } else if (count > 1 && count == attributesMatches) {
                LOGGER.warn("Another possible match {}", node.nodeName());
            }
        }

        return Optional.of((Element) matchElement);
    }

    private static List<Node> getElementChildNodes(Optional<Elements> optionalElements, String nodeName) {
        Optional<List<Node>> nodes = optionalElements.map(elements -> {
            List<Node> buttons = new ArrayList<>();
            buttons.addAll(elements.stream().flatMap(element -> getChildNodes(element.childNodes()).stream()).collect(Collectors.toList()));
            return buttons;
        });
        return nodes.get().stream().filter(node -> node.nodeName().equals(nodeName)).collect(Collectors.toList());
    }

    private static List<Node> getChildNodes(List<Node> nodes) {
        return nodes.stream().flatMap(node -> {
            List<Node> childNodes = new ArrayList<>();
            childNodes.add(node);
            if (node.childNodeSize() > 0) {
                childNodes.addAll(getChildNodes(node.childNodes()));
            }
            return childNodes.stream();
        }).collect(Collectors.toList());
    }

    private static List<String> getAttributesAsKeyValue(Node node) {
        return node.attributes().asList().stream().map(attr -> "[" + attr.getKey() + " = " + attr.getValue() + "]").collect(Collectors.toList());
    }

    public Optional<Element> findElementInOriginal(File file) {
        Optional<Element> element = findElementById(file, elementId);
        LOGGER.debug("Element found with {} element", element);

        return element;
    }

    public Optional<Element> findElementInDiffCase(File file, Element originalElement) {
        Optional<Elements> optionalElements = findParentNode(file, originalElement);
        if (optionalElements.isEmpty()) {
            return Optional.empty();
        }
        LOGGER.debug("{} elements found", optionalElements.get().size());

        List<Node> nodes = getElementChildNodes(optionalElements, originalElement.nodeName());
        return findBestMatchElement(originalElement, nodes);
    }

}
