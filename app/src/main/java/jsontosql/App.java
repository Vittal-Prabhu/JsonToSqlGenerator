/*
 * Java application to convert JSON file to Sql query
 */
package jsontosql;

import java.io.File;
import java.nio.file.Files;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashSet;
import java.util.Set;

class JsonToSql {
    private StringBuilder sqlQuery; //Mutable field used build sql query
    private Set<String> usedOperators; // Track operators used for each field

    public JsonToSql() {
        this.sqlQuery = new StringBuilder();
        this.usedOperators = new HashSet<>();
    }
    
    // Function to build query from columns field fetched from JSON as JSONArray
    public void buildQuery(JSONArray columns) {
        sqlQuery.append("SELECT * FROM your_table WHERE ");

        for (int i = 0; i < columns.length(); i++) {
            JSONObject column = columns.getJSONObject(i);
            String operator = column.getString("operator");
            String fieldName = column.getString("fieldName");
            Object fieldValue = column.get("fieldValue");

            checkDuplicateOperator(operator, fieldName);
            appendCondition(operator, fieldName, fieldValue);

            if (i < columns.length() - 1) {
                sqlQuery.append(" AND ");
            }
        }
    }

    // Function to validate that same operator is not used same field irrespective of the field value
    private void checkDuplicateOperator(String operator, String fieldName) {
        if (!usedOperators.add(fieldName + operator.toUpperCase())) {
            throw new IllegalArgumentException("Duplicate operator '" + operator + "' for field '" + fieldName + "'");
        }
    }

    // Function to append condition based on operator, fieldName and fieldValue
    private void appendCondition(String operator, String fieldName, Object fieldValue) {
        switch (operator.toUpperCase()) {
            case "IN":
                if (fieldValue instanceof JSONArray) {
                    sqlQuery.append(fieldName).append(" IN (")
                            .append(buildInConditionValues((JSONArray) fieldValue)).append(")");
                } else if (fieldValue instanceof String) {
                    sqlQuery.append(fieldName).append(" IN ('").append(fieldValue).append("')");
                }
                break;
            case "LIKE":
                sqlQuery.append(fieldName).append(" LIKE '").append(fieldValue).append("'");
                break;
            case "EQUAL":
                sqlQuery.append(fieldName).append(" = '").append(fieldValue).append("'");
                break;
            case ">":
                sqlQuery.append(fieldName).append(" > '").append(fieldValue).append("'");
            break;
            case "<":
                sqlQuery.append(fieldName).append(" < '").append(fieldValue).append("'");
                break;
            case ">=":
                sqlQuery.append(fieldName).append(" >= '").append(fieldValue).append("'");
                break;
            case "<=":
                sqlQuery.append(fieldName).append(" <= '").append(fieldValue).append("'");
                break;
            case "!=":
                sqlQuery.append(fieldName).append(" != '").append(fieldValue).append("'");
                break;
            case "<>":
                sqlQuery.append(fieldName).append(" <> '").append(fieldValue).append("'");
                break;
            case "BETWEEN":
                if (fieldValue instanceof JSONObject) {
                    JSONObject betweenValues = (JSONObject) fieldValue;
                    if (betweenValues.has("min") && betweenValues.has("max")) {
                        sqlQuery.append(fieldName).append(" BETWEEN '")
                                .append(betweenValues.getString("min"))
                                .append("' AND '")
                                .append(betweenValues.getString("max"))
                                .append("'");
                    } else {
                        // Handle invalid BETWEEN condition
                        throw new IllegalArgumentException("Invalid BETWEEN condition: " + fieldValue);
                    }
                } else {
                    // Handle invalid BETWEEN condition
                    throw new IllegalArgumentException("Invalid BETWEEN TYPE: " + fieldValue);
                }
                break;
            default:
                // Handle unsupported operators or throw an exception
                throw new IllegalArgumentException("Unsupported operator: " + operator);
        }
    }

    // Function to build condition values for IN operator when fieldValue is of type array
    private String buildInConditionValues(JSONArray array) {
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < array.length(); i++) {
            values.append("'").append(array.getString(i)).append("'");
            if (i < array.length() - 1) {
                values.append(", ");
            }
        }
        return values.toString();
    }

    // Function converting sqlQuery to String
    public String getSqlQuery() {
        return sqlQuery.toString();
    }
}

public class App {
    public static void main(String[] args) {
        try {
            // Read JSON from file stored in resources directory
            URL fileURL = App.class.getClassLoader().getResource("testJson1.json");
            File jsonFile = new File(fileURL.getFile());
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()));

            // Parse JSON
            JSONObject jsonObject = new JSONObject(jsonContent);
            JSONArray columns = jsonObject.getJSONArray("columns");

            // Build SQL query
            JsonToSql jsonToSql = new JsonToSql();
            jsonToSql.buildQuery(columns);
            String sqlQuery = jsonToSql.getSqlQuery();

            // Output SQL query
            System.out.println("Generated SQL Query:\n" + sqlQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}