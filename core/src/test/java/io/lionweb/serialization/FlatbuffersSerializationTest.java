package io.lionweb.serialization;

import static org.junit.Assert.*;

import io.lionweb.LionWebVersion;
import io.lionweb.language.*;
import io.lionweb.model.*;
import io.lionweb.model.impl.DynamicAnnotationInstance;
import io.lionweb.model.impl.DynamicNode;
import io.lionweb.serialization.data.*;
import io.lionweb.serialization.refsmm.ContainerNode;
import io.lionweb.serialization.refsmm.RefNode;
import io.lionweb.serialization.refsmm.RefsLanguage;
import io.lionweb.serialization.simplemath.IntLiteral;
import io.lionweb.serialization.simplemath.SimpleMathLanguage;
import io.lionweb.serialization.simplemath.Sum;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;

/** Testing various functionalities of FlatBuffersSerialization. */
public class FlatbuffersSerializationTest extends SerializationTest {

  private void prepareDeserializationOfSimpleMath(
      FlatBuffersSerialization flatBuffersSerialization) {
    flatBuffersSerialization.getClassifierResolver().registerLanguage(SimpleMathLanguage.INSTANCE);
    flatBuffersSerialization
        .getInstantiator()
        .registerCustomDeserializer(
            SimpleMathLanguage.INT_LITERAL.getID(),
            (concept, serializedNode, deserializedNodesByID, propertiesValues) ->
                new IntLiteral(
                    (Integer) propertiesValues.get(concept.getPropertyByName("value")),
                    serializedNode.getID()));
    flatBuffersSerialization
        .getInstantiator()
        .registerCustomDeserializer(
            SimpleMathLanguage.SUM.getID(),
            (concept, serializedNode, deserializedNodesByID, propertiesValues) -> {
              SerializedContainmentValue leftSCV =
                  serializedNode.getContainments().stream()
                      .filter(c -> c.getMetaPointer().getKey().equals("SimpleMath_Sum_left"))
                      .findFirst()
                      .get();
              IntLiteral left = (IntLiteral) deserializedNodesByID.get(leftSCV.getValue().get(0));
              SerializedContainmentValue rightSCV =
                  serializedNode.getContainments().stream()
                      .filter(c -> c.getMetaPointer().getKey().equals("SimpleMath_Sum_right"))
                      .findFirst()
                      .get();
              IntLiteral right = (IntLiteral) deserializedNodesByID.get(rightSCV.getValue().get(0));
              return new Sum(left, right, serializedNode.getID());
            });
  }

  @Test
  public void deserializeMultipleRoots() throws IOException {
    Sum sum1 = new Sum(new IntLiteral(1), new IntLiteral(2));
    Sum sum2 = new Sum(new IntLiteral(3), new IntLiteral(4));
    FlatBuffersSerialization serialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = serialization.serializeTreesToByteArray(sum1, sum2);
    prepareDeserializationOfSimpleMath(serialization);
    List<Sum> deserialized =
        serialization.deserializeToNodes(serialized).stream()
            .filter(n -> n instanceof Sum)
            .map(n -> (Sum) n)
            .collect(Collectors.toList());
    assertEquals(Arrays.asList(sum1, sum2), deserialized);
  }

  @Test
  public void deserializeNodesWithoutIDsInTheRightOrder() throws IOException {
    IntLiteral il1 = new IntLiteral(1, null);
    IntLiteral il2 = new IntLiteral(2, null);
    IntLiteral il3 = new IntLiteral(3, null);
    IntLiteral il4 = new IntLiteral(4, null);
    FlatBuffersSerialization serialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = serialization.serializeTreesToByteArray(il1, il2, il3, il4);
    prepareDeserializationOfSimpleMath(serialization);
    List<IntLiteral> deserialized =
        serialization.deserializeToNodes(serialized).stream()
            .map(n -> (IntLiteral) n)
            .collect(Collectors.toList());
    assertEquals(Arrays.asList(il1, il2, il3, il4), deserialized);
  }

  @Test
  public void deserializeTreesWithoutIDsInTheRightOrder() throws IOException {
    IntLiteral il1 = new IntLiteral(1, "int_1");
    IntLiteral il2 = new IntLiteral(2, "int_2");
    Sum sum1 = new Sum(il1, il2, null);
    IntLiteral il3 = new IntLiteral(3, "int_3");
    IntLiteral il4 = new IntLiteral(4, "int_4");
    Sum sum2 = new Sum(il3, il4, null);
    FlatBuffersSerialization serialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = serialization.serializeTreesToByteArray(sum1, sum2);
    prepareDeserializationOfSimpleMath(serialization);
    List<Node> deserialized = serialization.deserializeToNodes(serialized);
    assertEquals(Arrays.asList(sum1, il1, il2, sum2, il3, il4), deserialized);
  }

  @Test
  public void deserializeTreesWithArbitraryOrderAndNullIDsInTheRightOrder() throws IOException {
    // handling multiple parents with null IDs require special care as they
    // are ambiguous (i.e., they cannot be distinguished by looking at their ID)
    IntLiteral il1 = new IntLiteral(1, "int_1");
    IntLiteral il2 = new IntLiteral(2, "int_2");
    Sum sum1 = new Sum(il1, il2, null);
    IntLiteral il3 = new IntLiteral(3, "int_3");
    IntLiteral il4 = new IntLiteral(4, "int_4");
    Sum sum2 = new Sum(il3, il4, null);
    FlatBuffersSerialization serialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = serialization.serializeNodesToByteArray(il4, il1, sum1, il2, sum2, il3);
    prepareDeserializationOfSimpleMath(serialization);
    List<Node> deserialized = serialization.deserializeToNodes(serialized);
    assertEquals(Arrays.asList(il4, il1, sum1, il2, sum2, il3), deserialized);
  }

  // We should get a DeserializationException as we are unable to reassign the child with null ID
  @Test(expected = DeserializationException.class)
  public void deserializeChildrenWithNullID() throws IOException {
    IntLiteral il1 = new IntLiteral(1, "int_1");
    IntLiteral il2 = new IntLiteral(2, null);
    Sum sum1 = new Sum(il1, il2, null);
    FlatBuffersSerialization serialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = serialization.serializeNodesToByteArray(sum1, il1, il2);
    prepareDeserializationOfSimpleMath(serialization);
    List<Node> deserialized = serialization.deserializeToNodes(serialized);
    assertEquals(Arrays.asList(sum1, il1, il2), deserialized);
  }

  private void prepareDeserializationOfRefMM(FlatBuffersSerialization flatBuffersSerialization) {
    flatBuffersSerialization.getClassifierResolver().registerLanguage(RefsLanguage.INSTANCE);
    flatBuffersSerialization
        .getInstantiator()
        .registerCustomDeserializer(
            RefsLanguage.CONTAINER_NODE.getID(),
            (concept, serializedNode, deserializedNodesByID, propertiesValues) ->
                new ContainerNode(
                    (ContainerNode) propertiesValues.get(concept.getContainmentByName("contained")),
                    serializedNode.getID()));
    flatBuffersSerialization
        .getInstantiator()
        .registerCustomDeserializer(
            RefsLanguage.REF_NODE.getID(),
            (concept, serializedNode, deserializedNodesByID, propertiesValues) -> {
              return new RefNode(serializedNode.getID());
            });
  }

  @Test(expected = DeserializationException.class)
  public void deadReferences() throws IOException {
    RefNode r1 = new RefNode();
    RefNode r2 = new RefNode();
    r1.setReferred(r2);
    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = flatBuffersSerialization.serializeNodesToByteArray(r1);
    prepareDeserializationOfRefMM(flatBuffersSerialization);
    List<Node> deserialized = flatBuffersSerialization.deserializeToNodes(serialized);
  }

  @Test
  public void referencesLoop() throws IOException {
    RefNode r1 = new RefNode();
    RefNode r2 = new RefNode();
    RefNode r3 = new RefNode();
    r1.setReferred(r2);
    r2.setReferred(r3);
    r3.setReferred(r1);
    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = flatBuffersSerialization.serializeNodesToByteArray(r1, r2, r3);
    prepareDeserializationOfRefMM(flatBuffersSerialization);
    List<Node> deserialized = flatBuffersSerialization.deserializeToNodes(serialized);
    assertEquals(Arrays.asList(r1, r2, r3), deserialized);
  }

  @Test(expected = DeserializationException.class)
  public void containmentsLoop() throws IOException {
    ContainerNode c1 = new ContainerNode();
    ContainerNode c2 = new ContainerNode();
    c1.setContained(c2);
    c2.setContained(c1);
    c2.setParent(c1);
    c1.setParent(c2);

    assertEquals(c2, c1.getParent());
    assertEquals(c1, c2.getParent());
    Assert.assertEquals(Arrays.asList(c2), ClassifierInstanceUtils.getChildren(c1));
    Assert.assertEquals(Arrays.asList(c1), ClassifierInstanceUtils.getChildren(c2));

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] serialized = flatBuffersSerialization.serializeNodesToByteArray(c1, c2);
    prepareDeserializationOfRefMM(flatBuffersSerialization);
    List<Node> deserialized = flatBuffersSerialization.deserializeToNodes(serialized);
  }

  @Test
  public void serializationOfLanguageVersionsWithImports() {
    Language myLanguage = new Language();
    myLanguage.setKey("myLanguage-key");
    myLanguage.setVersion("3");
    Concept myConcept = new Concept();
    myConcept.addImplementedInterface(LionCoreBuiltins.getINamed());
    myLanguage.addElement(myConcept);

    DynamicNode myInstance = new DynamicNode("instance-a", myConcept);
    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    SerializedChunk serializedChunk =
        flatBuffersSerialization.serializeNodesToSerializationChunk(myInstance);
    assertEquals(1, serializedChunk.getClassifierInstances().size());
    SerializedClassifierInstance serializedClassifierInstance =
        serializedChunk.getClassifierInstances().get(0);
    assertEquals("instance-a", serializedClassifierInstance.getID());
    assertEquals(1, serializedClassifierInstance.getProperties().size());
    SerializedPropertyValue serializedName = serializedClassifierInstance.getProperties().get(0);
    assertEquals(
        new MetaPointer("LionCore-builtins", "2024.1", "LionCore-builtins-INamed-name"),
        serializedName.getMetaPointer());
  }

  @Test
  public void serializeAnnotations() {
    Language l = new Language("l", "l", "l", "1");
    Annotation a1 = new Annotation(l, "a1", "a1", "a1");
    Annotation a2 = new Annotation(l, "a2", "a2", "a2");
    Concept c = new Concept(l, "c", "c", "c");
    assertEquals(3, l.getElements().size());

    DynamicNode n1 = new DynamicNode("n1", c);
    AnnotationInstance a1_1 = new DynamicAnnotationInstance("a1_1", a1, n1);
    AnnotationInstance a1_2 = new DynamicAnnotationInstance("a1_2", a1, n1);
    AnnotationInstance a2_3 = new DynamicAnnotationInstance("a2_3", a2, n1);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    flatBuffersSerialization.enableDynamicNodes();
    SerializedChunk serializedChunk =
        flatBuffersSerialization.serializeNodesToSerializationChunk(n1);
    assertEquals(1, serializedChunk.getLanguages().size());
    assertEquals(new UsedLanguage("l", "1"), serializedChunk.getLanguages().get(0));

    assertEquals(4, serializedChunk.getClassifierInstances().size());
    SerializedClassifierInstance serializedN1 = serializedChunk.getClassifierInstances().get(0);
    assertEquals("n1", serializedN1.getID());
    assertNull(serializedN1.getParentNodeID());
    assertEquals(Arrays.asList("a1_1", "a1_2", "a2_3"), serializedN1.getAnnotations());
    SerializedClassifierInstance serializedA1_1 = serializedChunk.getClassifierInstances().get(1);
    assertEquals("n1", serializedA1_1.getParentNodeID());

    List<ClassifierInstance<?>> deserialized =
        flatBuffersSerialization.deserializeSerializationChunk(serializedChunk);
    assertEquals(4, deserialized.size());
    assertInstancesAreEquals(n1, deserialized.get(0));
  }

  @Test
  public void serializeAnnotationsUsingLW2023_1() {
    Language l = new Language(LionWebVersion.v2023_1, "l");
    l.setKey("l");
    l.setID("l");
    l.setVersion("1");
    Annotation a1 = new Annotation(l, "a1", "a1", "a1");
    Annotation a2 = new Annotation(l, "a2", "a2", "a2");
    Concept c = new Concept(l, "c", "c", "c");

    DynamicNode n1 = new DynamicNode("n1", c);
    AnnotationInstance a1_1 = new DynamicAnnotationInstance("a1_1", a1, n1);
    AnnotationInstance a1_2 = new DynamicAnnotationInstance("a1_2", a1, n1);
    AnnotationInstance a2_3 = new DynamicAnnotationInstance("a2_3", a2, n1);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization(LionWebVersion.v2023_1);
    flatBuffersSerialization.enableDynamicNodes();
    SerializedChunk serializedChunk =
        flatBuffersSerialization.serializeNodesToSerializationChunk(n1);

    assertEquals(4, serializedChunk.getClassifierInstances().size());
    SerializedClassifierInstance serializedN1 = serializedChunk.getClassifierInstances().get(0);
    assertEquals("n1", serializedN1.getID());
    assertNull(serializedN1.getParentNodeID());
    assertEquals(Arrays.asList("a1_1", "a1_2", "a2_3"), serializedN1.getAnnotations());
    SerializedClassifierInstance serializedA1_1 = serializedChunk.getClassifierInstances().get(1);
    assertEquals("n1", serializedA1_1.getParentNodeID());

    List<ClassifierInstance<?>> deserialized =
        flatBuffersSerialization.deserializeSerializationChunk(serializedChunk);
    assertEquals(4, deserialized.size());
    assertInstancesAreEquals(n1, deserialized.get(0));
  }

  @Test
  public void serializeLanguage() {
    Language metaLang = new Language("metaLang", "metaLang", "metaLang", "1");
    Annotation metaAnn = new Annotation(metaLang, "metaAnn", "metaAnn", "metaAnn");

    Language l = new Language("l", "l", "l", "1");
    Annotation a1 = new Annotation(l, "a1", "a1", "a1");
    Annotation a2 = new Annotation(l, "a2", "a2", "a2");
    Concept c = new Concept(l, "c", "c", "c");
    DynamicAnnotationInstance ann = new DynamicAnnotationInstance("metaAnn_1", metaAnn, c);
    c.addAnnotation(ann);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    flatBuffersSerialization.enableDynamicNodes();
    SerializedChunk serializedChunk = flatBuffersSerialization.serializeTreeToSerializationChunk(l);

    assertEquals(5, serializedChunk.getClassifierInstances().size());
    SerializedClassifierInstance serializedL = serializedChunk.getClassifierInstances().get(0);
    assertEquals("l", serializedL.getID());
    assertNull(serializedL.getParentNodeID());

    SerializedClassifierInstance serializedC = serializedChunk.getInstanceByID("c");
    assertEquals("c", serializedC.getID());
    assertEquals(Arrays.asList("metaAnn_1"), serializedC.getAnnotations());

    flatBuffersSerialization.registerLanguage(metaLang);
    List<ClassifierInstance<?>> deserialized =
        flatBuffersSerialization.deserializeSerializationChunk(serializedChunk);
    assertEquals(5, deserialized.size());
    ClassifierInstance<?> deserializedC = deserialized.get(3);
    assertInstancesAreEquals(c, deserializedC);
    assertEquals(deserialized.get(0), deserializedC.getParent());
    ClassifierInstance<?> deserializedAnn = deserialized.get(4);
    assertInstancesAreEquals(ann, deserializedAnn);
    assertEquals(deserializedC, deserializedAnn.getParent());
    assertEquals(Arrays.asList(deserializedAnn), deserializedC.getAnnotations());
  }

  @Test
  public void serializeLanguage2023_1() {
    Language metaLang = new Language(LionWebVersion.v2023_1, "metaLang");
    metaLang.setID("metaLang");
    metaLang.setKey("metaLang");
    metaLang.setVersion("1");
    Annotation metaAnn = new Annotation(metaLang, "metaAnn", "metaAnn", "metaAnn");

    Language l = new Language(LionWebVersion.v2023_1, "l");
    l.setKey("l");
    l.setID("l");
    l.setVersion("1");
    Annotation a1 = new Annotation(l, "a1", "a1", "a1");
    Annotation a2 = new Annotation(l, "a2", "a2", "a2");
    Concept c = new Concept(l, "c", "c", "c");
    DynamicAnnotationInstance ann = new DynamicAnnotationInstance("metaAnn_1", metaAnn, c);
    c.addAnnotation(ann);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization(LionWebVersion.v2023_1);
    flatBuffersSerialization.enableDynamicNodes();
    SerializedChunk serializedChunk = flatBuffersSerialization.serializeTreeToSerializationChunk(l);

    assertEquals(5, serializedChunk.getClassifierInstances().size());
    SerializedClassifierInstance serializedL = serializedChunk.getClassifierInstances().get(0);
    assertEquals("l", serializedL.getID());
    assertNull(serializedL.getParentNodeID());

    SerializedClassifierInstance serializedC = serializedChunk.getInstanceByID("c");
    assertEquals("c", serializedC.getID());
    assertEquals(Arrays.asList("metaAnn_1"), serializedC.getAnnotations());

    flatBuffersSerialization.registerLanguage(metaLang);
    List<ClassifierInstance<?>> deserialized =
        flatBuffersSerialization.deserializeSerializationChunk(serializedChunk);
    assertEquals(5, deserialized.size());
    ClassifierInstance<?> deserializedC = deserialized.get(3);
    assertInstancesAreEquals(c, deserializedC);
    assertEquals(deserialized.get(0), deserializedC.getParent());
    ClassifierInstance<?> deserializedAnn = deserialized.get(4);
    assertInstancesAreEquals(ann, deserializedAnn);
    assertEquals(deserializedC, deserializedAnn.getParent());
    assertEquals(Arrays.asList(deserializedAnn), deserializedC.getAnnotations());
  }

  @Test
  public void deserializeLanguageToChunk() {
    Language metaLang = new Language(LionWebVersion.v2023_1, "metaLang");
    metaLang.setID("metaLang");
    metaLang.setKey("metaLang");
    metaLang.setVersion("1");
    Annotation metaAnn = new Annotation(metaLang, "metaAnn", "metaAnn", "metaAnn");

    Language l = new Language(LionWebVersion.v2023_1, "l");
    l.setKey("l");
    l.setID("l");
    l.setVersion("1");
    Annotation a1 = new Annotation(l, "a1", "a1", "a1");
    Annotation a2 = new Annotation(l, "a2", "a2", "a2");
    Concept c = new Concept(l, "c", "c", "c");
    DynamicAnnotationInstance ann = new DynamicAnnotationInstance("metaAnn_1", metaAnn, c);
    c.addAnnotation(ann);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization(LionWebVersion.v2023_1);
    flatBuffersSerialization.enableDynamicNodes();
    SerializedChunk serializedChunk = flatBuffersSerialization.serializeTreeToSerializationChunk(l);

    byte[] bytes = flatBuffersSerialization.serialize(serializedChunk);
    SerializedChunk deserializedChunk = flatBuffersSerialization.deserializeToChunk(bytes);

    assertEquals(serializedChunk, deserializedChunk);
  }

  @Test
  public void serializationIncludeBuiltinsWhenUsedInProperties() {
    Language l = new Language("l", "l", "l", "1");
    Concept c = new Concept(l, "c", "c", "c");
    c.addFeature(
        Property.createRequired("foo", LionCoreBuiltins.getString())
            .setID("foo-id")
            .setKey("foo-key"));

    DynamicNode n1 = new DynamicNode("n1", c);
    ClassifierInstanceUtils.setPropertyValueByName(n1, "foo", "abc");

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    SerializedChunk serializedChunk =
        flatBuffersSerialization.serializeNodesToSerializationChunk(n1);

    assertEquals(1, serializedChunk.getLanguages().size());
    assertSerializedChunkContainsLanguage(serializedChunk, l);
  }

  @Test
  public void serializationWithNullReferencesAndProperties() throws IOException {
    Language myLanguage = new Language();
    myLanguage.setID("myLanguage");
    myLanguage.setName(null);
    myLanguage.setKey("myLanguage-key");
    myLanguage.setVersion("3");
    Concept myConcept = new Concept();
    myConcept.setID("myConcept");
    myConcept.addImplementedInterface(LionCoreBuiltins.getINamed());
    myConcept.setReferenceValues(
        myConcept.getClassifier().getReferenceByName("extends"),
        Arrays.asList(new ReferenceValue(null, null)));
    myLanguage.addElement(myConcept);

    FlatBuffersSerialization flatBuffersSerialization =
        SerializationProvider.getStandardFlatBuffersSerialization();
    byte[] bytes = flatBuffersSerialization.serializeTree(myLanguage);
    assertInstancesAreEquals(myLanguage, flatBuffersSerialization.deserializeToNodes(bytes).get(0));
  }

  private void assertSerializedChunkContainsLanguage(
      SerializedChunk serializedChunk, Language language) {
    assertTrue(
        serializedChunk.getLanguages().stream()
            .anyMatch(
                entry ->
                    entry.getKey().equals(language.getKey())
                        && entry.getVersion().equals(language.getVersion())));
  }
}
