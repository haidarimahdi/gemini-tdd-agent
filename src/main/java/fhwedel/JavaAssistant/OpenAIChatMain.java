package fhwedel.JavaAssistant;

public class OpenAIChatMain {
    public static void main(String[] args) {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("Error: OPENAI_API_KEY environment variable is not set.");
            return;
        }

        OpenAIChatClient client = new OpenAIChatClient(apiKey);
        try {
            client.runChat();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
