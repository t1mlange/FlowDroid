package soot.jimple.infoflow.collections.parser;

import static soot.jimple.infoflow.collections.parser.CollectionXMLConstants.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import soot.jimple.infoflow.collections.data.*;
import soot.jimple.infoflow.collections.operations.ICollectionOperation;
import soot.jimple.infoflow.collections.operations.forward.*;
import soot.jimple.infoflow.collections.operations.forward.alias.*;
import soot.jimple.infoflow.sourcesSinks.definitions.AccessPathTuple;
import soot.jimple.infoflow.sourcesSinks.definitions.SourceSinkType;
import soot.jimple.infoflow.util.ResourceUtils;

public class CollectionXMLParser {
	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected class SAXHandler extends DefaultHandler {
		// Used inside an operation
		private Location[] keys;
		private int dataIdx;
		private String accessPathField;
		private String returnAccessPathField;
		private boolean doReturn;
		private int callbackIdx;
		private int callbackBaseIdx;
		private int callbackDataIdx;
		private int fromIdx;
		private int toIdx;

		// Used inside a CollectionMethod
		private String subSig;
		private List<ICollectionOperation> operations;
		private List<ICollectionOperation> aliasOperations;

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
				subSig = attributes.getValue(SUBSIG_ATTR);
				break;
			case ACCESS_TAG:
			case INSERT_TAG:
			case SHIFT_LEFT_TAG:
			case SHIFT_RIGHT_TAG:
			case REMOVE_TAG:
			case INVALIDATE_TAG:
			case COMPUTE_TAG:
				accessPathField = attributes.getValue(FIELD_ATTR);
				accessPathField = "<" + accessPathField.substring(1, accessPathField.length() - 1) + ">";
				break;
			case KEY_TAG:
				readKeyOrIndex(attributes, false);
				break;
			case INDEX_TAG:
				readKeyOrIndex(attributes, true);
				break;
			case DATA_TAG:
				dataIdx = Integer.parseInt(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case CALLBACK_TAG:
				callbackIdx = Integer.parseInt(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case CALLBACK_BASE_TAG:
				callbackBaseIdx = Integer.parseInt(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case CALLBACK_DATA_TAG:
				callbackDataIdx = Integer.parseInt(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case FROM_TAG:
				fromIdx = getParamIndex(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case TO_TAG:
				toIdx = getParamIndex(attributes.getValue(PARAM_IDX_ATTR));
				break;
			case RETURN_TAG:
				doReturn = true;
				returnAccessPathField = attributes.getValue(FIELD_ATTR);
				if (returnAccessPathField != null)
					returnAccessPathField = "<" + returnAccessPathField.substring(1, returnAccessPathField.length() - 1)
							+ ">";
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
				if (methods.put(subSig, new CollectionMethod(subSig, operations, aliasOperations)) != null)
					logger.error("Duplicate collection method found for <" + className + ": " + subSig + ">");
				resetAfterMethod();
				break;
			case ACCESS_TAG:
				operations.add(new AccessOperation(trimKeys(keys), accessPathField, returnAccessPathField));
				aliasOperations.add(new AliasAccessOperation(trimKeys(keys), accessPathField, returnAccessPathField));
				resetAfterOperation();
				break;
			case INSERT_TAG:
				operations.add(new InsertOperation(trimKeys(keys), dataIdx, accessPathField));
				aliasOperations.add(new AliasInsertOperation(trimKeys(keys), dataIdx, accessPathField));
				resetAfterOperation();
				break;
			case SHIFT_LEFT_TAG:
				operations.add(new ShiftLeftOperation(trimKeys(keys), accessPathField));
				aliasOperations.add(new AliasShiftLeftOperation(trimKeys(keys), accessPathField));
				resetAfterOperation();
				break;
			case SHIFT_RIGHT_TAG:
				operations.add(new ShiftRightOperation(trimKeys(keys), accessPathField));
				aliasOperations.add(new AliasShiftRightOperation(trimKeys(keys), accessPathField));
				resetAfterOperation();
				break;
			case REMOVE_TAG:
				operations.add(new RemoveOperation(trimKeys(keys), accessPathField));
				aliasOperations.add(new AliasRemoveOperation(trimKeys(keys), accessPathField));
				resetAfterOperation();
				break;
			case COPY_TAG:
				operations.add(new CopyOperation(fromIdx, toIdx));
				aliasOperations.add(new AliasCopyOperation(fromIdx, toIdx));
				resetAfterOperation();
				break;
			case INVALIDATE_TAG:
				AccessPathTuple retTuple = null;
				if (returnAccessPathField != null)
					retTuple = AccessPathTuple.fromPathElements(returnAccessPathField, null, SourceSinkType.Neither);
				operations.add(new InvalidateOperation(accessPathField, retTuple));
				// TODO
//                    aliasOperations.add(new InvalidateOperation(accessPathField, retTuple));
				resetAfterOperation();
				break;
			case COMPUTE_TAG:
				if (dataIdx != ParamIndex.UNUSED && callbackDataIdx == ParamIndex.UNUSED)
					throw new RuntimeException("callbackData must be set if data is set!");
				operations.add(new ComputeOperation(trimKeys(keys), accessPathField, dataIdx, callbackIdx,
						callbackBaseIdx, callbackDataIdx, doReturn));
				aliasOperations.add(new AliasComputeOperation(trimKeys(keys), accessPathField, dataIdx, callbackIdx,
						callbackBaseIdx, callbackDataIdx, doReturn));
				resetAfterOperation();
				break;
			}
		}

		protected void readKeyOrIndex(Attributes attributes, boolean isIndex) {
			for (int i = 0; i < MAX_KEYS; i++) {
				if (keys[i] == null) {
					String v = attributes.getValue(PARAM_IDX_ATTR);
					switch (v) {
					case ALL:
						keys[i] = isIndex ? new Index(ParamIndex.ALL) : new Key(ParamIndex.ALL);
						break;
					case FIRST_INDEX:
						keys[i] = isIndex ? new Index(ParamIndex.FIRST_INDEX) : new Key(ParamIndex.FIRST_INDEX);
						break;
					case LAST_INDEX:
						keys[i] = isIndex ? new Index(ParamIndex.LAST_INDEX) : new Key(ParamIndex.LAST_INDEX);
						break;
					case COPY_TAG:
						keys[i] = isIndex ? new Index(ParamIndex.COPY) : new Key(ParamIndex.COPY);
						break;
					default:
						try {
							keys[i] = isIndex ? new Index(Integer.parseInt(v)) : new Key(Integer.parseInt(v));
						} catch (NumberFormatException e) {
							throw new RuntimeException(v + " is not a valid index!");
						}
						break;
					}
					break;
				}
			}
		}

		protected int getParamIndex(String value) {
			switch (value) {
			case BASE_INDEX:
				return ParamIndex.BASE;
			case RETURN_INDEX:
				return ParamIndex.RETURN;
			default:
				try {
					return Integer.parseInt(value);
				} catch (NumberFormatException e) {
					throw new RuntimeException(value + " is not a valid index!");
				}
			}
		}

		protected Location[] trimKeys(Location[] keys) {
			int i;
			for (i = 0; i < keys.length; i++) {
				if (keys[i] == null)
					break;
			}
			Location[] newKeys = new Location[i];
			System.arraycopy(keys, 0, newKeys, 0, i);
			return newKeys;
		}

		protected void resetAfterOperation() {
			keys = new Location[MAX_KEYS];
			dataIdx = ParamIndex.UNUSED;
			fromIdx = ParamIndex.UNUSED;
			toIdx = ParamIndex.UNUSED;
			callbackIdx = ParamIndex.UNUSED;
			callbackBaseIdx = ParamIndex.UNUSED;
			callbackDataIdx = ParamIndex.UNUSED;
			doReturn = false;
			accessPathField = null;
			returnAccessPathField = null;
		}

		protected void resetAfterMethod() {
			subSig = "";
			operations = new ArrayList<>();
			aliasOperations = new ArrayList<>();
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
		checkXMLForValidity(fileName);

		SAXParserFactory pf = SAXParserFactory.newInstance();
		try {
			pf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			pf.setFeature("http://xml.org/sax/features/external-general-entities", false);
			pf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			SAXParser parser = pf.newSAXParser();
			parser.parse(new FileInputStream(fileName), handler);
		} catch (ParserConfigurationException | IOException | SAXException e) {
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

	protected StreamSource getXSDStreamSource() throws IOException {
		return new StreamSource(ResourceUtils.getResourceStream(XSD_FILE_PATH));
	}

	protected void checkXMLForValidity(String fileName) throws FileNotFoundException {
		SchemaFactory sf = SchemaFactory.newInstance(W3C_XML_SCHEMA);
		StreamSource xsdFile;
		try {
			xsdFile = getXSDStreamSource();
		} catch (IOException e) {
			throw new RuntimeException("Couldn't open the XSD file to check the validity", e);
		}

		StreamSource xmlFile = new StreamSource(new FileReader(fileName));
		try {
			Schema schema = sf.newSchema(xsdFile);
			Validator validator = schema.newValidator();
			try {
				validator.validate(xmlFile);
			} catch (IOException e) {
				throw new RuntimeException(fileName + " isn't valid against the xsd specification", e);
			}
		} catch (SAXException e) {
			throw new RuntimeException(fileName + " isn't valid against the xsd specification", e);
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
