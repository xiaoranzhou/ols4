import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.jena.vocabulary.RDF;

import java.io.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class JSON2CSV {

    static Gson gson = new Gson();

    public static void main(String[] args) throws IOException {

        Options options = new Options();

        Option input = new Option(null, "input", true, "ontologies JSON input filename");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option(null, "outDir", true, "output CSV folder path");
        output.setRequired(true);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("json2csv", options);

            System.exit(1);
            return;
        }

        String inputFilePath = cmd.getOptionValue("input");
        String outputFilePath = cmd.getOptionValue("outDir");

        JsonReader reader = new JsonReader(new InputStreamReader(new FileInputStream(inputFilePath)));

        reader.beginObject();

        while(reader.peek() != JsonToken.END_OBJECT) {

            String name = reader.nextName();

            if (name.equals("ontologies")) {

                reader.beginArray();

                while(reader.peek() != JsonToken.END_ARRAY) {
                    JsonOntology ontology = gson.fromJson(reader, JsonOntology.class);

                    NodesAndPropertiesExtractor.Result nodesAndProps =
                            NodesAndPropertiesExtractor.extractNodesAndProperties(ontology);

                    //EdgesExtractor.Result edges =
                    //EdgesExtractor.extractEdges(ontology, nodesAndProps);

                    writeOntology(ontology, outputFilePath);

                    writeClasses(ontology, outputFilePath, nodesAndProps);
                    writeProperties(ontology, outputFilePath, nodesAndProps);
                    writeIndividuals(ontology, outputFilePath, nodesAndProps);

                    writeEdges(ontology, outputFilePath, nodesAndProps);
                }

                reader.endArray();

            } else {

                reader.skipValue();

            }
        }

        reader.endObject();
        reader.close();
    }

    public static void writeOntology(JsonOntology ontology, String outPath) throws IOException {

        List<String> properties = new ArrayList<String>(ontology.ontologyProperties.keySet());

        List<String> csvHeader = new ArrayList<>();
        csvHeader.add("id:ID");
        csvHeader.add(":LABEL");
        csvHeader.add("config");
        csvHeader.add("propertyLabels");
        csvHeader.addAll(propertyHeaders(properties));

        String outName = outPath + "/" + (String) ontology.ontologyConfig.get("id") + "_ontologies.csv";

        CSVPrinter printer = CSVFormat.POSTGRESQL_CSV.withHeader(csvHeader.toArray(new String[0])).print(
                new File(outName), Charset.defaultCharset());

        String[] row = new String[csvHeader.size()];
        int n = 0;

        row[n++] = (String) ontology.ontologyConfig.get("id");
        row[n++] = "Ontology";
        row[n++] = gson.toJson(ontology.ontologyConfig);
        row[n++] = gson.toJson(ontology.ontologyProperties.get("propertyLabels"));

        for (String column : properties) {
            if(column.equals("propertyLabels"))
                continue;
            row[n++] = getValue(ontology.ontologyProperties, column);
        }

        printer.printRecord(row);
        printer.close(true);
    }


    public static void writeClasses(JsonOntology ontology, String outPath, NodesAndPropertiesExtractor.Result nodesAndProps) throws IOException {

        String id = (String) ontology.ontologyConfig.get("id");

        String outName = outPath + "/" + id + "_classes.csv";

        List<String> properties = new ArrayList<String>(nodesAndProps.allClassProperties);

        List<String> csvHeader = new ArrayList<>();
        csvHeader.add("id:ID");
        csvHeader.add(":LABEL");
        csvHeader.add("ontology_id");
        csvHeader.add("uri");
        csvHeader.add("propertyLabels");
        csvHeader.addAll(propertyHeaders(properties));

        CSVPrinter printer = CSVFormat.POSTGRESQL_CSV.withHeader(csvHeader.toArray(new String[0])).print(
                new File(outName), Charset.defaultCharset());

        for (Map<String, Object> _class : ontology.classes) {

            String[] row = new String[csvHeader.size()];
            int n = 0;

            row[n++] = id + "+" + (String) _class.get("uri");
            row[n++] = "OwlClass";
            row[n++] = id;
            row[n++] = (String) _class.get("uri");
            row[n++] = gson.toJson(_class.get("propertyLabels"));

            for (String column : properties) {
                row[n++] = getValue(_class, column);
            }

            printer.printRecord(row);
        }

        printer.close(true);
    }


    public static void writeProperties(JsonOntology ontology, String outPath, NodesAndPropertiesExtractor.Result nodesAndProps) throws IOException {

        String id = (String) ontology.ontologyConfig.get("id");

        String outName = outPath + "/" + id + "_properties.csv";

        List<String> properties = new ArrayList<String>(nodesAndProps.allPropertyProperties);

        List<String> csvHeader = new ArrayList<>();
        csvHeader.add("id:ID");
        csvHeader.add(":LABEL");
        csvHeader.add("ontology_id");
        csvHeader.add("uri");
        csvHeader.add("propertyLabels");
        csvHeader.addAll(propertyHeaders(properties));

        CSVPrinter printer = CSVFormat.POSTGRESQL_CSV.withHeader(csvHeader.toArray(new String[0])).print(
                new File(outName), Charset.defaultCharset());

        for (Map<String, Object> _property : ontology.properties) {

            String[] row = new String[csvHeader.size()];
            int n = 0;

            row[n++] = id + "+" + (String) _property.get("uri");
            row[n++] = "OwlProperty";
            row[n++] = id;
            row[n++] = (String) _property.get("uri");
            row[n++] = gson.toJson(_property.get("propertyLabels"));

            for (String column : properties) {
                row[n++] = getValue(_property, column);
            }

            printer.printRecord(row);
        }

        printer.close(true);
    }

    public static void writeIndividuals(JsonOntology ontology, String outPath, NodesAndPropertiesExtractor.Result nodesAndProps) throws IOException {

        String id = (String) ontology.ontologyConfig.get("id");

        String outName = outPath + "/" + id + "_individuals.csv";

        List<String> properties = new ArrayList<String>(nodesAndProps.allIndividualProperties);

        List<String> csvHeader = new ArrayList<>();
        csvHeader.add("id:ID");
        csvHeader.add(":LABEL");
        csvHeader.add("ontology_id");
        csvHeader.add("uri");
        csvHeader.add("propertyLabels");
        csvHeader.addAll(propertyHeaders(properties));

        CSVPrinter printer = CSVFormat.POSTGRESQL_CSV.withHeader(csvHeader.toArray(new String[0])).print(
                new File(outName), Charset.defaultCharset());

        for (Map<String, Object> _individual : ontology.individuals) {

            String[] row = new String[csvHeader.size()];
            int n = 0;

            row[n++] = id + "+" + (String) _individual.get("uri");
            row[n++] = "OwlIndividual";
            row[n++] = id;
            row[n++] = (String) _individual.get("uri");
            row[n++] = gson.toJson(_individual.get("propertyLabels"));

            for (String column : properties) {
                row[n++] = getValue(_individual, column);
            }

            printer.printRecord(row);
        }

        printer.close(true);
    }


    public static void writeEdges(JsonOntology ontology, String outPath, NodesAndPropertiesExtractor.Result nodesAndProps) throws IOException {

        String ontologyId = (String) ontology.ontologyConfig.get("id");

        String outName = outPath + "/" + ontologyId + "_edges.csv";

        List<String> properties = new ArrayList<String>(nodesAndProps.allEdgeProperties);

        List<String> csvHeader = new ArrayList<>();
        csvHeader.add(":START_ID");
        csvHeader.add(":TYPE");
        csvHeader.add(":END_ID");
        csvHeader.add("propertyLabels");
        csvHeader.addAll(propertyHeaders(properties));

        CSVPrinter printer = CSVFormat.POSTGRESQL_CSV.withHeader(csvHeader.toArray(new String[0])).print(
                new File(outName), Charset.defaultCharset());

        for (Map<String, Object> _class : ontology.classes) {

            for (String predicate : _class.keySet()) {

                if (predicate.equals("uri"))
                    continue;

                Object value = _class.get(predicate);

                List<Object> values;

                if(value instanceof List) {
                    values = (List<Object>) value;
                } else {
                    values = new ArrayList<>();
                    values.add(value);
                }

                for(Object v : values) {

                    if (v instanceof Map) {
                        // maybe axiom
                        Map<String, Object> mapValue = (Map<String, Object>) v;
                        if (mapValue.containsKey("value") && !mapValue.containsKey("lang")) {
                            // axiom
                            Object axiomValue = mapValue.get("value");
                            assert (axiomValue instanceof String);
                            if (nodesAndProps.allNodes.contains(axiomValue)) {
                                printEdge(printer, properties, ontologyId, _class, predicate, axiomValue, mapValue);
                            }
                        }
                    } else if (v instanceof String) {
                        if (nodesAndProps.allNodes.contains((String) v)) {
                            printEdge(printer, properties, ontologyId, _class, predicate, v, new HashMap<>());
                        }
                    } else {
                        assert(false);
                    }

                }

            }
        }

        for (Map<String, Object> property : ontology.properties) {

            for (String predicate : property.keySet()) {

                if (predicate.equals("uri"))
                    continue;

                Object value = property.get(predicate);

                List<Object> values;

                if(value instanceof List) {
                    values = (List<Object>) value;
                } else {
                    values = new ArrayList<>();
                    values.add(value);
                }

                for(Object v : values) {

                    if (v instanceof Map) {
                        // maybe axiom
                        Map<String, Object> mapValue = (Map<String, Object>) v;
                        if (mapValue.containsKey("value") && !mapValue.containsKey("lang")) {
                            // axiom
                            Object axiomValue = mapValue.get("value");
                            assert (axiomValue instanceof String);
                            if (nodesAndProps.allNodes.contains(axiomValue)) {
                                printEdge(printer, properties, ontologyId, property, predicate, axiomValue, mapValue);
                            }
                        }
                    } else if (v instanceof String) {
                        if (nodesAndProps.allNodes.contains((String) v)) {
                            printEdge(printer, properties, ontologyId, property, predicate, v, new HashMap<>());
                        }
                    } else {
                        assert(false);
                    }

                }

            }
        }

        for (Map<String, Object> individual : ontology.individuals) {

            for (String predicate : individual.keySet()) {

                if (predicate.equals("uri"))
                    continue;

                Object value = individual.get(predicate);

                List<Object> values;

                if(value instanceof List) {
                    values = (List<Object>) value;
                } else {
                    values = new ArrayList<>();
                    values.add(value);
                }

                for(Object v : values) {

                    if (v instanceof Map) {
                        // maybe axiom
                        Map<String, Object> mapValue = (Map<String, Object>) v;
                        if (mapValue.containsKey("value") && !mapValue.containsKey("lang")) {
                            // axiom
                            Object axiomValue = mapValue.get("value");
                            assert (axiomValue instanceof String);
                            if (nodesAndProps.allNodes.contains(axiomValue)) {
                                printEdge(printer, properties, ontologyId, individual, predicate, axiomValue, mapValue);
                            }
                        }
                    } else if (v instanceof String) {
                        if (nodesAndProps.allNodes.contains((String) v)) {
                            printEdge(printer, properties, ontologyId, individual, predicate, v, new HashMap<>());
                        }
                    } else {
                        assert(false);
                    }

                }

            }
        }

        printer.close(true);
    }


    private static void printEdge(CSVPrinter printer, List<String> properties, String ontologyId, Map<String,Object> a, String predicate, Object bUri, Map<String,Object> edgeProps) throws IOException {

        String[] row = new String[4 + properties.size()];
        int n = 0;

        row[n++] = ontologyId + "+" + (String) a.get("uri");
        row[n++] = predicate;
        row[n++] = ontologyId + "+" + (String) bUri;
        row[n++] = gson.toJson(edgeProps.get("propertyLabels"));

        for (String column : properties) {

            // anything else are properties on the edge itself (from axioms)
            //
            row[n++] = getValue(edgeProps, column);
        }

        printer.printRecord(row);
    }

    private static String valueToCsv(Object value) {

	if(value instanceof List) {
		String out = "";
		for(Object val : (List<Object>) value)  {
			if(out.length() > 0) {
				out += "|";
			}
			out += valueToCsv(val);
		}
		return out;
	}

	if (value == null) {
		return "";
	}

	if (value instanceof String) {
		return replaceNeo4jSpecialCharsValue((String) value);
	}

	if(value instanceof Map) {

		// could be an axiom or a langString, but we are writing the value
        // itself as a property directly in this case; the rest of the axiom 
        // properties or localized strings go in the axiom+ field or lang+ fields

		Map<String, Object> mapValue = (Map<String, Object>) value;

		if (mapValue.containsKey("value")) {
			Object val = mapValue.get("value");
            return valueToCsv(val);
		}
	}

	return replaceNeo4jSpecialCharsValue(gson.toJson(value));
    }


    private static String replaceNeo4jSpecialCharsValue(String val) {
	return val.replace("|", "+");
    }

    private static List<String> propertyHeaders(List<String> uris) {
        List<String> headers = new ArrayList<>();

        for(String uri : uris) {
            
	    if(uri.equals("propertyLabels"))
		    continue;

            headers.add(uri.replace(":", "__") + ":string[]");
        }

        return headers;
    }

    private static String getValue(Map<String,Object> properties, String column) {

        //System.out.println("get " + column);
        //System.out.println(properties.get(column));

        if(column.startsWith("axiom+")) {

                String predicate = column.substring(6);
                Object axiom = properties.get(predicate);

                return axiom != null ? gson.toJson(axiom) : "";
            }

            if(column.indexOf('+') != -1) {
                String lang = column.substring(0, column.indexOf('+'));
                String predicate = column.substring(column.indexOf('+')+1);

                return valueToCsv(getLocalizedValue(properties, predicate, lang));
            }

            Object value = properties.get(column);

            return valueToCsv(value);
    }


    private static Object getLocalizedValue(Map<String,Object> properties, String predicate, String lang) {

            Object values = properties.get(predicate);

            if(values == null)
                return null;

            if(! (values instanceof List)) {
                List<Object> valuesList = new ArrayList<>();
                valuesList.add(values);
                values = valuesList;
            }

            for(Object value : ((List<Object>) values)) {
                if(value instanceof Map) {
                    Map<String, Object> mapValue = (Map<String, Object>) value;
                    String valueLang = (String)mapValue.get("lang");
                    if(valueLang != null && valueLang.equals(lang)) {
                        return valueToCsv(mapValue.get("value"));
                    }
                }
            }

            return null;
    }

}

