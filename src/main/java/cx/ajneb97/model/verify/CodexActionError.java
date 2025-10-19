package cx.ajneb97.model.verify;

import cx.ajneb97.utils.JSONMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import cx.ajneb97.Codex;

public class CodexActionError extends CodexBaseError {

    private String actionGroup;
    private String actionId;
    private Codex plugin;

    public CodexActionError(Codex plugin, String file, String errorText, boolean critical, String actionGroup, String actionId) {
        super(plugin, file, errorText, critical);
        this.plugin = plugin;
        this.actionId = actionId;
        this.actionGroup = actionGroup;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        JSONMessage jsonMessage = new JSONMessage(player,this.plugin.getMessagesConfig().getString("verifyActionInvalid")
                .replace("{actionGroup}", actionGroup)
                .replace("{actionId}", actionId)
                .replace("{file}", file));
        hover.add("&eTHIS IS A WARNING!");
        hover.add("&fThe action defined is probably");
        hover.add("&fnot formatted correctly:");
        for(String m : getFixedErrorText()) {
            hover.add("&c"+m);
        }
        hover.add(" ");
        hover.add("&fRemember to use a valid action from this list:");
        hover.add("&ahttps://ajneb97.gitbook.io/codex/actions");

        jsonMessage.hover(hover).send();
    }
}
