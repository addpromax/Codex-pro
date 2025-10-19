package cx.ajneb97.model.verify;

import cx.ajneb97.utils.JSONMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import cx.ajneb97.Codex;

public class CodexInventoryInvalidSlotError extends CodexBaseError {

    private String inventoryName;
    private int slot;
    private int maxSlots;
    private Codex plugin;

    public CodexInventoryInvalidSlotError(Codex plugin, String file, String errorText, boolean critical, int slot, String inventoryName, int maxSlots) {
        super(plugin, file, errorText, critical);
        this.plugin = plugin;
        this.inventoryName = inventoryName;
        this.slot = slot;
        this.maxSlots = maxSlots;
    }

    @Override
    public void sendMessage(Player player) {
        List<String> hover = new ArrayList<String>();

        JSONMessage jsonMessage = new JSONMessage(player,prefix+plugin.getMessagesConfig().getString("inventory.invalidSlot.message"));
        hover.add(plugin.getMessagesConfig().getString("inventory.invalidSlot.hover1"));
        hover.add(plugin.getMessagesConfig().getString("inventory.invalidSlot.hover2"));
        hover.add(plugin.getMessagesConfig().getString("inventory.invalidSlot.hover3"));
        hover.add(plugin.getMessagesConfig().getString("inventory.invalidSlot.hover4"));

        jsonMessage.hover(hover).send();
    }
}
