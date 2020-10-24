package me.leoko.abgui;

import me.leoko.advancedgui.manager.GuiItemManager;
import me.leoko.advancedgui.manager.LayoutManager;
import me.leoko.advancedgui.utils.GuiItemInstance;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import static me.leoko.abgui.Components.LAYOUT_NAME;

public class AdvancedBanGUI extends JavaPlugin {
    @Override
    public void onEnable() {
        LayoutManager.getInstance().registerLayoutExtension(new GUIExtension(), this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission("advancedban.gui")){
            if(sender instanceof Player){
                final GuiItemInstance itemInstance = GuiItemManager.getInstance().getItemInstance(LAYOUT_NAME);
                if(itemInstance != null){
                    ((Player) sender).getInventory().addItem(itemInstance.createItem());
                    sender.sendMessage("§aDone.");
                } else {
                    sender.sendMessage("§cYou need to place the AdvancedBan.json layout file into the AG layout folder!");
                }
            } else {
                sender.sendMessage("§cThis command is only for players");
            }
        } else {
            sender.sendMessage("§cPermission required: advancedban.gui");
        }
        return true;
    }
}
