package io.swagger.parser.util;

import static io.swagger.models.properties.PropertyBuilder.PropertyId.ALLOW_EMPTY_VALUE;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.DEFAULT;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.ENUM;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.EXCLUSIVE_MAXIMUM;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.EXCLUSIVE_MINIMUM;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.FORMAT;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MAXIMUM;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MAX_ITEMS;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MAX_LENGTH;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MINIMUM;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MIN_ITEMS;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MIN_LENGTH;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.MULTIPLE_OF;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.PATTERN;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.READ_ONLY;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.TYPE;
import static io.swagger.models.properties.PropertyBuilder.PropertyId.UNIQUE_ITEMS;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import io.swagger.models.ArrayModel;
import io.swagger.models.ComposedModel;
import io.swagger.models.Contact;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Info;
import io.swagger.models.License;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.RefModel;
import io.swagger.models.RefPath;
import io.swagger.models.RefResponse;
import io.swagger.models.Response;
import io.swagger.models.Scheme;
import io.swagger.models.SecurityRequirement;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.Xml;
import io.swagger.models.auth.ApiKeyAuthDefinition;
import io.swagger.models.auth.BasicAuthDefinition;
import io.swagger.models.auth.In;
import io.swagger.models.auth.OAuth2Definition;
import io.swagger.models.auth.SecuritySchemeDefinition;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.FormParameter;
import io.swagger.models.parameters.HeaderParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.parameters.RefParameter;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.PropertyBuilder;
import io.swagger.util.Json;

public class SwaggerDeserializer {
	static Set<String> ROOT_KEYS = new HashSet<>(Arrays.asList("swagger", "info", "host", "basePath", "schemes",
			"consumes", "produces", "paths", "definitions", "parameters", "responses", "securityDefinitions",
			"security", "tags", "externalDocs"));
	static Set<String> EXTERNAL_DOCS_KEYS = new HashSet<>(Arrays.asList("description", "url"));
	static Set<String> SCHEMA_KEYS = new HashSet<>(
			Arrays.asList("discriminator", "example", "$ref", "format", "title", "description", "default", "multipleOf",
					"maximum", "exclusiveMaximum", "minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern",
					"maxItems", "minItems", "uniqueItems", "maxProperties", "minProperties", "required", "enum", "type",
					"items", "allOf", "properties", "additionalProperties", "xml", "readOnly", "allowEmptyValue"));
	static Set<String> INFO_KEYS = new HashSet<>(
			Arrays.asList("title", "description", "termsOfService", "contact", "license", "version"));
	static Set<String> TAG_KEYS = new HashSet<>(Arrays.asList("description", "name", "externalDocs"));
	static Set<String> RESPONSE_KEYS = new HashSet<>(
			Arrays.asList("description", "schema", "headers", "examples"));
	static Set<String> CONTACT_KEYS = new HashSet<>(Arrays.asList("name", "url", "email"));
	static Set<String> LICENSE_KEYS = new HashSet<>(Arrays.asList("name", "url"));
	static Set<String> REF_MODEL_KEYS = new HashSet<>(Arrays.asList("$ref"));
	static Set<String> PATH_KEYS = new HashSet<>(
			Arrays.asList("$ref", "get", "put", "post", "delete", "head", "patch", "options", "parameters"));
	static Set<String> OPERATION_KEYS = new HashSet<>(
			Arrays.asList("scheme", "tags", "summary", "description", "externalDocs", "operationId", "consumes",
					"produces", "parameters", "responses", "schemes", "deprecated", "security"));
	static Set<String> PARAMETER_KEYS = new HashSet<>(Arrays.asList("name", "in", "description", "required",
			"type", "format", "allowEmptyValue", "items", "collectionFormat", "default", "maximum", "exclusiveMaximum",
			"minimum", "exclusiveMinimum", "maxLength", "minLength", "pattern", "maxItems", "minItems", "uniqueItems",
			"enum", "multipleOf", "readOnly", "allowEmptyValue"));
	static Set<String> BODY_PARAMETER_KEYS = new HashSet<>(
			Arrays.asList("name", "in", "description", "required", "schema"));
	static Set<String> SECURITY_SCHEME_KEYS = new HashSet<>(
			Arrays.asList("type", "name", "in", "description", "flow", "authorizationUrl", "tokenUrl", "scopes"));

	public SwaggerDeserializationResult deserialize(final JsonNode rootNode) {
		final SwaggerDeserializationResult result = new SwaggerDeserializationResult();
		final ParseResult rootParse = new ParseResult();

		final Swagger swagger = parseRoot(rootNode, rootParse);
		result.setSwagger(swagger);
		result.setMessages(rootParse.getMessages());
		return result;
	}

	public Swagger parseRoot(final JsonNode node, final ParseResult result) {
		final String location = "";
		final Swagger swagger = new Swagger();
		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			final ObjectNode on = (ObjectNode) node;
			Iterator<JsonNode> it = null;

			// required
			String value = getString("swagger", on, true, location, result);
			swagger.setSwagger(value);

			ObjectNode obj = getObject("info", on, true, "", result);
			if (obj != null) {
				final Info info = info(obj, "info", result);
				swagger.info(info);
			}

			// optional
			value = getString("host", on, false, location, result);
			swagger.setHost(value);

			value = getString("basePath", on, false, location, result);
			swagger.setBasePath(value);

			ArrayNode array = getArray("schemes", on, false, location, result);
			if (array != null) {
				it = array.iterator();
				while (it.hasNext()) {
					final JsonNode n = it.next();
					final String s = getString(n, location + ".schemes", result);
					if (s != null) {
						final Scheme scheme = Scheme.forValue(s);
						if (scheme != null) {
							swagger.scheme(scheme);
						}
					}
				}
			}

			array = getArray("consumes", on, false, location, result);
			if (array != null) {
				it = array.iterator();
				while (it.hasNext()) {
					final JsonNode n = it.next();
					final String s = getString(n, location + ".consumes", result);
					if (s != null) {
						swagger.consumes(s);
					}
				}
			}

			array = getArray("produces", on, false, location, result);
			if (array != null) {
				it = array.iterator();
				while (it.hasNext()) {
					final JsonNode n = it.next();
					final String s = getString(n, location + ".produces", result);
					if (s != null) {
						swagger.produces(s);
					}
				}
			}

			obj = getObject("paths", on, true, location, result);
			final Map<String, Path> paths = paths(obj, "paths", result);
			swagger.paths(paths);

			obj = getObject("definitions", on, false, location, result);
			final Map<String, Model> definitions = definitions(obj, "definitions", result);
			swagger.setDefinitions(definitions);

			obj = getObject("parameters", on, false, location, result);
			// TODO: parse

			if (obj != null) {
				final Map<String, Parameter> parameters = new HashMap<>();
				final Set<String> keys = getKeys(obj);
				for (final String key : keys) {
					final JsonNode paramNode = obj.get(key);
					if (paramNode instanceof ObjectNode) {
						final Parameter parameter = parameter((ObjectNode) paramNode, location, result);
						parameters.put(key, parameter);
					}
				}
				swagger.setParameters(parameters);
			}

			obj = getObject("responses", on, false, location, result);
			final Map<String, Response> responses = responses(obj, "responses", result);
			swagger.responses(responses);

			obj = getObject("securityDefinitions", on, false, location, result);
			final Map<String, SecuritySchemeDefinition> securityDefinitions = securityDefinitions(obj, location,
					result);
			swagger.setSecurityDefinitions(securityDefinitions);

			array = getArray("security", on, false, location, result);
			final List<SecurityRequirement> security = securityRequirements(array, location, result);
			swagger.setSecurity(security);

			array = getArray("tags", on, false, location, result);
			final List<Tag> tags = tags(array, location, result);
			swagger.tags(tags);

			obj = getObject("externalDocs", on, false, location, result);
			final ExternalDocs docs = externalDocs(obj, location, result);
			swagger.externalDocs(docs);

			// extra keys
			final Set<String> keys = getKeys(on);
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					swagger.vendorExtension(key, extension(on.get(key)));
				} else if (!ROOT_KEYS.contains(key)) {
					result.extra(location, key, node.get(key));
				}
			}
		} else {
			result.invalidType("", "", "object", node);
			result.invalid();
			return null;
		}
		return swagger;
	}

	public Map<String, Path> paths(final ObjectNode obj, final String location, final ParseResult result) {
		final Map<String, Path> output = new LinkedHashMap<>();
		if (obj == null) {
			return null;
		}

		final Set<String> pathKeys = getKeys(obj);
		for (final String pathName : pathKeys) {
			final JsonNode pathValue = obj.get(pathName);
			if (pathName.startsWith("x-")) {
				result.unsupported(location, pathName, pathValue);
			} else {
				if (!pathValue.getNodeType().equals(JsonNodeType.OBJECT)) {
					result.invalidType(location, pathName, "object", pathValue);
				} else {
					final ObjectNode path = (ObjectNode) pathValue;
					final Path pathObj = path(path, location + ".'" + pathName + "'", result);
					output.put(pathName, pathObj);
				}
			}
		}
		return output;
	}

	public Path path(final ObjectNode obj, final String location, final ParseResult result) {
		final boolean hasRef = false;
		final Path output = null;
		if (obj.get("$ref") != null) {
			final JsonNode ref = obj.get("$ref");
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				return pathRef((TextNode) ref, location, result);
			}

			else if (ref.getNodeType().equals(JsonNodeType.OBJECT)) {
				final ObjectNode on = (ObjectNode) ref;

				// extra keys
				final Set<String> keys = getKeys(on);
				for (final String key : keys) {
					result.extra(location, key, on.get(key));
				}
			}
			return null;
		}
		final Path path = new Path();

		final ArrayNode parameters = getArray("parameters", obj, false, location, result);
		path.setParameters(parameters(parameters, location, result));

		ObjectNode on = getObject("get", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(get)", result);
			if (op != null) {
				path.setGet(op);
			}
		}
		on = getObject("put", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(put)", result);
			if (op != null) {
				path.setPut(op);
			}
		}
		on = getObject("post", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(post)", result);
			if (op != null) {
				path.setPost(op);
			}
		}
		on = getObject("head", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(head)", result);
			if (op != null) {
				path.setHead(op);
			}
		}
		on = getObject("delete", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(delete)", result);
			if (op != null) {
				path.setDelete(op);
			}
		}
		on = getObject("patch", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(patch)", result);
			if (op != null) {
				path.setPatch(op);
			}
		}
		on = getObject("options", obj, false, location, result);
		if (on != null) {
			final Operation op = operation(on, location + "(options)", result);
			if (op != null) {
				path.setOptions(op);
			}
		}

		// extra keys
		final Set<String> keys = getKeys(obj);
		for (final String key : keys) {
			if (key.startsWith("x-")) {
				path.setVendorExtension(key, extension(obj.get(key)));
			} else if (!PATH_KEYS.contains(key)) {
				result.extra(location, key, obj.get(key));
			}
		}
		return path;
	}

	public Operation operation(final ObjectNode obj, final String location, final ParseResult result) {
		if (obj == null) {
			return null;
		}
		final Operation output = new Operation();
		ArrayNode array = getArray("tags", obj, false, location, result);
		final List<String> tags = tagStrings(array, location, result);
		if (tags != null) {
			output.tags(tags);
		}
		String value = getString("summary", obj, false, location, result);
		output.summary(value);

		value = getString("description", obj, false, location, result);
		output.description(value);

		final ObjectNode externalDocs = getObject("externalDocs", obj, false, location, result);
		final ExternalDocs docs = externalDocs(externalDocs, location, result);
		output.setExternalDocs(docs);

		value = getString("operationId", obj, false, location, result);
		output.operationId(value);

		array = getArray("consumes", obj, false, location, result);
		if (array != null) {
			if (array.size() == 0) {
				output.consumes(Collections.<String> emptyList());
			} else {
				final Iterator<JsonNode> it = array.iterator();
				while (it.hasNext()) {
					final JsonNode n = it.next();
					final String s = getString(n, location + ".consumes", result);
					if (s != null) {
						output.consumes(s);
					}
				}
			}
		}
		array = getArray("produces", obj, false, location, result);
		if (array != null) {
			if (array.size() == 0) {
				output.produces(Collections.<String> emptyList());
			} else {
				final Iterator<JsonNode> it = array.iterator();
				while (it.hasNext()) {
					final JsonNode n = it.next();
					final String s = getString(n, location + ".produces", result);
					if (s != null) {
						output.produces(s);
					}
				}
			}
		}
		final ArrayNode parameters = getArray("parameters", obj, false, location, result);
		output.setParameters(parameters(parameters, location, result));

		final ObjectNode responses = getObject("responses", obj, true, location, result);
		final Map<String, Response> responsesObject = responses(responses, location, result);
		if (responsesObject != null && responsesObject.size() == 0) {
			result.missing(location, "responses");
		}
		output.setResponses(responsesObject);

		array = getArray("schemes", obj, false, location, result);
		if (array != null) {
			final Iterator<JsonNode> it = array.iterator();
			while (it.hasNext()) {
				final JsonNode n = it.next();
				final String s = getString(n, location + ".schemes", result);
				if (s != null) {
					final Scheme scheme = Scheme.forValue(s);
					if (scheme != null) {
						output.scheme(scheme);
					}
				}
			}
		}
		final Boolean deprecated = getBoolean("deprecated", obj, false, location, result);
		if (deprecated != null) {
			output.setDeprecated(deprecated);
		}
		array = getArray("security", obj, false, location, result);
		final List<SecurityRequirement> security = securityRequirements(array, location, result);
		if (security != null) {
			final List<Map<String, List<String>>> ss = new ArrayList<>();
			for (final SecurityRequirement s : security) {
				if (s.getRequirements() != null && s.getRequirements().size() > 0) {
					ss.add(s.getRequirements());
				}
			}
			output.setSecurity(ss);
		}

		// extra keys
		final Set<String> keys = getKeys(obj);
		for (final String key : keys) {
			if (key.startsWith("x-")) {
				output.setVendorExtension(key, extension(obj.get(key)));
			} else if (!OPERATION_KEYS.contains(key)) {
				result.extra(location, key, obj.get(key));
			}
		}

		return output;
	}

	public Boolean getBoolean(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		Boolean value = null;
		final JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else {
			if (v.getNodeType().equals(JsonNodeType.BOOLEAN)) {
				value = v.asBoolean();
			} else if (v.getNodeType().equals(JsonNodeType.STRING)) {
				final String stringValue = v.textValue();
				return Boolean.parseBoolean(stringValue);
			}
		}
		return value;
	}

	public List<Parameter> parameters(final ArrayNode obj, final String location, final ParseResult result) {
		final List<Parameter> output = new ArrayList<>();
		if (obj == null) {
			return output;
		}
		for (final JsonNode item : obj) {
			if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
				final Parameter param = parameter((ObjectNode) item, location, result);
				if (param != null) {
					output.add(param);
				}
			}
		}
		return output;
	}

	public Parameter parameter(final ObjectNode obj, String location, final ParseResult result) {
		if (obj == null) {
			return null;
		}

		Parameter output = null;
		final JsonNode ref = obj.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				return refParameter((TextNode) ref, location, result);
			} else {
				result.invalidType(location, "$ref", "string", obj);
				return null;
			}
		}

		String l = null;
		final JsonNode ln = obj.get("name");
		if (ln != null) {
			l = ln.asText();
		} else {
			l = "['unknown']";
		}
		location += ".[" + l + "]";

		String value = getString("in", obj, true, location, result);
		if (value != null) {
			final String type = getString("type", obj, false, location, result);
			final String format = getString("format", obj, false, location, result);
			AbstractSerializableParameter<?> sp = null;

			if ("query".equals(value)) {
				sp = new QueryParameter();
			} else if ("header".equals(value)) {
				sp = new HeaderParameter();
			} else if ("path".equals(value)) {
				sp = new PathParameter();
			} else if ("formData".equals(value)) {
				sp = new FormParameter();
			}

			if (sp != null) {
				// type is mandatory when sp != null
				getString("type", obj, true, location, result);
				final Map<PropertyBuilder.PropertyId, Object> map = new HashMap<>();

				map.put(TYPE, type);
				map.put(FORMAT, format);
				final String defaultValue = getString("default", obj, false, location, result);
				map.put(DEFAULT, defaultValue);
				sp.setDefault(defaultValue);

				Double dbl = getDouble("maximum", obj, false, location, result);
				if (dbl != null) {
					map.put(MAXIMUM, new BigDecimal(dbl.toString()));
					sp.setMaximum(new BigDecimal(dbl.toString()));
				}

				Boolean bl = getBoolean("exclusiveMaximum", obj, false, location, result);
				if (bl != null) {
					map.put(EXCLUSIVE_MAXIMUM, bl);
					sp.setExclusiveMaximum(bl);
				}

				dbl = getDouble("minimum", obj, false, location, result);
				if (dbl != null) {
					map.put(MINIMUM, new BigDecimal(dbl.toString()));
					sp.setMinimum(new BigDecimal(dbl.toString()));
				}

				bl = getBoolean("exclusiveMinimum", obj, false, location, result);
				if (bl != null) {
					map.put(EXCLUSIVE_MINIMUM, bl);
					sp.setExclusiveMinimum(bl);
				}

				map.put(MAX_LENGTH, getInteger("maxLength", obj, false, location, result));
				map.put(MIN_LENGTH, getInteger("minLength", obj, false, location, result));

				final String pat = getString("pattern", obj, false, location, result);
				map.put(PATTERN, pat);
				sp.setPattern(pat);

				Integer iv = getInteger("maxItems", obj, false, location, result);
				map.put(MAX_ITEMS, iv);
				sp.setMaxItems(iv);

				iv = getInteger("minItems", obj, false, location, result);
				map.put(MIN_ITEMS, iv);
				sp.setMinItems(iv);

				iv = getInteger("minLength", obj, false, location, result);
				map.put(MIN_LENGTH, iv);
				sp.setMinLength(iv);

				iv = getInteger("maxLength", obj, false, location, result);
				map.put(MAX_LENGTH, iv);
				sp.setMaxLength(iv);

				dbl = getDouble("multipleOf", obj, false, location, result);
				if (dbl != null) {
					map.put(MULTIPLE_OF, new BigDecimal(dbl.toString()));
					sp.setMultipleOf(dbl);
				}

				map.put(UNIQUE_ITEMS, getBoolean("uniqueItems", obj, false, location, result));

				final ArrayNode an = getArray("enum", obj, false, location, result);
				if (an != null) {
					final List<String> _enum = new ArrayList<>();
					for (final JsonNode n : an) {
						_enum.add(n.textValue());
					}
					sp.setEnum(_enum);
					map.put(ENUM, _enum);
				}

				bl = getBoolean("readOnly", obj, false, location, result);
				if (bl != null) {
					map.put(READ_ONLY, bl);
					sp.setReadOnly(bl);
				}

				bl = getBoolean("allowEmptyValue", obj, false, location, result);
				if (bl != null) {
					map.put(ALLOW_EMPTY_VALUE, bl);
					sp.setAllowEmptyValue(bl);
				}

				final Property prop = PropertyBuilder.build(type, format, map);

				if (prop != null) {
					sp.setProperty(prop);
					final ObjectNode items = getObject("items", obj, false, location, result);
					if (items != null) {
						final Property inner = schema(null, items, location, result);
						sp.setItems(inner);
					}
				}

				final Set<String> keys = getKeys(obj);
				for (final String key : keys) {
					if (key.startsWith("x-")) {
						sp.setVendorExtension(key, extension(obj.get(key)));
					} else if (!PARAMETER_KEYS.contains(key)) {
						result.extra(location, key, obj.get(key));
					}
				}

				final String collectionFormat = getString("collectionFormat", obj, false, location, result);
				sp.setCollectionFormat(collectionFormat);

				output = sp;
			} else if ("body".equals(value)) {
				final BodyParameter bp = new BodyParameter();

				final JsonNode node = obj.get("schema");
				if (node != null && node instanceof ObjectNode) {
					bp.setSchema(definition((ObjectNode) node, location, result));
				}

				// examples
				final ObjectNode examplesNode = getObject("examples", obj, false, location, result);
				if (examplesNode != null) {
					final Map<String, String> examples = Json.mapper().convertValue(examplesNode,
							Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
					bp.setExamples(examples);
				}

				// pattern
				final String pat = getString("pattern", obj, false, location, result);
				if (pat != null) {
					bp.setPattern(pat);
				}

				// allowEmptyValue
				Boolean bl = getBoolean("allowEmptyValue", obj, false, location, result);
				if (bl != null) {
					bp.setAllowEmptyValue(bl);
				}
				// readOnly
				bl = getBoolean("readOnly", obj, false, location, result);
				if (bl != null) {
					bp.setReadOnly(bl);
				}

				// vendor extensions
				final Set<String> keys = getKeys(obj);
				for (final String key : keys) {
					if (key.startsWith("x-")) {
						bp.setVendorExtension(key, extension(obj.get(key)));
					} else if (!BODY_PARAMETER_KEYS.contains(key)) {
						result.extra(location, key, obj.get(key));
					}
				}
				output = bp;

				// output = Json.mapper().convertValue(obj, Parameter.class);
			}
			if (output != null) {
				value = getString("name", obj, true, location, result);
				output.setName(value);

				value = getString("description", obj, false, location, result);
				output.setDescription(value);

				final Boolean required = getBoolean("required", obj, false, location, result);
				if (required != null) {
					output.setRequired(required);
				}
			}
		}

		return output;
	}

	private Property schema(final Map<String, Object> schemaItems, final JsonNode obj, final String location,
			final ParseResult result) {
		return Json.mapper().convertValue(obj, Property.class);
	}

	public RefParameter refParameter(final TextNode obj, final String location, final ParseResult result) {
		return new RefParameter(obj.asText());
	}

	public RefResponse refResponse(final TextNode obj, final String location, final ParseResult result) {
		return new RefResponse(obj.asText());
	}

	public Path pathRef(final TextNode ref, final String location, final ParseResult result) {
		final RefPath output = new RefPath();
		output.set$ref(ref.textValue());
		return output;
	}

	public Map<String, Model> definitions(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}
		final Set<String> schemas = getKeys(node);
		final Map<String, Model> output = new LinkedHashMap<>();

		for (final String schemaName : schemas) {
			final JsonNode schema = node.get(schemaName);
			if (schema.getNodeType().equals(JsonNodeType.OBJECT)) {
				final Model model = definition((ObjectNode) schema, location + "." + schemaName, result);
				if (model != null) {
					output.put(schemaName, model);
				}
			} else {
				result.invalidType(location, schemaName, "object", schema);
			}
		}
		return output;
	}

	public Model definition(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			result.missing(location, "empty schema");
			return null;
		}
		if (node.get("$ref") != null) {
			return refModel(node, location, result);
		}
		if (node.get("allOf") != null) {
			return allOfModel(node, location, result);
		}
		Model model = null;
		String value = null;

		final String type = getString("type", node, false, location, result);
		final Model m = new ModelImpl();
		if ("array".equals(type)) {
			final ArrayModel am = new ArrayModel();
			final ObjectNode propertyNode = getObject("properties", node, false, location, result);
			final Map<String, Property> properties = properties(propertyNode, location, result);
			am.setProperties(properties);

			final ObjectNode itemsNode = getObject("items", node, false, location, result);
			final Property items = property(itemsNode, location, result);
			if (items != null) {
				am.items(items);
			}

			model = am;
		} else {
			final ModelImpl impl = new ModelImpl();
			impl.setType(type);

			JsonNode ap = node.get("additionalProperties");
			if (ap != null && ap.getNodeType().equals(JsonNodeType.OBJECT)) {
				impl.setAdditionalProperties(Json.mapper().convertValue(ap, Property.class));
			}

			value = getString("default", node, false, location, result);
			impl.setDefaultValue(value);

			value = getString("format", node, false, location, result);
			impl.setFormat(value);

			value = getString("discriminator", node, false, location, result);
			impl.setDiscriminator(value);

			final Boolean bp = getBoolean("uniqueItems", node, false, location, result);
			if (bp != null) {
				impl.setUniqueItems(bp);
			}

			ap = node.get("enum");
			if (ap != null) {
				final ArrayNode arrayNode = getArray("enum", node, false, location, result);
				if (arrayNode != null) {
					for (final JsonNode n : arrayNode) {
						if (n.isValueNode()) {
							impl._enum(n.asText());
						} else {
							result.invalidType(location, "enum", "value", n);
						}
					}
				}
			}

			final JsonNode xml = node.get("xml");
			if (xml != null) {
				impl.setXml(Json.mapper().convertValue(xml, Xml.class));
			}

			final ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
			final ExternalDocs docs = externalDocs(externalDocs, location, result);
			impl.setExternalDocs(docs);

			final ObjectNode properties = getObject("properties", node, false, location, result);
			if (properties != null) {
				final Set<String> propertyNames = getKeys(properties);
				for (final String propertyName : propertyNames) {
					final JsonNode propertyNode = properties.get(propertyName);
					if (propertyNode.getNodeType().equals(JsonNodeType.OBJECT)) {
						final ObjectNode on = (ObjectNode) propertyNode;
						final Property property = property(on, location, result);
						impl.property(propertyName, property);
					} else {
						result.invalidType(location, "properties", "object", propertyNode);
					}
				}
			}

			// need to set properties first
			final ArrayNode required = getArray("required", node, false, location, result);
			if (required != null) {
				final List<String> requiredProperties = new ArrayList<>();
				for (final JsonNode n : required) {
					if (n.getNodeType().equals(JsonNodeType.STRING)) {
						requiredProperties.add(((TextNode) n).textValue());
					} else {
						result.invalidType(location, "required", "string", n);
					}
				}
				if (requiredProperties.size() > 0) {
					impl.setRequired(requiredProperties);
				}
			}

			// extra keys
			final Set<String> keys = getKeys(node);
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					impl.setVendorExtension(key, extension(node.get(key)));
				} else if (!SCHEMA_KEYS.contains(key)) {
					result.extra(location, key, node.get(key));
				}
			}
			if ("{ }".equals(Json.pretty(impl))) {
				return null;
			}
			model = impl;
		}
		final JsonNode exampleNode = node.get("example");
		if (exampleNode != null) {
			// we support text or object nodes
			if (exampleNode.getNodeType().equals(JsonNodeType.OBJECT)) {
				final ObjectNode on = getObject("example", node, false, location, result);
				if (on != null) {
					model.setExample(on);
				}
			} else {
				model.setExample(exampleNode.toString());
			}
		}

		if (model != null) {
			value = getString("description", node, false, location, result);
			model.setDescription(value);

			value = getString("title", node, false, location, result);
			model.setTitle(value);
		}

		return model;
	}

	public Object extension(final JsonNode jsonNode) {
		if (jsonNode.getNodeType().equals(JsonNodeType.BOOLEAN)) {
			return jsonNode.asBoolean();
		}
		if (jsonNode.getNodeType().equals(JsonNodeType.STRING)) {
			return jsonNode.asText();
		}
		if (jsonNode.getNodeType().equals(JsonNodeType.NUMBER)) {
			final NumericNode n = (NumericNode) jsonNode;
			if (n.isLong()) {
				return jsonNode.asLong();
			}
			if (n.isInt()) {
				return jsonNode.asInt();
			}
			if (n.isBigDecimal()) {
				return jsonNode.textValue();
			}
			if (n.isBoolean()) {
				return jsonNode.asBoolean();
			}
			if (n.isFloat()) {
				return jsonNode.floatValue();
			}
			if (n.isDouble()) {
				return jsonNode.doubleValue();
			}
			if (n.isShort()) {
				return jsonNode.intValue();
			}
			return jsonNode.asText();
		}
		if (jsonNode.getNodeType().equals(JsonNodeType.ARRAY)) {
			final ArrayNode an = (ArrayNode) jsonNode;
			final List<Object> o = new ArrayList<>();
			for (final JsonNode i : an) {
				final Object obj = extension(i);
				if (obj != null) {
					o.add(obj);
				}
			}
			return o;
		}
		return jsonNode;
	}

	public Model allOfModel(final ObjectNode node, final String location, final ParseResult result) {
		final JsonNode sub = node.get("$ref");
		final JsonNode allOf = node.get("allOf");

		if (sub != null) {
			if (sub.getNodeType().equals(JsonNodeType.OBJECT)) {
				return refModel((ObjectNode) sub, location, result);
			} else {
				result.invalidType(location, "$ref", "object", sub);
				return null;
			}
		} else if (allOf != null) {
			ComposedModel model = null;
			if (allOf.getNodeType().equals(JsonNodeType.ARRAY)) {
				model = new ComposedModel();

				int pos = 0;
				for (final JsonNode part : allOf) {
					if (part.getNodeType().equals(JsonNodeType.OBJECT)) {
						final Model segment = definition((ObjectNode) part, location, result);
						if (segment != null) {
							model.getAllOf().add(segment);
						}
					} else {
						result.invalidType(location, "allOf[" + pos + "]", "object", part);
					}
					pos++;
				}

				Model child = null;
				final List<RefModel> interfaces = new ArrayList<>();
				for (final Model m : model.getAllOf()) {
					if (m instanceof RefModel) {
						interfaces.add((RefModel) m);
					} else if (m instanceof ModelImpl) {
						// NOTE: since ComposedModel.child allows only one inline child schema, the last one 'wins'.
						child = m;
					}
				}

				if (interfaces.size() == 1) {
					// If there's only 1 interface, consider it as the parent instead
					model.setParent(interfaces.remove(0));
				}

				model.setInterfaces(interfaces);

				if (child != null) {
					model.setChild(child);
				}

			} else {
				result.invalidType(location, "allOf", "array", allOf);
			}

			// extra keys
			final Set<String> keys = getKeys(node);
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					model.setVendorExtension(key, extension(node.get(key)));
				} else if (!SCHEMA_KEYS.contains(key)) {
					result.extra(location, key, node.get(key));
				} else {
					String value = getString("title", node, false, location, result);
					model.setTitle(value);

					value = getString("description", node, false, location, result);
					model.setDescription(value);
				}
			}

			return model;
		}
		return null;
	}

	public Map<String, Property> properties(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}
		final Map<String, Property> output = new LinkedHashMap<>();

		final Set<String> keys = getKeys(node);
		for (final String propertyName : keys) {
			final JsonNode propertyNode = node.get(propertyName);
			if (propertyNode.getNodeType().equals(JsonNodeType.OBJECT)) {
				final Property property = property((ObjectNode) propertyNode, location, result);
				output.put(propertyName, property);
			} else {
				result.invalidType(location, propertyName, "object", propertyNode);
			}
		}
		return output;
	}

	public Property property(final ObjectNode node, final String location, final ParseResult result) {
		if (node != null) {
			if (node.get("type") == null) {
				// may have an enum where type can be inferred
				final JsonNode enumNode = node.get("enum");
				if (enumNode != null && enumNode.isArray()) {
					final String type = inferTypeFromArray((ArrayNode) enumNode);
					node.put("type", type);
				}
			}
		}

		// work-around for https://github.com/swagger-api/swagger-core/issues/1977
		if (node.get("$ref") != null && node.get("$ref").isTextual()) {
			// check if it's a relative ref
			String refString = node.get("$ref").textValue();
			if (refString.indexOf("/") == -1 && refString.indexOf(".") > 0) {
				refString = "./" + refString;
				node.put("$ref", refString);
			}
		}
		return Json.mapper().convertValue(node, Property.class);
	}

	public String inferTypeFromArray(final ArrayNode an) {
		if (an.size() == 0) {
			return "string";
		}
		String type = null;
		for (int i = 0; i < an.size(); i++) {
			final JsonNode element = an.get(0);
			if (element.isBoolean()) {
				if (type == null) {
					type = "boolean";
				} else if (!"boolean".equals(type)) {
					type = "string";
				}
			} else if (element.isNumber()) {
				if (type == null) {
					type = "number";
				} else if (!"number".equals(type)) {
					type = "string";
				}
			} else {
				type = "string";
			}
		}

		return type;
	}

	public RefModel refModel(final ObjectNode node, final String location, final ParseResult result) {
		final RefModel output = new RefModel();

		if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
			final String refValue = ((TextNode) node.get("$ref")).textValue();
			output.set$ref(refValue);
		} else {
			result.invalidType(location, "$ref", "object", node);
			return null;
		}

		// extra keys
		final Set<String> keys = getKeys(node);
		for (final String key : keys) {
			if (!REF_MODEL_KEYS.contains(key)) {
				result.extra(location, key, node.get(key));
			}
		}

		return output;
	}

	public Map<String, Response> responses(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}

		final Map<String, Response> output = new TreeMap<>();

		final Set<String> keys = getKeys(node);

		for (final String key : keys) {
			if (key.startsWith("x-")) {

			} else {
				final ObjectNode obj = getObject(key, node, false, location + ".responses", result);
				final Response response = response(obj, location + "." + key, result);
				output.put(key, response);
			}
		}

		return output;
	}

	public Response response(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}

		final Response output = new Response();
		final JsonNode ref = node.get("$ref");
		if (ref != null) {
			if (ref.getNodeType().equals(JsonNodeType.STRING)) {
				return refResponse((TextNode) ref, location, result);
			} else {
				result.invalidType(location, "$ref", "string", node);
				return null;
			}
		}

		final String value = getString("description", node, true, location, result);
		output.description(value);

		final ObjectNode schema = getObject("schema", node, false, location, result);
		if (schema != null) {
			output.schema(Json.mapper().convertValue(schema, Property.class));
		}
		final ObjectNode headersNode = getObject("headers", node, false, location, result);
		if (headersNode != null) {
			// TODO
			final Map<String, Property> headers = Json.mapper().convertValue(headersNode,
					Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Property.class));
			output.headers(headers);
		}

		final ObjectNode examplesNode = getObject("examples", node, false, location, result);
		if (examplesNode != null) {
			final Map<String, Object> examples = Json.mapper().convertValue(examplesNode,
					Json.mapper().getTypeFactory().constructMapType(Map.class, String.class, Object.class));
			output.setExamples(examples);
		}

		// extra keys
		final Set<String> keys = getKeys(node);
		for (final String key : keys) {
			if (key.startsWith("x-")) {
				output.setVendorExtension(key, extension(node.get(key)));
			} else if (!RESPONSE_KEYS.contains(key)) {
				result.extra(location, key, node.get(key));
			}
		}
		return output;
	}

	public Info info(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}

		final Info info = new Info();
		String value = getString("title", node, true, location, result);
		info.title(value);

		value = getString("description", node, false, location, result);
		info.description(value);

		value = getString("termsOfService", node, false, location, result);
		info.termsOfService(value);

		ObjectNode obj = getObject("contact", node, false, "contact", result);
		final Contact contact = contact(obj, location, result);
		info.contact(contact);

		obj = getObject("license", node, false, location, result);
		final License license = license(obj, location, result);
		info.license(license);

		value = getString("version", node, false, location, result);
		info.version(value);

		// extra keys
		final Set<String> keys = getKeys(node);
		for (final String key : keys) {
			if (key.startsWith("x-")) {
				info.setVendorExtension(key, extension(node.get(key)));
			} else if (!INFO_KEYS.contains(key)) {
				result.extra(location, key, node.get(key));
			}
		}

		return info;
	}

	public License license(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}

		final License license = new License();

		String value = getString("name", node, true, location, result);
		license.name(value);

		value = getString("url", node, false, location, result);
		license.url(value);

		// extra keys
		final Set<String> keys = getKeys(node);
		for (final String key : keys) {
			if (key.startsWith("x-")) {
				license.setVendorExtension(key, extension(node.get(key)));
			} else if (!LICENSE_KEYS.contains(key)) {
				result.extra(location + ".license", key, node.get(key));
			}
		}

		return license;
	}

	public Contact contact(final ObjectNode node, final String location, final ParseResult result) {
		if (node == null) {
			return null;
		}

		final Contact contact = new Contact();

		String value = getString("name", node, false, location + ".name", result);
		contact.name(value);

		value = getString("url", node, false, location + ".url", result);
		contact.url(value);

		value = getString("email", node, false, location + ".email", result);
		contact.email(value);

		// extra keys
		final Set<String> keys = getKeys(node);
		for (final String key : keys) {
			if (!CONTACT_KEYS.contains(key)) {
				result.extra(location + ".contact", key, node.get(key));
			}
		}

		return contact;
	}

	public Map<String, SecuritySchemeDefinition> securityDefinitions(final ObjectNode node, final String location,
			final ParseResult result) {
		if (node == null) {
			return null;
		}

		final Map<String, SecuritySchemeDefinition> output = new HashMap<>();
		final Set<String> keys = getKeys(node);

		for (final String key : keys) {
			final ObjectNode obj = getObject(key, node, false, location, result);
			final SecuritySchemeDefinition def = securityDefinition(obj, location, result);

			if (def != null) {
				output.put(key, def);
			}
		}

		return output;
	}

	public SecuritySchemeDefinition securityDefinition(final ObjectNode node, final String location,
			final ParseResult result) {
		if (node == null) {
			return null;
		}

		SecuritySchemeDefinition output = null;

		final String type = getString("type", node, true, location, result);
		if (type != null) {
			if (type.equals("basic")) {
				// TODO: parse manually for better feedback
				output = Json.mapper().convertValue(node, BasicAuthDefinition.class);
			} else if (type.equals("apiKey")) {
				final String position = getString("in", node, true, location, result);
				final String name = getString("name", node, true, location, result);

				if (name != null && ("header".equals(position) || "query".equals(position))) {
					final In in = In.forValue(position);
					if (in != null) {
						output = new ApiKeyAuthDefinition().name(name).in(in);
					}
				}
			} else if (type.equals("oauth2")) {
				// TODO: parse manually for better feedback
				output = Json.mapper().convertValue(node, OAuth2Definition.class);
			} else {
				result.invalidType(location + ".type", "type", "basic|apiKey|oauth2", node);
			}

			// extra keys
			final Set<String> keys = getKeys(node);
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					output.setVendorExtension(key, extension(node.get(key)));
				} else if (!SECURITY_SCHEME_KEYS.contains(key)) {
					result.extra(location, key, node.get(key));
				}
			}
		}

		return output;
	}

	public List<SecurityRequirement> securityRequirements(final ArrayNode node, final String location,
			final ParseResult result) {
		if (node == null) {
			return null;
		}

		final List<SecurityRequirement> output = new ArrayList<>();

		for (final JsonNode item : node) {
			final SecurityRequirement security = new SecurityRequirement();
			if (item.getNodeType().equals(JsonNodeType.OBJECT)) {
				final ObjectNode on = (ObjectNode) item;
				final Set<String> keys = getKeys(on);

				for (final String key : keys) {
					final List<String> scopes = new ArrayList<>();
					final ArrayNode obj = getArray(key, on, false, location + ".security", result);
					if (obj != null) {
						for (final JsonNode n : obj) {
							if (n.getNodeType().equals(JsonNodeType.STRING)) {
								scopes.add(n.asText());
							} else {
								result.invalidType(location, key, "string", n);
							}
						}
					}
					security.requirement(key, scopes);
				}
			}
			output.add(security);
		}

		return output;
	}

	public List<String> tagStrings(final ArrayNode nodes, final String location, final ParseResult result) {
		if (nodes == null) {
			return null;
		}

		final List<String> output = new ArrayList<>();

		for (final JsonNode node : nodes) {
			if (node.getNodeType().equals(JsonNodeType.STRING)) {
				output.add(node.textValue());
			}
		}
		return output;
	}

	public List<Tag> tags(final ArrayNode nodes, final String location, final ParseResult result) {
		if (nodes == null) {
			return null;
		}

		final List<Tag> output = new ArrayList<>();

		for (final JsonNode node : nodes) {
			if (node.getNodeType().equals(JsonNodeType.OBJECT)) {
				final Tag tag = tag((ObjectNode) node, location + ".tags", result);
				if (tag != null) {
					output.add(tag);
				}
			}
		}

		return output;
	}

	public Tag tag(final ObjectNode node, final String location, final ParseResult result) {
		Tag tag = null;

		if (node != null) {
			tag = new Tag();
			final Set<String> keys = getKeys(node);

			String value = getString("name", node, true, location, result);
			tag.name(value);

			value = getString("description", node, false, location, result);
			tag.description(value);

			final ObjectNode externalDocs = getObject("externalDocs", node, false, location, result);
			final ExternalDocs docs = externalDocs(externalDocs, location + "externalDocs", result);
			tag.externalDocs(docs);

			// extra keys
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					tag.setVendorExtension(key, extension(node.get(key)));
				} else if (!TAG_KEYS.contains(key)) {
					result.extra(location + ".externalDocs", key, node.get(key));
				}
			}
		}

		return tag;
	}

	public ExternalDocs externalDocs(final ObjectNode node, final String location, final ParseResult result) {
		ExternalDocs output = null;

		if (node != null) {
			output = new ExternalDocs();
			final Set<String> keys = getKeys(node);

			String value = getString("description", node, false, location, result);
			output.description(value);

			value = getString("url", node, true, location, result);
			output.url(value);

			// extra keys
			for (final String key : keys) {
				if (key.startsWith("x-")) {
					output.setVendorExtension(key, extension(node.get(key)));
				} else if (!EXTERNAL_DOCS_KEYS.contains(key)) {
					result.extra(location + ".externalDocs", key, node.get(key));
				}
			}
		}

		return output;
	}

	public String getString(final JsonNode node, final String location, final ParseResult result) {
		String output = null;
		if (!node.getNodeType().equals(JsonNodeType.STRING)) {
			result.invalidType(location, "", "string", node);
		} else {
			output = ((TextNode) node).asText();
		}
		return output;
	}

	public ArrayNode getArray(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		final JsonNode value = node.get(key);
		ArrayNode an = null;
		if (value == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!value.getNodeType().equals(JsonNodeType.ARRAY)) {
			result.invalidType(location, key, "array", value);
		} else {
			an = (ArrayNode) value;
		}
		return an;
	}

	public ObjectNode getObject(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		final JsonNode value = node.get(key);
		ObjectNode on = null;
		if (value == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!value.getNodeType().equals(JsonNodeType.OBJECT)) {
			result.invalidType(location, key, "object", value);
			if (required) {
				result.invalid();
			}
		} else {
			on = (ObjectNode) value;
		}
		return on;
	}

	public Double getDouble(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		Double value = null;
		final JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
			value = v.asDouble();
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "double", node);
		}
		return value;
	}

	public Number getNumber(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		Number value = null;
		final JsonNode v = node.get(key);
		if (v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
			value = v.numberValue();
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "number", node);
		}
		return value;
	}

	public Integer getInteger(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		Integer value = null;
		final JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (v.getNodeType().equals(JsonNodeType.NUMBER)) {
			value = v.intValue();
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "integer", node);
		}
		return value;
	}

	public String getString(final String key, final ObjectNode node, final boolean required, final String location,
			final ParseResult result) {
		String value = null;
		final JsonNode v = node.get(key);
		if (node == null || v == null) {
			if (required) {
				result.missing(location, key);
				result.invalid();
			}
		} else if (!v.isValueNode()) {
			result.invalidType(location, key, "string", node);
		} else {
			value = v.asText();
		}
		return value;
	}

	public Set<String> getKeys(final ObjectNode node) {
		final Set<String> keys = new LinkedHashSet<>();
		if (node == null) {
			return keys;
		}

		final Iterator<String> it = node.fieldNames();
		while (it.hasNext()) {
			keys.add(it.next());
		}

		return keys;
	}

	static class ParseResult {
		private boolean valid = true;
		private Map<Location, JsonNode> extra = new HashMap<>();
		private Map<Location, JsonNode> unsupported = new HashMap<>();
		private Map<Location, String> invalidType = new HashMap<>();
		private List<Location> missing = new ArrayList<>();

		public void unsupported(final String location, final String key, final JsonNode value) {
			unsupported.put(new Location(location, key), value);
		}

		public void extra(final String location, final String key, final JsonNode value) {
			extra.put(new Location(location, key), value);
		}

		public void missing(final String location, final String key) {
			missing.add(new Location(location, key));
		}

		public void invalidType(final String location, final String key, final String expectedType,
				final JsonNode value) {
			invalidType.put(new Location(location, key), expectedType);
		}

		public void invalid() {
			valid = false;
		}

		public Map<Location, JsonNode> getUnsupported() {
			return unsupported;
		}

		public void setUnsupported(final Map<Location, JsonNode> unsupported) {
			this.unsupported = unsupported;
		}

		public boolean isValid() {
			return valid;
		}

		public void setValid(final boolean valid) {
			this.valid = valid;
		}

		public Map<Location, JsonNode> getExtra() {
			return extra;
		}

		public void setExtra(final Map<Location, JsonNode> extra) {
			this.extra = extra;
		}

		public Map<Location, String> getInvalidType() {
			return invalidType;
		}

		public void setInvalidType(final Map<Location, String> invalidType) {
			this.invalidType = invalidType;
		}

		public List<Location> getMissing() {
			return missing;
		}

		public void setMissing(final List<Location> missing) {
			this.missing = missing;
		}

		public List<String> getMessages() {
			final List<String> messages = new ArrayList<>();
			for (final Location l : extra.keySet()) {
				final String location = l.location.equals("") ? "" : l.location + ".";
				final String message = "attribute " + location + l.key + " is unexpected";
				messages.add(message);
			}
			for (final Location l : invalidType.keySet()) {
				final String location = l.location.equals("") ? "" : l.location + ".";
				final String message = "attribute " + location + l.key + " is not of type `" + invalidType.get(l) + "`";
				messages.add(message);
			}
			for (final Location l : missing) {
				final String location = l.location.equals("") ? "" : l.location + ".";
				final String message = "attribute " + location + l.key + " is missing";
				messages.add(message);
			}
			for (final Location l : unsupported.keySet()) {
				final String location = l.location.equals("") ? "" : l.location + ".";
				final String message = "attribute " + location + l.key + " is unsupported";
				messages.add(message);
			}
			return messages;
		}
	}

	static class Location {
		private final String location;

		@Override
		public boolean equals(final Object o) {
			if (this == o) {
				return true;
			}
			if (!(o instanceof Location)) {
				return false;
			}

			final Location location1 = (Location) o;

			if (location != null ? !location.equals(location1.location) : location1.location != null) {
				return false;
			}
			return !(key != null ? !key.equals(location1.key) : location1.key != null);

		}

		@Override
		public int hashCode() {
			int result = location != null ? location.hashCode() : 0;
			result = 31 * result + (key != null ? key.hashCode() : 0);
			return result;
		}

		private final String key;

		public Location(final String location, final String key) {
			this.location = location;
			this.key = key;
		}
	}
}
