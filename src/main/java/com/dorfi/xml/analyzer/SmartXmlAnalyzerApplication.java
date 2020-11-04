package com.dorfi.xml.analyzer;

import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

@SpringBootApplication
public class SmartXmlAnalyzerApplication implements CommandLineRunner {

    private static Logger LOGGER = LoggerFactory.getLogger(SmartXmlAnalyzerApplication.class);

    private final XmlAnalyzerService xmlAnalyzerService;

    public SmartXmlAnalyzerApplication(XmlAnalyzerService xmlAnalyzerService) {
        this.xmlAnalyzerService = xmlAnalyzerService;
    }

    public static void main(String[] args) {
        SpringApplication.run(SmartXmlAnalyzerApplication.class, args);
    }

    private static String printPath(Element element) {
        Deque<String> stack = new ArrayDeque<>();
        stack.push(element.nodeName());
        Node parentNode = element.parentNode();
        while (parentNode != null) {
            stack.push(parentNode.nodeName());
            parentNode = parentNode.parentNode();
        }
        return String.join(" > ", stack);
    }

    @Override
    public void run(String... args) throws Exception {
        if (args.length < 2) {
            LOGGER.error("Application runs with 2 arguments: <input_origin_file_path> <input_other_sample_file_path>");
            System.exit(1);
        }
        String originalFile = args[0];
        String diffCaseFile = args[1];
        LOGGER.info("Starting to find element from {} in {}", originalFile, diffCaseFile);

        Optional<Element> originalElement = xmlAnalyzerService.findElementInOriginal(new File(originalFile));
        if (originalElement.isEmpty()) {
            LOGGER.info("Element not found in {}", originalFile);
            System.exit(0);
        }

        Optional<Element> foundElement = xmlAnalyzerService.findElementInDiffCase(new File(diffCaseFile), originalElement.get());
        if (foundElement.isEmpty()) {
            LOGGER.info("Element not found in {}", diffCaseFile);
            System.exit(0);
        }

        LOGGER.info("Element found {}", printPath(foundElement.get()));
    }
}
