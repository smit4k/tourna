package codes.smit;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

import io.github.cdimascio.dotenv.Dotenv;
import codes.smit.database.DatabaseManager;
import codes.smit.listeners.CommandListener;


public class Tourna {

    public static void main(String[] args) {
        // Use environment variable for security
        Dotenv dotenv = Dotenv.load();
        String token = dotenv.get("DISCORD_TOKEN");


        DatabaseManager db = new DatabaseManager();

        if (token == null) {
            System.err.println("DISCORD_TOKEN not found in .env!");
            return;
        }

        JDABuilder.createDefault(token)
                .enableIntents(
                        GatewayIntent.MESSAGE_CONTENT,
                        GatewayIntent.GUILD_MEMBERS
                )
                .addEventListeners(new CommandListener())
                .build();

        System.out.println("Tourna bot is starting...");
    }
}