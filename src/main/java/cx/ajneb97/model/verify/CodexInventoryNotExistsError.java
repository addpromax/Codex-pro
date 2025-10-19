package cx.ajneb97.model.verify;

import cx.ajneb97.utils.JSONMessage;
import org.bukkit.entity.Player;
import cx.ajneb97.Codex;

import java.util.ArrayList;
import java.util.List;

public class CodexInventoryNotExistsError extends CodexBaseError {

    private String inventoryName;
    private String inventoryPath;
    private String slot;
    private Codex plugin;

    public CodexInventoryNotExistsError(Codex plugin, String file, String errorText, boolean critical, String inventoryName) {
        super(plugin, file, errorText, critical);
        this.plugin = plugin;
        this.inventoryName = inventoryName;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        JSONMessage jsonMessage = new JSONMessage(player,prefix+"&7Inventory &c"+inventoryName+" &7not valid");
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fThe &c"+inventoryName+" &fopenened inventory used");
        hover.add("&fon slot &c"+slot+" &fon inventory &c"+inventoryPath);
        hover.add("&fis not valid. Create it on the inventory.yml file.");

        jsonMessage.hover(hover).send();
    }
}
