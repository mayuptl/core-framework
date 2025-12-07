package core.json;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.config.CoreConfigReader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class for reading test data from JSON files.
 *
 * <p>This class is designed to handle JSON files that contain **arrays of test case data**.<br>
 * It uses the test case ID (TCID), defined by the property {@code JSON_DATA_TCID_KEY}, as the<br>
 * primary key for data retrieval, enabling robust, data-driven test automation.</p>
 *
 * <p>It relies on **Jackson Databind** for mapping JSON content to Java collections,<br>
 * primarily using {@code Map<String, String>} for individual data sets.</p>
 *
 * <h2>Key Methods Summary:</h2>
 * <ul>
 * <li>{@code getJsonInput}: Retrieves a single, specific data entry based on a given test case name, formatted for a TestNG DataProvider.</li>
 * <li>{@code getJsonInputs}: Retrieves the entire list of data entries from the JSON array, useful for iterating through all data rows.</li>
 * <li>{@code readAllJson}: Reads the entire JSON file and indexes the data into a {@code Map<TCID, DataMap>} for efficient lookup by test case name.</li>
 * </ul>
 */
public class CoreJsonReader {
    /** Private constructor for a utility class; all methods are static. */
    private CoreJsonReader() { }
    /**
     * Key used within the JSON data objects to identify the test case name/ID (TCID).
     *
     * <p>The value for this constant is dynamically retrieved from the framework configuration<br>
     * via {@code CoreConfigReader.getStrProp("json.input.data.key")} during class loading.<br>
     * This key is mandatory for methods that look up data by test case name,<br>
     * such as {@code getJsonInput} and {@code readAllJson}.</p>
     */
    private static final String TCID_KEY = CoreConfigReader.getStrProp("json.input.data.key");

    /**
     * Retrieves the input data for a specific test case ID from a JSON array file.
     *
     * <p>The method searches the JSON array for a single object where the key defined by {@code TCID_KEY}<br>
     * matches the provided {@code testCaseName}.</p>
     *
     * @param jsonFilePath The full path to the JSON data file. The JSON must be an array of objects.
     * @param testCaseName The unique ID/Name of the test case to look for (e.g., "TC_001_Login").
     * @return A {@code Object[][]} containing the single matching data HashMap. This specific<br>
     * structure (one row, one column) is formatted for **direct use as a TestNG DataProvider**,<br>
     * allowing the data map to be passed as the first argument to the test method.
     * @throws RuntimeException if the file is not found, cannot be read, the JSON is invalid,<br>
     * or the specified {@code testCaseName} is not found in the data, wrapping the original {@link IOException}.
     *
     * <h4>Example Usage in TestNG (DataProvider Pattern)</h4>
     * This method is designed to be called from a **DataProvider** which supplies the test method<br>
     * name (or TCID) via {@code ITestContext} or {@code ITestResult}:
     * <pre>{@code
     * import static core.json.CoreJsonReader.*;
     * public class LoginTest {
     * // 1. Data Provider calls the static getJsonInput() method
     * @DataProvider(name = "SingleCaseData")
     * public Object[][] getTestData(ITestContext context) {
     * // Fetch the name of the test method currently being executed (often used as the TCID)
     * String testCaseName = context.getCurrentXmlTest().getName();
     * // OR you can hardcode the TCID for debugging:
     * // String testCaseName = "TC_001_Login_Valid";
     *
     * // Correct static call, returns Object[1][1] containing the HashMap
     * Object[][] inputData = JsonReader.getJsonInput("data/loginData.json", testCaseName);
     * return inputData;
     * }
     *
     * // 2. Test method receives the HashMap
     * @Test(dataProvider = "SingleCaseData")
     * public void TC_001_Login_Valid(Map<String, String> input) {
     * // Map<String, String> input is the HashMap returned by getJsonInput()
     * String username = input.get("username");
     * String password = input.get("password");
     * // ... perform test actions ...
     * }
     * }
     * }</pre>
     * <h4>Example JSON Structure (Required for this method):</h4>
     * <pre>{@code
     * [
     * {
     * "testCaseName": "TC_001_Login_Valid",
     * "username": "adminUser",
     * "password": "securePassword123"
     * },
     * // ... other test cases ...
     * ]
     * }</pre>
     */
    public static Object[][] getJsonInput(String jsonFilePath, String testCaseName) {
        String jsonCont;
        try {
            // Read file content
            jsonCont = FileUtils.readFileToString(new File(jsonFilePath), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            // Provide a clear message for file not found
            throw new RuntimeException("Json file not found at location: " + jsonFilePath, e);
        } catch (IOException e) {
            // Handle other IO errors (e.g., permission issues)
            throw new RuntimeException("Error reading JSON file at: " + jsonFilePath, e);
        }
        ObjectMapper mapper = new ObjectMapper();
        try {
            // Deserialize JSON string into a List of HashMaps
            List<HashMap<String, String>> dataList =
                    mapper.readValue(jsonCont, new TypeReference<List<HashMap<String, String>>>() {
                    });

            // Iterate and find the matching entry
            for (HashMap<String, String> entry : dataList) {
                // Use the internal constant key (TCID_KEY) for the search
                if (entry.containsKey(TCID_KEY) && entry.get(TCID_KEY).equals(testCaseName))
                {
                    // Create a 2D array: [1 row] [1 column] to hold the single HashMap
                    Object[][] dataArray = new Object[1][1];
                    dataArray[0][0] = entry;
                    return dataArray;
                }
            }
            // Handle the case where the ID is not found in the JSON data
            throw new RuntimeException("Test case ID '" + testCaseName + "' not found in JSON data using key: " + TCID_KEY); // Minor clarification
        } catch (IOException e) {
            // Catch Jackson deserialization errors (invalid JSON format)
            throw new RuntimeException("Error parsing JSON content from: " + jsonFilePath, e);
        }
    }

    /**
     * Reads all objects from a JSON array file and returns them as a List of HashMaps.
     *
     * <p>This method is highly useful for DataProviders that need to iterate through all rows<br>
     * or a specific subset of rows in the JSON file, as it returns the raw, parsed data<br>
     * structure directly.</p>
     *
     * @param jsonFilePath The full path to the JSON data file. The JSON must be an array of objects.
     * @return A {@code List<HashMap<String, String>>}, where each HashMap represents<br>
     * one test data entry (data row) from the JSON array.
     * @throws IOException if a JSON parsing error occurs (e.g., content is invalid JSON).
     * @throws RuntimeException if the file is not found or cannot be read during the initial file operation, wrapping the {@link IOException}.
     *
     * <h4>Example Usage in a TestNG DataProvider:</h4>
     * <p>The DataProvider must process the returned {@code List} and convert the selected elements<br>
     * into an {@code Object[]} or {@code Object[][]} suitable for TestNG injection.</p>
     * <pre>{@code
     * import static core.json.CoreJsonReader.*;
     * public class DataDrivenTest {
     * @DataProvider(name = "getAllData")
     * public Object[] getData() throws IOException {
     * // Read all rows into a List using the static method
     * List<HashMap<String, String>> data = JsonReader.getJsonInputs("path/to/data.json");
     * // The DataProvider must return an Object[] or Object[][]
     * // Example 1: To execute the test case using ALL rows in the file
     * return data.toArray();
     * // Example 2: To execute the test case 2 times (e.g., first two rows)
     * // return new Object[] {data.get(0), data.get(1)};
     * }
     * @Test(dataProvider = "getAllData")
     * public void myTest(HashMap<String, String> input) {
     * // TestNG passes each HashMap as the argument 'input' for each test iteration.
     * String username = input.get("username");
     * String password = input.get("password");
     * // ... perform actions with username and password
     * }
     * }
     * }</pre>
     * <h4>Expected JSON Structure:</h4>
     * <p>The JSON file must contain an array of objects, similar to the following structure:</p>
     * <pre>{@code
     * [
     * {
     * "username" : "Admin",
     * "password" : "admin123"
     * },
     * {
     * "username" :"Mayur1",
     * "password" :"Mayur01@2025"
     * }
     * ]
     * }</pre>
     */
    public static List<HashMap<String, String>> getJsonInputs(String jsonFilePath) throws IOException {
        String jsonCont;
        try {
            jsonCont = FileUtils.readFileToString(new File(jsonFilePath), StandardCharsets.UTF_8);
        } catch (FileNotFoundException e) {
            // Provide a clear message for file not found
            throw new RuntimeException("Json file not found at location: " + jsonFilePath, e);
        } catch (IOException e) {
            // Handle other IO errors (e.g., permission issues)
            throw new RuntimeException("Error reading JSON file at: " + jsonFilePath, e);
        }
        // String to List of HashMap- Jackson Databind
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(jsonCont, new TypeReference<List<HashMap<String, String>>>() {
        });
    }

    /**
     * Reads the content of a JSON file, which is expected to contain a JSON array
     * of key-value maps (objects), and transforms it into a Map keyed by a specified
     * test case ID (TCID).
     *
     * <p>Each element in the input JSON array must be an object containing the key<br>
     * defined by {@code TCID_KEY} (e.g., "testCaseName") to be used as the primary<br>
     * key in the output map.</p>
     *
     * @param jsonFilePath The absolute or relative path to the JSON file to be read.
     * @return A {@code Map<String, Map<String, String>>} where the outer map's key is<br>
     * the value of {@code TCID_KEY}, and the inner map<br>
     * contains all the key-value pairs from that JSON object.
     * @throws RuntimeException if an {@link IOException} occurs during file reading<br>
     * or JSON parsing, wrapping the original exception for context.
     *
     * <h4>Example Usage in TestNG (Output Consumption)</h4>
     * The resulting Map is typically loaded once per class and then used in {@code @BeforeMethod}<br>
     * to fetch the specific data for the currently executing test method name.<br>
     * <pre>
     * import static core.json.CoreJsonReader.*;<br>
     * public class LoginTest {<br>
     * // Map containing all data keyed by TCID<br>
     * private static Map&lt;String, Map&lt;String, String&gt;&gt; testData;<br>
     * // Map for the specific test case's data<br>
     * private Map &lt;String, String&gt; input;<br>
     *
     * {@code @BeforeClass}<br>
     * public void setupClass() {<br>
     * String jsonFilePath = "path/to/jsonFile";<br>
     * testData = JsonReader.readAllJson(jsonFilePath); // Loads all data once<br>
     * }<br>
     * {@code @BeforeMethod}<br>
     * public void setupMethod(ITestResult result) {<br>
     * // Gets the specific data map based on the test method name<br>
     * String testCaseName = result.getMethod().getMethodName();<br>
     * input = testData.get(testCaseName);<br>
     * }<br>
     * {@code @Test}<br>
     * public void Test_001_Login_ValidUser() {<br>
     * // Test case uses the specific input map<br>
     * String username = input.get("username");<br>
     * String password = input.get("password");<br>
     * // ... perform actions with username and password<br>
     * }<br>
     * }<br>
     * </pre>
     * <h4>Example JSON Structure (Input)</h4>
     * The JSON file content should look like this, where "TestCaseName" is the value<br>
     * held by the {@code TCID_KEY} constant:<br>
     * <pre>
     * [<br>
     * {<br>
     * "TestCaseName": "Test_001_Login_ValidUser",<br>
     * "username": "user123",<br>
     * "password": "Password1"<br>
     * },<br>
     * {<br>
     * "TestCaseName": "Test_002_Login_InvalidPassword",<br>
     * "username": "user123",<br>
     * "password": "wrong password"<br>
     * }<br>
     * ]<br>
     * </pre>
     * <p>This method utilizes Jackson's {@link ObjectMapper} for parsing and Apache<br>
     * Commons IO's {@code FileUtils} for reading the file content.</p>
     */
    public static Map<String, Map<String, String>> readAllJson(String jsonFilePath)
    {
        try
        {
            String jsonContent =
                    FileUtils.readFileToString(new File(jsonFilePath), StandardCharsets.UTF_8);
            ObjectMapper mapper = new ObjectMapper();
            //Parse json array into list of map
            List<Map<String,String>> dataList= mapper.readValue(jsonContent, new TypeReference<List<Map<String,String>>>() {});

            //Convert List to Map keyed by testCaseName
            Map<String,Map<String,String>> result = new HashMap<>();
            for(Map<String,String> entry : dataList) {
                String testCaseName = entry.get(TCID_KEY);
                result.put(testCaseName,entry);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error reading JSON file: " + jsonFilePath, e);
        }
    }
}