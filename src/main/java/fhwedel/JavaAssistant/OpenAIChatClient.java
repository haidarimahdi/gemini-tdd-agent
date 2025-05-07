package fhwedel.JavaAssistant;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import org.json.JSONArray;
import org.json.JSONObject;

public class OpenAIChatClient {

    private final String apiKey;
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";

    public OpenAIChatClient(String apiKey) {
        this.apiKey = apiKey;
    }

    public void runChat() throws Exception {
        JSONObject functionDef = buildFunctionDefinition();
        JSONObject requestBody = buildRequestBody(functionDef);
        String response = callOpenAIAPI(requestBody);
        handleResponse(response);
    }

    /**
     * Builds the function definition for the OpenAI function calling feature.
     *
     * Resulting JSON:
     * {
     *   "name": "get_weather",
     *   "description": "Get the current weather in a given location",
     *   "parameters": {
     *     "type": "object",
     *     "properties": {
     *       "location": {
     *         "type": "string",
     *         "description": "The city and state, e.g. San Francisco, CA"
     *       }
     *     },
     *     "required": ["location"]
     *   }
     * }
     */
    public JSONObject buildFunctionDefinition() {
        JSONObject location = new JSONObject()
            .put("type", "string")
            .put("description", "The city and state, e.g. San Francisco, CA");

        JSONObject properties = new JSONObject()
            .put("location", location);

        JSONObject parameters = new JSONObject()
            .put("type", "object")
            .put("properties", properties)
            .put("required", new JSONArray().put("location"));

        return new JSONObject()
            .put("name", "get_weather")
            .put("description", "Get the current weather in a given location")
            .put("parameters", parameters);
    }

    /**
     * Constructs the request body to send to the OpenAI API.
     *
     * Resulting JSON:
     * {
     *   "model": "gpt-4",
     *   "messages": [
     *     {
     *       "role": "user",
     *       "content": "What's the weather like in Boston?"
     *     }
     *   ],
     *   "functions": [ ... function definition ... ],
     *   "function_call": "auto"
     * }
     */
    public JSONObject buildRequestBody(JSONObject functionDef) {
        JSONArray messages = new JSONArray()
            .put(new JSONObject()
                .put("role", "user")
                .put("content", "What's the weather like in Boston?"));

        return new JSONObject()
            .put("model", "gpt-4")
            .put("messages", messages)
            .put("functions", new JSONArray().put(functionDef))
            .put("function_call", "auto");
    }

    /**
     * Sends the HTTP request and returns the response JSON string.
     */
    public String callOpenAIAPI(JSONObject requestBody) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(API_URL).openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        StringBuilder responseBuilder = new StringBuilder();
        try (Scanner scanner = new Scanner(connection.getInputStream(), "utf-8")) {
            while (scanner.hasNextLine()) {
                responseBuilder.append(scanner.nextLine());
            }
        }

        return responseBuilder.toString();
    }

    /**
     * Parses and prints relevant parts of the OpenAI response.
     */
    public void handleResponse(String response) {
        System.out.println("Raw JSON Response:\n" + response);
        JSONObject jsonResponse = new JSONObject(response);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");

        if (message.has("function_call")) {
            JSONObject functionCall = message.getJSONObject("function_call");
            System.out.println("\n--- Parsed Function Call ---");
            System.out.println("Function name: " + functionCall.getString("name"));
            System.out.println("Arguments: " + functionCall.getString("arguments"));
        } else {
            System.out.println("\nNo function call was triggered by the model.");
        }
    }
}
