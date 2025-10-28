package com.example;

public class TddAgentMain {

    private static final String SANDBOX_PATH = "code-sandbox";

    public static void main(String[] args) {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: GEMINI_API_KEY environment variable is not set.");
            return;
        }

        if (args.length != 1) {
            System.err.println("Error: Please provide test file path as an argument.");
            System.err.println("Usage: mvn exec:java -Dexec.args=\"<path/to/Test.java>\"");
            return;
        }

        String testFilePath = args[0];

        System.out.println("Starting TDD Agent with Workspace:");
        System.out.println(" Sandbox Path: " + SANDBOX_PATH + "/src/main/java/com/example");
        System.out.println(" Task Test File: " + SANDBOX_PATH + "/" + testFilePath);

        try {
            TDDAgentOrchestrator agent = new TDDAgentOrchestrator(apiKey, SANDBOX_PATH, testFilePath);
            agent.runTDDWorkflow();
        } catch (Exception e) {
            System.err.println("An error occurred during the TDD workflow:");
            e.printStackTrace();
        }
    }
}
