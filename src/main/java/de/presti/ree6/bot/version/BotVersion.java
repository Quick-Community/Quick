package de.presti.ree6.bot.version;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Class with every Version.
 */
@Getter
@AllArgsConstructor
public enum BotVersion {

    /**
     * Version for Development tests.
     */
    DEVELOPMENT("bot.tokens.dev", true),
    /**
     * Version for a not yet fully stable release.
     */
    BETA("bot.tokens.beta",false),
    /**
     * Version for a stable release.
     */
    RELEASE("bot.tokens.release", false);

    /**
     * The Token-Path in the config file.
     */
    final String tokenPath;

    /**
     * If the Bot version should activate the debug mode.
     */
    final boolean debug;

}
