package codes.smit.listeners;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

public class CommandListener extends ListenerAdapter {

    @Override
    public void onReady(ReadyEvent event) {
        // Register commands when bot is ready
        event.getJDA().updateCommands().addCommands(
                Commands.slash("ping", "Check the bot's response time")
        ).queue();

        System.out.println("✅ Commands registered successfully!");
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ping")) {
            handlePing(event);
        }
    }

    private void handlePing(SlashCommandInteractionEvent event) {
        long time = System.currentTimeMillis();
        event.reply("Pong!").setEphemeral(true).queue();
    }
}