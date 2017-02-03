package io.swagger.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ByteArrayProperty;
import io.swagger.models.properties.IntegerProperty;
import io.swagger.models.properties.MapProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import io.swagger.models.properties.StringProperty;
import io.swagger.parser.util.SwaggerDeserializationResult;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

public class SwaggerParserTest {
	@Test
	public void testModelParameters() throws Exception {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  version: \"0.0.0\"\n" + "  title: test\n" + "paths:\n"
				+ "  /foo:\n" + "    get:\n" + "      parameters:\n" + "      - name: amount\n" + "        in: body\n"
				+ "        schema:\n" + "          type: integer\n" + "          format: int64\n"
				+ "          description: amount of money\n" + "          default: 1000\n"
				+ "          maximum: 100000\n" + "      responses:\n" + "        200:\n" + "          description: ok";

		final SwaggerParser parser = new SwaggerParser();

		final Swagger swagger = parser.parse(yaml);

	}

	@Test
	public void testParseSharedPathParameters() throws Exception {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  version: \"0.0.0\"\n" + "  title: test\n" + "paths:\n"
				+ "  /persons/{id}:\n" + "    parameters:\n" + "      - in: path\n" + "        name: id\n"
				+ "        type: string\n" + "        required: true\n" + "        description: \"no\"\n" + "    get:\n"
				+ "      parameters:\n" + "        - name: id\n" + "          in: path\n" + "          required: true\n"
				+ "          type: string\n" + "          description: \"yes\"\n" + "        - name: name\n"
				+ "          in: query\n" + "          type: string\n" + "      responses:\n" + "        200:\n"
				+ "          description: ok\n";

		final SwaggerParser parser = new SwaggerParser();

		final Swagger swagger = parser.parse(yaml);
		final List<Parameter> parameters = swagger.getPath("/persons/{id}").getGet().getParameters();
		assertTrue(parameters.size() == 2);
		final Parameter id = parameters.get(0);
		assertEquals(id.getDescription(), "yes");
	}

	@Test
	public void testParseRefPathParameters() throws Exception {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  title: test\n" + "  version: '0.0.0'\n"
				+ "parameters:\n" + "  report-id:\n" + "    name: id\n" + "    in: path\n" + "    type: string\n"
				+ "    required: true\n" + "paths:\n" + "  /reports/{id}:\n" + "    parameters:\n"
				+ "        - $ref: '#/parameters/report-id'\n" + "    put:\n" + "      parameters:\n"
				+ "        - name: id\n" + "          in: body\n" + "          required: true\n" + "          schema:\n"
				+ "            $ref: '#/definitions/report'\n" + "      responses:\n" + "        200:\n"
				+ "          description: ok\n" + "definitions:\n" + "  report:\n" + "    type: object\n"
				+ "    properties:\n" + "      id:\n" + "        type: string\n" + "      name:\n"
				+ "        type: string\n" + "    required:\n" + "    - id\n" + "    - name\n";
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.parse(yaml);
	}

	@Test
	public void testUniqueParameters() throws Exception {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  title: test\n" + "  version: '0.0.0'\n"
				+ "parameters:\n" + "  foo-id:\n" + "    name: id\n" + "    in: path\n" + "    type: string\n"
				+ "    required: true\n" + "paths:\n" + "  /foos/{id}:\n" + "    parameters:\n"
				+ "        - $ref: '#/parameters/foo-id'\n" + "    get:\n" + "      responses:\n" + "        200:\n"
				+ "          schema:\n" + "            $ref: '#/definitions/foo'\n" + "    put:\n"
				+ "      parameters:\n" + "        - name: foo\n" + "          in: body\n"
				+ "          required: true\n" + "          schema:\n" + "            $ref: '#/definitions/foo'\n"
				+ "      responses:\n" + "        200:\n" + "          schema:\n"
				+ "            $ref: '#/definitions/foo'\n" + "definitions:\n" + "  foo:\n" + "    type: object\n"
				+ "    properties:\n" + "      id:\n" + "        type: string\n" + "    required:\n" + "      - id\n";
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.parse(yaml);
		final List<Parameter> parameters = swagger.getPath("/foos/{id}").getPut().getParameters();
		assertTrue(parameters.size() == 2);
	}

	@Test
	public void testLoadRelativeFileTree_Json() throws Exception {
		final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/json/parent.json");
		// Json.mapper().writerWithDefaultPrettyPrinter().writeValue(new File("resolved.json"), swagger);
	}

	@Test
	public void testLoadExternalNestedDefinitions() throws Exception {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/nested-references/b.yaml");

		final Map<String, Model> definitions = swagger.getDefinitions();
		assertTrue(definitions.containsKey("x"));
		assertTrue(definitions.containsKey("y"));
		assertTrue(definitions.containsKey("z"));
		assertEquals(((RefModel) definitions.get("i")).get$ref(), "#/definitions/k");
	}

	@Test
	public void testPetstore() throws Exception {
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult result = parser.readWithInfo("src/test/resources/petstore.json", null, true);

		assertNotNull(result);
		assertTrue(result.getMessages().isEmpty());

		final Swagger swagger = result.getSwagger();
		final Map<String, Model> definitions = swagger.getDefinitions();
		final Set<String> expectedDefinitions = new HashSet<>();
		expectedDefinitions.add("User");
		expectedDefinitions.add("Category");
		expectedDefinitions.add("Pet");
		expectedDefinitions.add("Tag");
		expectedDefinitions.add("Order");
		expectedDefinitions.add("PetArray");
		assertEquals(definitions.keySet(), expectedDefinitions);

		final Model petModel = definitions.get("Pet");
		final Set<String> expectedPetProps = new HashSet<>();
		expectedPetProps.add("id");
		expectedPetProps.add("category");
		expectedPetProps.add("name");
		expectedPetProps.add("photoUrls");
		expectedPetProps.add("tags");
		expectedPetProps.add("status");
		assertEquals(petModel.getProperties().keySet(), expectedPetProps);

		final ArrayModel petArrayModel = (ArrayModel) definitions.get("PetArray");
		assertEquals(petArrayModel.getType(), "array");
		final RefProperty refProp = (RefProperty) petArrayModel.getItems();
		assertEquals(refProp.get$ref(), "#/definitions/Pet");
		assertNull(petArrayModel.getProperties());
	}

	@Test
	public void testFileReferenceWithVendorExt() throws Exception {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/file-reference-with-vendor-ext/b.yaml");
		final Map<String, Model> definitions = swagger.getDefinitions();
		assertTrue(definitions.get("z").getVendorExtensions().get("x-foo") instanceof ObjectNode);
		assertTrue(definitions.get("x").getVendorExtensions().get("x-foo") instanceof ObjectNode);
	}

	@Test
	public void testTroublesomeFile() throws Exception {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/troublesome.yaml");
	}

	@Test
	public void testLoadRelativeFileTree_Yaml() throws Exception {
		JsonToYamlFileDuplicator.duplicateFilesInYamlFormat("src/test/resources/relative-file-references/json",
				"src/test/resources/relative-file-references/yaml");
		final Swagger swagger = doRelativeFileTest("src/test/resources/relative-file-references/yaml/parent.yaml");
		assertNotNull(Yaml.mapper().writeValueAsString(swagger));
	}

	@Test
	public void testLoadRecursiveExternalDef() throws Exception {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/file-reference-to-recursive-defs/b.yaml");

		final Map<String, Model> definitions = swagger.getDefinitions();
		assertEquals(((RefProperty) ((ArrayProperty) definitions.get("v").getProperties().get("children")).getItems())
				.get$ref(), "#/definitions/v");
		assertTrue(definitions.containsKey("y"));
		assertEquals(((RefProperty) ((ArrayProperty) definitions.get("x").getProperties().get("children")).getItems())
				.get$ref(), "#/definitions/y");
	}

	@Test
	public void testLoadNestedItemsReferences() {
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult result = parser
				.readWithInfo("src/test/resources/nested-items-references/b.yaml", null, true);
		final Swagger swagger = result.getSwagger();
		final Map<String, Model> definitions = swagger.getDefinitions();
		assertTrue(definitions.containsKey("z"));
		assertTrue(definitions.containsKey("w"));
	}

	@Test
	public void testIssue75() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/issue99.json");

		final BodyParameter param = (BodyParameter) swagger.getPaths().get("/albums").getPost().getParameters().get(0);
		final Model model = param.getSchema();

		assertNotNull(model);
		assertTrue(model instanceof ArrayModel);

		final ArrayModel am = (ArrayModel) model;
		assertTrue(am.getItems() instanceof ByteArrayProperty);
		assertEquals(am.getItems().getFormat(), "byte");
	}

	@Test(enabled = false, description = "see https://github.com/swagger-api/swagger-parser/issues/337")
	public void testIssue62() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read(
				"https://raw.githubusercontent.com/OAI/OpenAPI-Specification/master/fixtures/v2.0/json/resources/resourceWithLinkedDefinitions.json");

		assertNotNull(swagger.getPaths().get("/pets/{petId}").getGet());
	}

	@Test
	public void testIssue146() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/issue_146.yaml");
		assertNotNull(swagger);
		final QueryParameter p = ((QueryParameter) swagger.getPaths().get("/checker").getGet().getParameters().get(0));
		final StringProperty pp = (StringProperty) p.getItems();
		assertTrue("registration".equalsIgnoreCase(pp.getEnum().get(0)));
	}

	@Test(description = "Test (path & form) parameter's required attribute")
	public void testParameterRequired() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/petstore.json");
		final List<Parameter> operationParams = swagger.getPath("/pet/{petId}").getPost().getParameters();

		final PathParameter pathParameter = (PathParameter) operationParams.get(0);
		Assert.assertTrue(pathParameter.getRequired());

		final FormParameter formParameter = (FormParameter) operationParams.get(1);
		Assert.assertFalse(formParameter.getRequired());
	}

	@Test(description = "Test consumes and produces in top level and operation level")
	public void testConsumesAndProduces() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/consumes_and_produces");
		Assert.assertNotNull(swagger);

		// test consumes and produces at spec level
		Assert.assertEquals(swagger.getConsumes(), Arrays.asList("application/json"));
		Assert.assertEquals(swagger.getProduces(), Arrays.asList("application/xml"));

		// test consumes and produces at operation level
		Assert.assertEquals(swagger.getPath("/pets").getPost().getConsumes(), Arrays.asList("image/jpeg"));
		Assert.assertEquals(swagger.getPath("/pets").getPost().getProduces(), Arrays.asList("image/png"));

		// test empty consumes and produces at operation level
		Assert.assertEquals(swagger.getPath("/pets").getGet().getConsumes(), Collections.<String> emptyList());
		Assert.assertEquals(swagger.getPath("/pets").getGet().getProduces(), Collections.<String> emptyList());

		// test consumes and produces not defined at operation level
		Assert.assertNull(swagger.getPath("/pets").getPatch().getConsumes());
		Assert.assertNull(swagger.getPath("/pets").getPatch().getProduces());

	}

	@Test
	public void testIssue108() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/issue_108.json");

		assertNotNull(swagger);
	}

	@Test
	public void testIssue() {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  description: 'No description provided.'\n"
				+ "  version: '2.0'\n" + "  title: 'My web service'\n" + "  x-endpoint-name: 'default'\n" + "paths:\n"
				+ "  x-nothing: 'sorry not supported'\n" + "  /foo:\n" + "    x-something: 'yes, it is supported'\n"
				+ "    get:\n" + "      parameters: []\n" + "      responses:\n" + "        200:\n"
				+ "          description: 'Swagger API document for this service'\n" + "x-some-vendor:\n"
				+ "  sometesting: 'bye!'";
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult result = parser.readWithInfo(yaml);

		final Swagger swagger = result.getSwagger();

		assertEquals(swagger.getVendorExtensions().get("x-some-vendor"),
				Json.mapper().createObjectNode().put("sometesting", "bye!"));
		assertEquals(swagger.getPath("/foo").getVendorExtensions().get("x-something"), "yes, it is supported");
		assertTrue(result.getMessages().size() == 1);
		assertEquals(result.getMessages().get(0), "attribute paths.x-nothing is unsupported");
	}

	@Test
	public void testIssue292WithNoCollectionFormat() {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  version: '0.0.0'\n" + "  title: nada\n" + "paths:\n"
				+ "  /persons:\n" + "    get:\n" + "      parameters:\n" + "      - name: testParam\n"
				+ "        in: query\n" + "        type: array\n" + "        items:\n" + "          type: string\n"
				+ "      responses:\n" + "        200:\n" + "          description: Successful response";
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult result = parser.readWithInfo(yaml);

		final Swagger swagger = result.getSwagger();

		final Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
		final QueryParameter qp = (QueryParameter) param;
		assertNull(qp.getCollectionFormat());
	}

	@Test
	public void testIssue292WithCSVCollectionFormat() {
		final String yaml = "swagger: '2.0'\n" + "info:\n" + "  version: '0.0.0'\n" + "  title: nada\n" + "paths:\n"
				+ "  /persons:\n" + "    get:\n" + "      parameters:\n" + "      - name: testParam\n"
				+ "        in: query\n" + "        type: array\n" + "        items:\n" + "          type: string\n"
				+ "        collectionFormat: csv\n" + "      responses:\n" + "        200:\n"
				+ "          description: Successful response";
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult result = parser.readWithInfo(yaml);

		final Swagger swagger = result.getSwagger();

		final Parameter param = swagger.getPaths().get("/persons").getGet().getParameters().get(0);
		final QueryParameter qp = (QueryParameter) param;
		assertEquals(qp.getCollectionFormat(), "csv");
	}

	@Test
	public void testIssue255() {
		final SwaggerParser parser = new SwaggerParser();

		final Swagger swagger = parser.read("objectExample.json");
		assertEquals(swagger.getDefinitions().get("SamplePayload").getExample(),
				"[{\"op\":\"replace\",\"path\":\"/s\",\"v\":\"w\"}]");
	}

	@Test
	public void testIssue286() {
		final SwaggerParser parser = new SwaggerParser();

		final Swagger swagger = parser.read("issue_286.yaml");
		final Property response = swagger.getPath("/").getGet().getResponses().get("200").getSchema();
		assertTrue(response instanceof RefProperty);
		assertEquals(((RefProperty) response).getSimpleRef(), "issue_286_PetList");
		assertNotNull(swagger.getDefinitions().get("issue_286_Allergy"));
	}

	@Test
	public void testIssue360() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/issue_360.yaml");
		assertNotNull(swagger);

		final Parameter parameter = swagger.getPath("/pets").getPost().getParameters().get(0);
		assertNotNull(parameter);
		assertTrue(parameter instanceof BodyParameter);

		final BodyParameter bp = (BodyParameter) parameter;

		assertNotNull(bp.getSchema());
		final Model model = bp.getSchema();
		assertTrue(model instanceof ModelImpl);

		final ModelImpl impl = (ModelImpl) model;
		assertNotNull(impl.getProperties().get("foo"));

		final Map<String, Object> extensions = bp.getVendorExtensions();
		assertNotNull(extensions);

		assertNotNull(extensions.get("x-examples"));
		final Object o = extensions.get("x-examples");
		assertTrue(o instanceof ObjectNode);

		final ObjectNode on = (ObjectNode) o;

		final JsonNode jn = on.get("application/json");
		assertTrue(jn instanceof ObjectNode);

		final ObjectNode objectNode = (ObjectNode) jn;
		assertEquals(objectNode.get("foo").textValue(), "bar");

		final Parameter stringBodyParameter = swagger.getPath("/otherPets").getPost().getParameters().get(0);

		assertTrue(stringBodyParameter instanceof BodyParameter);
		final BodyParameter sbp = (BodyParameter) stringBodyParameter;
		assertTrue(sbp.getRequired());
		assertTrue(sbp.getAllowEmptyValue());
		assertEquals(sbp.getName(), "simple");

		final Model sbpModel = sbp.getSchema();
		assertTrue(sbpModel instanceof ModelImpl);
		final ModelImpl sbpModelImpl = (ModelImpl) sbpModel;

		assertEquals(sbpModelImpl.getType(), "string");
		assertEquals(sbpModelImpl.getFormat(), "uuid");

		final Parameter refBodyParameter = swagger.getPath("/evenMorePets").getPost().getParameters().get(0);

		assertTrue(refBodyParameter instanceof BodyParameter);
		final BodyParameter ref = (BodyParameter) refBodyParameter;
		assertTrue(ref.getRequired());
		assertTrue(ref.getAllowEmptyValue());
		assertEquals(ref.getName(), "simple");

		final Model refModel = ref.getSchema();
		assertTrue(refModel instanceof RefModel);
		final RefModel refModelImpl = (RefModel) refModel;

		assertEquals(refModelImpl.getSimpleRef(), "Pet");
	}

	@Test
	public void testParentIssue() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/issue_parent.yaml");

		final Model child = swagger.getDefinitions().get("Child");
		assertTrue(child instanceof ComposedModel);
		assertNotNull(((ComposedModel) child).getParent());
	}

	private Swagger doRelativeFileTest(final String location) {
		final SwaggerParser parser = new SwaggerParser();
		final SwaggerDeserializationResult readResult = parser.readWithInfo(location, null, true);
		if (readResult.getMessages().size() > 0) {
			Json.prettyPrint(readResult.getMessages());
		}
		final Swagger swagger = readResult.getSwagger();

		final Path path = swagger.getPath("/health");
		assertEquals(path.getClass(), Path.class); // we successfully converted the RefPath to a Path

		final List<Parameter> parameters = path.getParameters();
		assertParamDetails(parameters, 0, QueryParameter.class, "param1", "query");
		assertParamDetails(parameters, 1, HeaderParameter.class, "param2", "header");

		final Operation operation = path.getGet();
		final List<Parameter> operationParams = operation.getParameters();
		assertParamDetails(operationParams, 0, PathParameter.class, "param3", "path");
		assertParamDetails(operationParams, 1, HeaderParameter.class, "param4", "header");
		assertParamDetails(operationParams, 2, BodyParameter.class, "body", "body");
		final BodyParameter bodyParameter = (BodyParameter) operationParams.get(2);
		assertEquals(((RefModel) bodyParameter.getSchema()).get$ref(), "#/definitions/health");

		final Map<String, Response> responsesMap = operation.getResponses();

		assertResponse(swagger, responsesMap, "200", "Health information from the server", "#/definitions/health");
		assertResponse(swagger, responsesMap, "400", "Your request was not valid", "#/definitions/error");
		assertResponse(swagger, responsesMap, "500", "An unexpected error occur during processing",
				"#/definitions/error");

		final Map<String, Model> definitions = swagger.getDefinitions();
		final ModelImpl refInDefinitions = (ModelImpl) definitions.get("refInDefinitions");
		assertEquals(refInDefinitions.getDescription(), "The example model");
		expectedPropertiesInModel(refInDefinitions, "foo", "bar");

		final ArrayModel arrayModel = (ArrayModel) definitions.get("arrayModel");
		final RefProperty arrayModelItems = (RefProperty) arrayModel.getItems();
		assertEquals(arrayModelItems.get$ref(), "#/definitions/foo");

		final ModelImpl fooModel = (ModelImpl) definitions.get("foo");
		assertEquals(fooModel.getDescription(), "Just another model");
		expectedPropertiesInModel(fooModel, "hello", "world");

		final ComposedModel composedCat = (ComposedModel) definitions.get("composedCat");
		final ModelImpl child = (ModelImpl) composedCat.getChild();
		expectedPropertiesInModel(child, "huntingSkill", "prop2", "reflexes", "reflexMap");
		final ArrayProperty reflexes = (ArrayProperty) child.getProperties().get("reflexes");
		final RefProperty reflexItems = (RefProperty) reflexes.getItems();
		assertEquals(reflexItems.get$ref(), "#/definitions/reflex");
		assertTrue(definitions.containsKey(reflexItems.getSimpleRef()));

		final MapProperty reflexMap = (MapProperty) child.getProperties().get("reflexMap");
		final RefProperty reflexMapAdditionalProperties = (RefProperty) reflexMap.getAdditionalProperties();
		assertEquals(reflexMapAdditionalProperties.get$ref(), "#/definitions/reflex");

		assertEquals(composedCat.getInterfaces().size(), 2);
		assertEquals(composedCat.getInterfaces().get(0).get$ref(), "#/definitions/pet");
		assertEquals(composedCat.getInterfaces().get(1).get$ref(), "#/definitions/foo");

		return swagger;
	}

	private void expectedPropertiesInModel(final ModelImpl model, final String... expectedProperties) {
		assertEquals(model.getProperties().size(), expectedProperties.length);
		for (final String expectedProperty : expectedProperties) {
			assertTrue(model.getProperties().containsKey(expectedProperty));
		}
	}

	private void assertResponse(final Swagger swagger, final Map<String, Response> responsesMap,
			final String responseCode, final String expectedDescription, final String expectedSchemaRef) {
		final Response response = responsesMap.get(responseCode);
		final RefProperty schema = (RefProperty) response.getSchema();
		assertEquals(response.getDescription(), expectedDescription);
		assertEquals(schema.getClass(), RefProperty.class);
		assertEquals(schema.get$ref(), expectedSchemaRef);
		assertTrue(swagger.getDefinitions().containsKey(schema.getSimpleRef()));
	}

	private void assertParamDetails(final List<Parameter> parameters, final int index, final Class<?> expectedType,
			final String expectedName, final String expectedIn) {
		final Parameter param1 = parameters.get(index);
		assertEquals(param1.getClass(), expectedType);
		assertEquals(param1.getName(), expectedName);
		assertEquals(param1.getIn(), expectedIn);
	}

	@Test
	public void testNestedReferences() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/relative-file-references/json/parent.json");
		assertTrue(swagger.getDefinitions().containsKey("externalArray"));
		assertTrue(swagger.getDefinitions().containsKey("referencedByLocalArray"));
		assertTrue(swagger.getDefinitions().containsKey("externalObject"));
		assertTrue(swagger.getDefinitions().containsKey("referencedByLocalElement"));
		assertTrue(swagger.getDefinitions().containsKey("referencedBy"));
	}

	@Test
	public void testCodegenPetstore() {
		final SwaggerParser parser = new SwaggerParser();
		final Swagger swagger = parser.read("src/test/resources/petstore-codegen.yaml");
		final ModelImpl enumModel = (ModelImpl) swagger.getDefinitions().get("Enum_Test");
		assertNotNull(enumModel);
		final Property enumProperty = enumModel.getProperties().get("enum_integer");
		assertNotNull(enumProperty);

		assertTrue(enumProperty instanceof IntegerProperty);
		final IntegerProperty enumIntegerProperty = (IntegerProperty) enumProperty;
		final List<Integer> integers = enumIntegerProperty.getEnum();
		assertEquals(integers.get(0), new Integer(1));
		assertEquals(integers.get(1), new Integer(-1));

		final Operation getOrderOperation = swagger.getPath("/store/order/{orderId}").getGet();
		assertNotNull(getOrderOperation);
		final Parameter orderId = getOrderOperation.getParameters().get(0);
		assertTrue(orderId instanceof PathParameter);
		final PathParameter orderIdPathParam = (PathParameter) orderId;
		assertNotNull(orderIdPathParam.getMinimum());

		final BigDecimal minimum = orderIdPathParam.getMinimum();
		assertEquals(minimum.toString(), "1.0");

		final FormParameter formParam = (FormParameter) swagger.getPath("/fake").getPost().getParameters().get(3);

		assertEquals(formParam.getMinimum().toString(), "32.1");
	}
}
