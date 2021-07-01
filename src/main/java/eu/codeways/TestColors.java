package eu.codeways;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.EventListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestColors extends JavaPlugin implements Listener {
    @Override
    public void onEnable() {
        getLogger().info("TestColor enabled");
        getServer().getPluginManager().registerEvents(this,this);
    }

    @Override
    public void onDisable() {
        getLogger().info("TestColor enabled");
    }

    @EventHandler
    public void playerMessageEvent(AsyncPlayerChatEvent event) {
        String message = translateHexCodes(event.getMessage());
        event.setMessage(message);
        getLogger().info(message);
    }

    private static final Pattern HEX_PATTERN = Pattern.compile("&#(\\w{5}[0-9a-f])");

    //stolen from github lol
    private String translateHexCodes (String textToTranslate) {

        Matcher matcher = HEX_PATTERN.matcher(textToTranslate);
        StringBuffer buffer = new StringBuffer();

        while(matcher.find()) {
            matcher.appendReplacement(buffer, ChatColor.of("#" + matcher.group(1)).toString());
        }

        return ChatColor.translateAlternateColorCodes('&', matcher.appendTail(buffer).toString());

    }
}
