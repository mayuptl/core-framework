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
 * <p>This class is designed to handle JSON files that contain arrays of test case data.
 * It uses the test case ID (TCID) as the primary key for data retrieval, enabling
 * robust, data-driven test automation.</p>
 * <p>It uses Jackson Databind for mapping JSON content to Java collections,
 * specifically {@code Map<String, String>} for individual data sets.</p>
 * <h3>Key Methods Summary:</h3>
 * <ul>
 * <li>{@code getJsonInput}: Retrieves a single, specific data entry based on a given test case name.</li>
 * <li>{@code getJsonInputs}: Useful for executing the same test case logic with multiple different input variations
 * (Data Provider style).</li>
 * <li>{@code readAllJson}: Reads the entire JSON file into a class-level cache
 * ({@code Map<String, Map<String, String>>}) during {@code @BeforeClass} setup for
 * efficient, repeated access in test methods.</li>
 * </ul>
 */
public class CoreJsonReader {
    /**
     * Key used in the JSON data objects to identify the test case name/ID (TCID).
     * The value for this key (e.g., "testCaseName") is dynamically retrieved
     * during initialization using the {@code ConfigReader.getStrProp("JSON_DATA_TCID_KEY")}.
     * This key is mandatory for methods that look up data by test case name,
     * such as {@code getJsonInput} and {@code readAllJson}.
     */
    //private static final String JSON_DATA_TCID_KEY = "testCaseName";
    private static final String TCID_KEY = CoreConfigReader.getStrProp("JSON_DATA_TCID_KEY");
    /**
     * Retrieves the input data for a specific test case ID from a JSON array file.
     * <p>The method searches the JSON array for a single object where the key defined by {@code JSON_DATA_TCID_KEY}
     * matches the provided {@code testCaseName}.</p>
     *
     * @param jsonFilePath The full path to the JSON data file. The JSON must be an array of objects.
     * @param testCaseName The unique ID/Name of the test case to look for (e.g., "TC_001_Login").
     * @return A {@code Object[][]} containing the single matching data HashMap. This specific
     * structure (one row, one column) is formatted for **direct use as a TestNG DataProvider**,
     * allowing the data map to be passed as the first argument to the test method.
     * @throws RuntimeException if the file is not found, cannot be read, the JSON is invalid,
     * or the specified testCaseName is not found in the data.
     *
     * <h3>Example Usage in TestNG (DataProvider Pattern)</h3>
     * This method is designed to be called from a **DataProvider** which supplies the test method
     * name (or TCID) via {@code ITestContext} or {@code ITestResult}:
     * <pre>{@code
     * import core.json.JsonReader;
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
     * <p>This method is highly useful for DataProviders that need to iterate through all rows
     * or a specific subset of rows in the JSON file, as it returns the raw, parsed data
     * structure directly.</p>
     *
     * @param jsonFilePath The full path to the JSON data file. The JSON must be an array of objects.
     * @return A {@code List<HashMap<String, String>>}, where each HashMap represents
     * one test data entry (data row) from the JSON array.
     * @throws IOException if the JSON content is invalid or a file reading error occurs (e.g., parsing failure after file read).
     * @throws RuntimeException if the file is not found or cannot be read during the initial file operation.
     * <h3>Example Usage in a TestNG DataProvider:</h3>
     * <p>The DataProvider must process the returned {@code List} and convert the selected elements
     * into an {@code Object[]} or {@code Object[][]} suitable for TestNG injection.</p>
     * <pre>{@code
     * import core.json.JsonReader;
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
     * test case ID.
     *
     * <p>Each element in the input JSON array must be an object containing the key
     * defined by {@code TCID_KEY} (e.g., "testCaseName") to be used as the primary
     * key in the output map.</p>
     *
     * <h3>Example Usage in TestNG (Output Consumption)</h3>
     * The resulting Map is typically loaded once per class and then used in {@code @BeforeMethod}
     * to fetch the specific data for the currently executing test method name.
     * <pre>
     * import core.json.JsonReader;
     * public class LoginTest {
     * // Map containing all data keyed by TCID
     * private static Map&lt;String, Map&lt;String, String&gt;&gt; testData;
     * // Map for the specific test case's data
     * private Map &lt;String, String&gt; input;
     *
     * {@code @BeforeClass}
     * public void setupClass() {
     * String jsonFilePath = "path/to/jsonFile";
     * testData = JsonReader.readAllJson(jsonFilePath); // Loads all data once
     * }
     * {@code @BeforeMethod}
     * public void setupMethod(ITestResult result) {
     * // Gets the specific data map based on the test method name
     * String testCaseName = result.getMethod().getMethodName();
     * input = testData.get(testCaseName);
     * }
     * {@code @Test}
     * public void Test_001_Login_ValidUser() {
     * // Test case uses the specific input map
     * String username = input.get("username");
     * String password = input.get("password");
     *  // ... perform actions with username and password
     * }
     * }
     * </pre>
     * <h3>Example JSON Structure (Input)</h3>
     * The JSON file content should look like this, where "TestCaseName" is the value
     * held by the {@code TCID_KEY} constant:
     * <pre>
     * [
     * {
     * "TestCaseName": "Test_001_Login_ValidUser",
     * "username": "user123",
     * "password": "Password1"
     * },
     * {
     * "TestCaseName": "Test_002_Login_InvalidPassword",
     * "username": "user123",
     * "password": "wrong password"
     * }
     * ]
     * </pre>
     * <p>This method utilizes Jackson's {@link ObjectMapper} for parsing and Apache
     * Commons IO's {@code FileUtils} for reading the file content.</p>
     *
     * @param jsonFilePath The absolute or relative path to the JSON file to be read.
     * @return A {@code Map<String, Map<String, String>>} where the outer map's key is
     * the value of {@code TCID_KEY}, and the inner map
     * contains all the key-value pairs from that JSON object.
     * @throws RuntimeException if an {@link IOException} occurs during file reading
     * or JSON parsing, wrapping the original exception for context.
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
