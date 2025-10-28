package com.example;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import java.nio.charset.StandardCharsets;

public class TDDAgentOrchestrator {

    private final GeminiApiClient geminiApiClient;
    private final JSONArray conversationHistory;
    private final JSONArray tddTools;

    private static final int MAX_ATTEMPTS = 5;

    private final Path canonicalSandboxPath;
    private final String taskTestFile;

    public TDDAgentOrchestrator(String apiKey, String sandboxPath, String testFilePath) throws IOException {
        this.geminiApiClient = new GeminiApiClient(apiKey);
        this.conversationHistory = new JSONArray();
        this.tddTools = new JSONArray()
                .put(buildWriteFileDefinition())
                .put(buildReadFileDefinition())
                .put(buildRunMavenTestDefinition());
        this.canonicalSandboxPath = Path.of(new File(sandboxPath).getCanonicalPath());
        this.taskTestFile = testFilePath;
    }

    public void runTDDWorkflow() throws Exception {
        String startPrompt = "Your goal is to write Java code that passes the tests in '" + this.taskTestFile + "'.\n\n" +
                        "--- TASK ---\n" +
                "You will work in a strict loop. You MUST only respond with tool calls.\n" +
                "1. Call `read_file` to read the test file at '" + this.taskTestFile + "'.\n" +
                "2. After you receive the file content, you MUST determine the source file path " +
                "(e.g., 'src/main/java/com/example/ClassName.java').\n" +
                "3. You MUST then call `write_file` to create the source code.\n" +
                "4. After you receive the write confirmation, you MUST call `run_maven_test`.\n" +
                "5. If the tests fail, analyze the error and go back to step 3. DO NOT just talk about it, " +
                "call `write_file` with the fix.\n" +
                "6. If the tests pass, the task is complete.\n\n" +
                "--- RULES ---\n" +
                "1. You MUST NOT modify the test file at '" + this.taskTestFile + "'.\n" +
                "2. You MUST ONLY write source code to the 'src/main/java/' directory.\n" +
                "3. ALL file paths MUST be relative (e.g., 'src/main/java/MyClass.java').\n\n" +
                "Start by calling `read_file`.";

        addUserMessageToHistory(startPrompt);

        boolean testsPassed = false;
        int attempts = 0;

        while (!testsPassed) {
            if (attempts >= MAX_ATTEMPTS) {
                System.err.println("Agent failed to pass tests after " + MAX_ATTEMPTS + " attempts. Aborting.");
                break;
            }
            attempts++;

            testsPassed = processModelResponse();

        }

        if (testsPassed) {
            System.out.println("Workflow completed! Tests passed.");
        }
    }

    private boolean processModelResponse() throws Exception {
        String responseJson = geminiApiClient.callGeminiAPI(conversationHistory, tddTools);
        JSONObject responsePart = geminiApiClient.parseResponse(responseJson);
        addModelPartToHistory(responsePart);

        if (responsePart.has("functionCall")) {
            JSONObject functionCall = responsePart.getJSONObject("functionCall");
            String toolName = functionCall.getString("name");
            JSONObject args = functionCall.getJSONObject("args");

            String functionResult = executeTool(toolName, args);

            addFunctionResultToHistory(toolName, functionResult);

            return toolName.equals("run_maven_test") && functionResult.contains("BUILD SUCCESS");

        } else {
            String aiText = responsePart.getString("text");
            System.out.println("AI: " + aiText);

            if (aiText.contains("API Error:")) {
                System.err.println("Stopping loop due to API error.");
                return true;    // Stop the loop on error
            }
        }

        return false;   // Continue the loop
    }

    private String executeTool(String toolName, JSONObject args) throws Exception {
        switch (toolName) {
            case "write_file":      return executeWriteFile(args);
            case "read_file":       return executeReadFile(args);
            case "run_maven_test" : return executeRunMavenTest(args);
            default:                return "Unknown tool: " + toolName;
        }
    }

    private String executeWriteFile(JSONObject args) throws Exception {
        String writePath = args.getString("filePath");
        String content = args.getString("fileContent");

        try {
            Path securePath = resolveSecurePath(writePath);

            if (writePath.equals(this.taskTestFile)) {
                System.err.println("AGENT: BLOCKED attempt to write to test file: " + writePath);
                throw new SecurityException("Access denied: You are NOT allowed to modify the test file at " +
                        this.taskTestFile);
            }

            if (!writePath.startsWith("src/main/java/")) {
                System.err.println("AGENT: BLOCKED attempt to write outside 'src/main/java/': " + writePath);
                throw new SecurityException("Access denied: You are ONLY allowed to write to files within " +
                        "'src/main/java/' directory. Your Path '" + writePath + "' is invalid.");
            }

            Files.createDirectories(securePath.getParent());
            System.out.println("AGENT: Writing to file " + writePath);
            Files.write(securePath, content.getBytes((StandardCharsets.UTF_8)));
            return "File written successfully to " + writePath;
        } catch (SecurityException e) {
            return e.getMessage();
        }
    }

    private String executeReadFile(JSONObject args) throws Exception {
        String readPath = args.getString("filePath");

        try {
            Path securePath = resolveSecurePath(readPath);

            System.out.println("AGENT: Reading file " + readPath);
            return new String(Files.readAllBytes(securePath));
        } catch (SecurityException | IOException e) {
            return e.getMessage();
        }
    }

    private String executeRunMavenTest(JSONObject args) throws Exception {
        System.out.println("AGENT: Running 'mvn clean test'...");
        try {
            String os =  System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", "mvn clean test");
            } else {
                pb = new ProcessBuilder("sh", "-c", "mvn clean test");
            }

            // Set the working directory for the Maven command
            pb.directory(this.canonicalSandboxPath.toFile());

            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            process.waitFor();
            String result = output.toString();

            if (result.contains("BUILD SUCCESS")) {
                return "Maven command successful.\n\n" + result;
            } else {
                return "Maven command failed. The AI must analyze the compile or test errors.\n\n" + result;
            }
        } catch (Exception e) {
            return "Failed to execute 'mvn clean test': " + e.getMessage();
        }
    }

    private Path resolveSecurePath(String relativePathFromAI) throws IOException, SecurityException {
        Path fullPath = this.canonicalSandboxPath.resolve(relativePathFromAI).normalize();

        Path canonicalFullPath = Path.of(fullPath.toFile().getCanonicalPath());

        if (!canonicalFullPath.startsWith(this.canonicalSandboxPath)) {
            System.err.println("AGENT: BLOCKED attempt to access file outside of sandbox: " + relativePathFromAI);
            throw new SecurityException("Access denied: Attempt to access file outside of sandbox.");
        }

        return canonicalFullPath;
    }

    public JSONObject buildWriteFileDefinition() {
        JSONObject path = new JSONObject()
                .put("type", "string")
                .put("description", "The full path to the file, e.g., 'src/test/java/com/example/MyServiceTest.java'");

        JSONObject content = new JSONObject()
                .put("type", "string")
                .put("description", "The entire Java code content to write to the file.");

        JSONObject properties = new JSONObject()
                .put("filePath", path)
                .put("fileContent", content);

        JSONObject parameters = new JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", new JSONArray().put("filePath").put("fileContent"));

        return new JSONObject()
                .put("name", "write_file")
                .put("description", "writes or overwrites a file with the provided content.")
                .put("parameters", parameters);
    }

    public JSONObject buildReadFileDefinition() {
        JSONObject filePath = new JSONObject()
                .put("type", "string")
                .put("description", "The full path to the file. e.g., 'src/main/test/java/MyServiceTest.java'");

        JSONObject properties = new JSONObject()
                .put("filePath", filePath);

        JSONObject parameters = new JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", new JSONArray().put("filePath"));

        return new JSONObject()
                .put("name", "read_file")
                .put("description", "Reads the entire content of a specified file.")
                .put("parameters", parameters);
    }

    public JSONObject buildRunMavenTestDefinition() {
        JSONObject properties = new JSONObject(); // No properties

        JSONObject parameters = new JSONObject()
                .put("type", "object")
                .put("properties", properties)
                .put("required", new JSONArray()); // No required parameters

        return new JSONObject()
                .put("name", "run_maven_test")
                .put("description", "Compiles all source code and runs the JUnit tests using Maven ('mvn clean test')." +
                        "Returns the full console output from Maven, including compile errors or test failures.")
                .put("parameters", parameters);
    }

    private void addModelPartToHistory(JSONObject part) {
        this.conversationHistory.put(new JSONObject().put("role", "model").put("parts", new JSONArray().put(part)));
    }

    private void addUserMessageToHistory(String content) {
        JSONObject part = new JSONObject().put("text", content);
        this.conversationHistory.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(part)));
    }

    private void addFunctionResultToHistory(String toolName, String result) {
        JSONObject resultPart = new JSONObject().put("functionResponse", new JSONObject()
                .put("name", toolName)
                .put("response", new JSONObject().put("content", result))
        );
        this.conversationHistory.put(new JSONObject().put("role", "user").put("parts", new JSONArray().put(resultPart)));

    }
}
