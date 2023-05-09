package soot.jimple.infoflow.collections.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.data.CollectionMethod;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.operations.AccessOperation;
import soot.jimple.infoflow.collections.operations.InsertOperation;
import soot.jimple.infoflow.collections.operations.RemoveOperation;
import soot.jimple.infoflow.util.ResourceUtils;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.*;
import java.util.*;

import static soot.jimple.infoflow.collections.parser.CollectionXMLConstants.*;

public class CollectionXMLParser {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    protected class SAXHandler extends DefaultHandler {
        // Used inside an operation
        private boolean allKeys;
        private int[] keys;
        private int data;
        private boolean returnOldElement;

        // Used inside a CollectionMethod
        private String subSig;
        private List<ICollectionOperation> operations;

        // Used inside a CollectionClass
        private String collectionType;
        private String className;
        private Map<String, CollectionMethod> methods;

        protected SAXHandler() {
            // Init all fields
            resetAfterOperation();
            resetAfterMethod();
            resetAfterCollection();
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
            switch (qName) {
                case COLLECTION_MODEL_TAG:
                    collectionType = attributes.getValue(TYPE_ATTR);
                    className = attributes.getValue(CLASS_ATTR);
                    break;

                case METHOD_TAG:
                    subSig = attributes.getValue(ID_ATTR);
                    break;

                case KEY_TAG:
                    readKey(attributes);
                    break;
                case ALL_KEYS_TAG:
                    allKeys = true;
                    break;
                case DATA_TAG:
                    data = Integer.parseInt(attributes.getValue(INDEX_ATTR));
                    break;
                case RETURN_TAG:
                    returnOldElement = true;
                    break;
            }
        }

        @Override
        public void endElement(String uri, String localName, String qName) {
            switch (qName) {
                case COLLECTION_MODEL_TAG:
                    addModel(new CollectionModel(className, collectionType, methods));
                    resetAfterCollection();
                    break;
                case METHOD_TAG:
                    if (methods.put(subSig, new CollectionMethod(subSig, operations)) != null)
                        logger.error("Duplicate collection method found for <" + className + ": " + subSig + ">");
                    resetAfterMethod();
                    break;
                case ACCESS_TAG:
                    operations.add(new AccessOperation(trimKeys(keys)));
                    resetAfterOperation();
                    break;
                case INSERT_TAG:
                    operations.add(new InsertOperation(trimKeys(keys), data, returnOldElement));
                    resetAfterOperation();
                    break;
                case REMOVE_TAG:
                    operations.add(new RemoveOperation(trimKeys(keys), allKeys, returnOldElement));
                    resetAfterOperation();
                    break;
            }
        }

        protected void readKey(Attributes attributes) {
            for (int i = 0; i < MAX_KEYS; i++) {
                if (keys[i] == -1) {
                    keys[i] = Integer.parseInt(attributes.getValue(INDEX_ATTR));
                    break;
                }
            }
        }

        protected int[] trimKeys(int[] keys) {
            int i;
            for (i = 0; i < keys.length; i++) {
                if (keys[i] == -1)
                    break;
            }
            int[] newKeys = new int[i];
            System.arraycopy(keys, 0, newKeys, 0, i);
            return newKeys;
        }

        protected void resetAfterOperation() {
            allKeys = false;
            keys = new int[MAX_KEYS];
            Arrays.fill(keys, -1);
            data = -1;
            returnOldElement = false;
        }

        protected void resetAfterMethod() {
            subSig = "";
            operations = new ArrayList<>();
        }

        protected void resetAfterCollection() {
            collectionType = "";
            className = "";
            methods = new HashMap<>();
        }
    }

    private final Map<String, CollectionModel> models;
    private final SAXHandler handler;

    public CollectionXMLParser() {
        this.models = new HashMap<>();
        this.handler = new SAXHandler();
    }

    public void parse(String fileName) throws FileNotFoundException {
        checkXMLForValidity(new FileReader(fileName));

        SAXParserFactory pf = SAXParserFactory.newInstance();
        try {
            pf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            pf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            pf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            SAXParser parser = pf.newSAXParser();
            parser.parse(new FileInputStream(fileName), handler);
        } catch (ParserConfigurationException |IOException | SAXException e) {
            logger.error("Could not parse sources/sinks from stream", e);
        }
    }

    public Map<String, CollectionModel> getModels() {
        return models;
    }

    protected void addModel(CollectionModel model) {
        if (this.models.put(model.getClassName(), model) != null)
            logger.error("Duplicate collection model found for " + model.getClassName());
    }

    private static final String XSD_FILE_PATH = "schema/CollectionModel.xsd";
    private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";

    protected void checkXMLForValidity(Reader reader) {
        SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
        StreamSource xsdFile;
        try {
            xsdFile = new StreamSource(ResourceUtils.getResourceStream(XSD_FILE_PATH));
        } catch (IOException e) {
            throw new RuntimeException("Couldn't open the XSD file to check the validity", e);
        }

        StreamSource xmlFile = new StreamSource(reader);
        try {
            Schema schema = sf.newSchema(xsdFile);
            Validator validator = schema.newValidator();
            try {
                validator.validate(xmlFile);
            } catch (IOException e) {
                throw new RuntimeException("File isn't valid against the xsd specification", e);
            }
        } catch (SAXException e) {
            throw new RuntimeException("File isn't valid against the xsd specification", e);
        } finally {
            try {
                xsdFile.getInputStream().close();
                if (xmlFile.getInputStream() != null)
                    xmlFile.getInputStream().close();
            } catch (IOException e) {
                // NO-OP
            }
        }
    }
}
