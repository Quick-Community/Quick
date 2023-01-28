package de.presti.ree6.commands.impl.community;

import com.google.gson.JsonObject;
import de.presti.ree6.commands.Category;
import de.presti.ree6.commands.CommandEvent;
import de.presti.ree6.commands.interfaces.Command;
import de.presti.ree6.commands.interfaces.ICommand;
import de.presti.ree6.language.LanguageService;
import de.presti.ree6.sql.SQLSession;
import de.presti.ree6.sql.entities.StreamAction;
import de.presti.ree6.sql.entities.TwitchIntegration;
import de.presti.ree6.streamtools.StreamActionContainer;
import de.presti.ree6.streamtools.StreamActionContainerCreator;
import de.presti.ree6.streamtools.action.IStreamAction;
import de.presti.ree6.streamtools.action.StreamActionInfo;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandGroupData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;

import java.util.Arrays;
import java.util.Map;

/**
 * A command used to create and manage StreamActions.
 */
@Command(name = "stream-action", description = "command.description.stream-action", category = Category.COMMUNITY)
public class StreamActionCommand implements ICommand {

    /**
     * @inheritDoc
     */
    @Override
    public void onPerform(CommandEvent commandEvent) {
        if (!commandEvent.getGuild().getSelfMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            commandEvent.reply(commandEvent.getResource("message.default.needPermission", Permission.MANAGE_WEBHOOKS.getName()));
            return;
        }

        if (!commandEvent.isSlashCommand()) {
            commandEvent.reply(commandEvent.getResource("command.perform.onlySlashSupported"));
            return;
        }


        if (!commandEvent.getMember().hasPermission(Permission.MANAGE_WEBHOOKS)) {
            commandEvent.reply(commandEvent.getResource("message.default.insufficientPermission", Permission.MANAGE_WEBHOOKS.getName()));
            return;
        }

        OptionMapping createName = commandEvent.getSlashCommandInteractionEvent().getOption("createName");
        OptionMapping deleteName = commandEvent.getSlashCommandInteractionEvent().getOption("deleteName");

        if (createName != null) {
            StreamAction streamAction = SQLSession.getSqlConnector().getSqlWorker()
                    .getEntity(new StreamAction(), "SELECT * FROM StreamActions WHERE actionName = :name AND guildId = :gid",
                            Map.of("name", deleteName.getAsString(), "gid", commandEvent.getGuild().getIdLong()));

            if (streamAction == null) {
                TwitchIntegration twitchIntegration = SQLSession.getSqlConnector().getSqlWorker()
                        .getEntity(new TwitchIntegration(),"SELECT * FROM TwitchIntegration WHERE user_id = :uid", Map.of("uid", commandEvent.getUser().getIdLong()));
                if (twitchIntegration != null) {
                    streamAction = new StreamAction();
                    streamAction.setIntegration(twitchIntegration);
                    streamAction.setGuildId(commandEvent.getGuild().getIdLong());
                    streamAction.setActionName(createName.getName());

                    SQLSession.getSqlConnector().getSqlWorker().updateEntity(streamAction);
                } else {
                    commandEvent.reply(commandEvent.getResource("message.stream-action.noTwitch", "https://cp.ree6.de/twitch/auth"));
                }
            } else {
                commandEvent.reply(commandEvent.getResource("message.stream-action.alreadyExisting", createName.getAsString()));
            }
        } else if (deleteName != null) {
            StreamAction streamAction = SQLSession.getSqlConnector().getSqlWorker()
                    .getEntity(new StreamAction(), "SELECT * FROM StreamActions WHERE actionName = :name AND guildId = :gid",
                            Map.of("name", deleteName.getAsString(), "gid", commandEvent.getGuild().getIdLong()));
            if (streamAction != null) {
                SQLSession.getSqlConnector().getSqlWorker().deleteEntity(streamAction);
                commandEvent.reply(commandEvent.getResource("message.stream-action.deleted", deleteName.getAsString()));
            } else {
                commandEvent.reply(commandEvent.getResource("message.stream-action.notFound", deleteName.getAsString()));
            }
        } else {
            OptionMapping name = commandEvent.getSlashCommandInteractionEvent().getOption("name");
            OptionMapping manageAction = commandEvent.getSlashCommandInteractionEvent().getOption("manageAction");
            OptionMapping manageActionValue = commandEvent.getSlashCommandInteractionEvent().getOption("manageActionValue");

            if (name == null) {
                commandEvent.reply(commandEvent.getResource("message.default.missingOption", "name"));
                return;
            }

            if (manageAction == null) {
                commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageAction"));
                return;
            }

            StreamAction streamAction = SQLSession.getSqlConnector().getSqlWorker()
                    .getEntity(new StreamAction(), "SELECT * FROM StreamActions WHERE actionName = :name AND guildId = :gid",
                            Map.of("name", name.getAsString(), "gid", commandEvent.getGuild().getIdLong()));

            if (streamAction != null) {
                switch (manageAction.getAsString()) {
                    case "add": {
                        if (manageActionValue == null) {
                            commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                            return;
                        }

                        String[] values = manageActionValue.getAsString().split("\\s+");

                        if (values.length < 2) {
                            return;
                        }

                        JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("action", values[0]);

                        values = Arrays.stream(values).skip(1).toArray(String[]::new);

                        jsonObject.addProperty("value", String.join(" ", values));

                        streamAction.getActions().getAsJsonArray().add(jsonObject);

                        SQLSession.getSqlConnector().getSqlWorker().updateEntity(streamAction);

                        commandEvent.reply(commandEvent.getResource("message.stream-action.added"));
                    }

                    case "listen": {
                        if (manageActionValue == null) {
                            commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                            return;
                        }

                        String[] values = manageActionValue.getAsString().split("\\s+");

                        if (values.length == 2) {
                            if (values[0].equalsIgnoreCase("redemption")) {
                                streamAction.setListener(StreamAction.StreamListener.REDEMPTION);
                            } else if (values[0].equalsIgnoreCase("follow")) {
                                streamAction.setListener(StreamAction.StreamListener.FOLLOW);
                            } else {
                                commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                                return;
                            }

                            streamAction.setArgument(values[1]);
                            SQLSession.getSqlConnector().getSqlWorker().updateEntity(streamAction);
                        } else {
                            commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                        }
                    }

                    case "list": {
                        StreamActionContainer streamActionContainer = new StreamActionContainer(streamAction);
                        commandEvent.reply(commandEvent.getResource("message.stream-action.list",
                                streamActionContainer.getActions().entrySet().stream()
                                        .map((Map.Entry<IStreamAction, String[]> entry) ->
                                                entry.getKey().getClass().getAnnotation(StreamActionInfo.class).name() + " -> "
                                                        + String.join(" ", entry.getValue()))));
                    }

                    case "delete": {
                        if (manageActionValue == null) {
                            commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                            return;
                        }

                        try {
                            int value = manageActionValue.getAsInt();

                        } catch (Exception exception) {
                            commandEvent.reply(commandEvent.getResource("message.default.missingOption", "manageActionValue"));
                        }
                    }
                }
            } else {
                commandEvent.reply(commandEvent.getResource("message.stream-action.notFound", name.getAsString()));
            }
        }

    }

    /**
     * @inheritDoc
     */
    @Override
    public CommandData getCommandData() {
        return new CommandDataImpl("stream-action",
                LanguageService.getDefault("command.description.stream-action"))
                .addOption(OptionType.STRING, "createName", "The name of the to be created action")
                .addOption(OptionType.STRING, "deleteName", "The name of the to be deleted action")
                .addOption(OptionType.STRING, "name", "The name of the already created action")
                .addOption(OptionType.STRING, "manageAction", "The managing action that should be performed on the Stream-Action")
                .addOption(OptionType.STRING, "manageActionValue", "The value of the managing action");
    }

    /**
     * @inheritDoc
     */
    @Override
    public String[] getAlias() {
        return new String[0];
    }
}
