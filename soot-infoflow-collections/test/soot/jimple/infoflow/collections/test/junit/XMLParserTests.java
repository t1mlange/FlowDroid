package soot.jimple.infoflow.collections.test.junit;

import org.junit.Assert;
import org.junit.Test;
import soot.jimple.infoflow.collections.data.CollectionModel;
import soot.jimple.infoflow.collections.parser.CollectionXMLParser;

import java.io.IOException;
import java.util.Map;

public class XMLParserTests {
    private static final String MAP_GET_SUBSIG = "java.lang.Object get(java.lang.Object)";
    private static final String MAP_PUT_SUBSIG = "java.lang.Object put(java.lang.Object,java.lang.Object)";
    private static final String MAP_REMOVE_SUBSIG = "java.lang.Object remove(java.lang.Object)";
    private static final String MAP_CLEAR_SUBSIG = "void clear()";

    @Test(timeout=30000)
    public void testXMLParser1() throws IOException {
        CollectionXMLParser parser = new CollectionXMLParser();
        parser.parse("collectionModels/java.util.Map.xml");
        Map<String, CollectionModel> models = parser.getModels();
        Assert.assertEquals(1, models.size());
        Assert.assertTrue(models.containsKey("java.util.Map"));

        CollectionModel cm = models.get("java.util.Map");
        Assert.assertEquals(CollectionModel.CollectionType.VALUE_BASED, cm.getType());
        Assert.assertTrue(cm.hasMethod(MAP_GET_SUBSIG));
        Assert.assertTrue(cm.hasMethod(MAP_PUT_SUBSIG));
        Assert.assertTrue(cm.hasMethod(MAP_REMOVE_SUBSIG));
        Assert.assertTrue(cm.hasMethod(MAP_CLEAR_SUBSIG));
    }
}
