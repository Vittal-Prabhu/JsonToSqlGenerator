/*
 * Java application to convert JSON file to Sql query
 * Application further optimized in this code to support qurying from multiple tables - (Solution to Problem 2) 
 */
package jsontosql;

import java.io.File;
import java.nio.file.Files;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.ResourceBundle;

class JsonToSql {
    private StringBuilder sqlQuery; //Mutable field used build sql query
    private Set<String> usedOperators; // Track operators used for each field
    private Set<String> tableNameSet; //Set of table names
    private Set<String> joinTableNameSet; // Set of table names that are joined
    private ResourceBundle rd ; // Config params

    public JsonToSql() {
        this.sqlQuery = new StringBuilder();
        this.usedOperators = new HashSet<>();
        this.tableNameSet = new HashSet<>();
        this.joinTableNameSet = new HashSet<>();
        this.rd = ResourceBundle.getBundle("config");
    }     
    
    // Function to build query from columns field fetched from JSON as JSONArray
    public void buildQuery(JSONObject jsonQuery) {
        sqlQuery.append("SELECT * FROM ");

        JSONArray tables = jsonQuery.getJSONArray(rd.getString("tables"));
        for (int i = 0; i < tables.length(); i++) {
            JSONObject table = tables.getJSONObject(i);
            String tableName = table.getString(rd.getString("tableName"));
            tableNameSet.add(tableName);

            if(i == 0){
                sqlQuery.append(tableName);
            }

            if (table.has(rd.getString("joinCondition"))) {
                JSONObject joinCondition = table.getJSONObject(rd.getString("joinCondition"));
                buildJoinCondition(tableName, joinCondition);
            }

            if (i < tables.length() - 1) {
                sqlQuery.append(" ");
            }
        }

        if (jsonQuery.has(rd.getString("whereCondition"))) {
            JSONObject whereCondition = jsonQuery.getJSONObject(rd.getString("whereCondition"));
            buildWhereCondition(whereCondition);
        }

        // Check if the table names specified in the join condition are valid
        if(joinTableNameSet.size()>0){
            if(!tableNameSet.containsAll(joinTableNameSet)){
                throw new IllegalArgumentException("Incorrect join table specified");
            }
        }
    }


    // Function to build join condition
    private void buildJoinCondition(String tableName, JSONObject joinCondition) {
        sqlQuery.append("\nJOIN ");
        String joinTable = joinCondition.getString(rd.getString("join"));
        joinTableNameSet.add(joinTable);
        sqlQuery.append( joinTable);
        sqlQuery.append(" ON ");
        JSONArray conditions = joinCondition.getJSONArray(rd.getString("conditions"));

        for (int i = 0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            String fieldName = condition.getString(rd.getString("fieldName"));
            String operator = condition.getString(rd.getString("operator"));
            String fieldValue = condition.getString(rd.getString("fieldValue"));

            sqlQuery.append(tableName).append(".").append(fieldName).append(" ").append(operator).append(" ").append(joinTable).append(".").append(fieldValue);

            if (i < conditions.length() - 1) {
                sqlQuery.append(" AND ");
            }
        }
    }


    // Function to build 'WHERE' condition
    private void buildWhereCondition(JSONObject whereCondition) {
        sqlQuery.append("\nWHERE ");
        JSONArray conditions = whereCondition.getJSONArray(rd.getString("conditions"));

        for (int i = 0; i < conditions.length(); i++) {
            JSONObject condition = conditions.getJSONObject(i);
            String fieldName = condition.getString(rd.getString("fieldName"));
            String operator = condition.getString(rd.getString("operator"));
            String fieldValue = condition.getString(rd.getString("fieldValue"));

            checkDuplicateOperator(operator, fieldName);
            appendCondition(operator, fieldName, fieldValue);

            if (i < conditions.length() - 1) {
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
            case "=":
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
                    String min = rd.getString("min");
                    String max = rd.getString("max");
                    if (betweenValues.has(min) && betweenValues.has(max)) {
                        sqlQuery.append(fieldName).append(" BETWEEN '")
                                .append(betweenValues.getString(min))
                                .append("' AND '")
                                .append(betweenValues.getString(max))
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
            ResourceBundle rd = ResourceBundle.getBundle("config");
            URL fileURL = App.class.getClassLoader().getResource(rd.getString("jsonFileName"));
            File jsonFile = new File(fileURL.getFile());
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()));

            // Parse JSON
            JSONObject jsonObject = new JSONObject(jsonContent);

            // Build SQL query
            JsonToSql jsonToSql = new JsonToSql();
            jsonToSql.buildQuery(jsonObject);
            String sqlQuery = jsonToSql.getSqlQuery();

            // Output SQL query
            System.out.println("Generated SQL Query:\n \n" + sqlQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}