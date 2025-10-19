package cx.ajneb97.model.verify;

import cx.ajneb97.utils.JSONMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import cx.ajneb97.Codex;

public class CodexInventoryInvalidCategoryError extends CodexBaseError {

    private String categoryName;
    private String inventoryName;
    private String slot;
    private Codex plugin;

    public CodexInventoryInvalidCategoryError(Codex plugin, String file, String errorText, boolean critical, String categoryName, String inventoryName, String slot) {
        super(plugin, file, errorText, critical);
        this.plugin = plugin;
        this.categoryName = categoryName;
        this.inventoryName = inventoryName;
        this.slot = slot;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        JSONMessage jsonMessage = new JSONMessage(player,prefix+"&7Invalid category named &c"+categoryName+" &7on file &c"+file);
        hover.add("&eTHIS IS AN ERROR!");
        hover.add("&fA category that doesn't exists is present on");
        hover.add("&finventory &c"+inventoryName+" &fand slot &c"+slot+"&f.");

        jsonMessage.hover(hover).send();
    }
}
