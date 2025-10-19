package net.Indyuce.mmoitems.comp.rpg;

import com.archyx.aureliumskills.AureliumSkills;
import com.archyx.aureliumskills.api.AureliumAPI;
import com.archyx.aureliumskills.api.event.SkillLevelUpEvent;
import com.archyx.aureliumskills.data.PlayerDataLoadEvent;
import com.archyx.aureliumskills.skills.Skill;
import com.archyx.aureliumskills.skills.Skills;
import com.archyx.aureliumskills.stats.Stats;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.EmptyRPGPlayer;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.RequiredLevelStat;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * @deprecated Old version of {@link AuraSkillsHook}
 */
@Deprecated
public class AureliumSkillsHook implements RPGHandler, Listener {
    private final AureliumSkills aSkills;

    private final Map<Stats, ItemStat> statExtra = new HashMap<>();

    public AureliumSkillsHook() {
        aSkills = (AureliumSkills) Bukkit.getPluginManager().getPlugin("AureliumSkills");

        for (Stats stat : Stats.values()) {
            final String statName = UtilityMethods.caseOnWords(stat.name().toLowerCase());
            final ItemStat miStat = new DoubleStat("ADDITIONAL_" + stat.name(), Material.BOOK,
                    "Additional " + statName,
                    new String[]{"Additional " + statName + " (AureliumSkills)"},
                    new String[]{"!miscellaneous", "!block", "all"});

            statExtra.put(stat, miStat);
            MMOItems.plugin.getStats().register(miStat);
        }

        // Register stat for required professions
        for (Skills skill : Skills.values())
            MMOItems.plugin.getStats().register(new RequiredProfessionStat(skill));
    }

    @EventHandler
    public void a(SkillLevelUpEvent event) {
        OfflinePlayer player = event.getPlayer();
        if (player.isOnline()) PlayerData.get(player).resolveModifiersLater();
    }

    /**
     * AureliumSkills stores modifiers using ONE hash map for every stat
     * unlike MythicLib which has several stat instances. Therefore, a
     * valid key for a stat modifier is "mmoitems_<stat_name>".
     * <p>
     * Be careful, ASkills permanently stores modifiers unlike ML
     */
    private static final String MODIFIER_KEY_PREFIX = "mmoitems_";

    @Override
    public void refreshStats(PlayerData data) {
        statExtra.forEach((stat, miStat) -> AureliumAPI.addStatModifier(data.getPlayer(), MODIFIER_KEY_PREFIX + stat.name(), stat, data.getStat(miStat)));
    }

    @Override
    public RPGPlayer getInfo(PlayerData data) {

        /**
         * AureliumSkills does not load player data directly on startup, instead we have to
         * listen to the PlayerDataLoadEvent before caching the rpg player data instance.
         *
         * See PlayerDataLoadEvent event handler below.
         */
        return new EmptyRPGPlayer(data);
    }

    @EventHandler
    public void a(PlayerDataLoadEvent event) {
        Player player = event.getPlayerData().getPlayer();
        PlayerData playerData = PlayerData.get(player);
        playerData.setRPGPlayer(new PlayerWrapper(playerData, event.getPlayerData()));
    }

    @Deprecated
    public static class PlayerWrapper extends RPGPlayer {
        private final com.archyx.aureliumskills.data.PlayerData info;

        public PlayerWrapper(PlayerData playerData, com.archyx.aureliumskills.data.PlayerData rpgPlayerData) {
            super(playerData);

            info = rpgPlayerData;
        }

        public com.archyx.aureliumskills.data.PlayerData getAureliumSkillsPlayerData() {
            return info;
        }

        @Override
        public int getLevel() {
            return info.getPowerLevel();
        }

        @Override
        public String getClassName() {
            return "";
        }

        @Override
        public double getMana() {
            return info.getMana();
        }

        @Override
        public double getStamina() {
            return getPlayer().getFoodLevel();
        }

        @Override
        public void setMana(double value) {
            info.setMana(value);
        }

        @Override
        public void setStamina(double value) {
            getPlayer().setFoodLevel((int) value);
        }
    }

    public class RequiredProfessionStat extends RequiredLevelStat {
        private final Skill skill;

        public RequiredProfessionStat(Skills skill) {
            super(skill.name(), Material.EXPERIENCE_BOTTLE, skill.getDisplayName(Locale.getDefault()),
                    new String[]{"Amount of " + skill.getDisplayName(Locale.getDefault()) + " levels the", "player needs to use the item.", "(AureliumSkills)"});

            this.skill = aSkills.getSkillRegistry().getSkill(skill.name());
        }

        @Override
        public boolean canUse(RPGPlayer player, NBTItem item, boolean message) {
            final int requirement = item.getInteger(this.getNBTPath());
            if (requirement <= 0) return true;

            final int skillLevel = AureliumAPI.getSkillLevel(player.getPlayer(), skill);
            if (skillLevel >= requirement || player.getPlayer().hasPermission("mmoitems.bypass.level")) return true;

            if (message) {
                Message.NOT_ENOUGH_PROFESSION.format(ChatColor.RED, "#profession#", skill.getDisplayName(Locale.getDefault())).send(player.getPlayer());
                player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.ENTITY_VILLAGER_NO, 1, 1.5f);
            }
            return false;
        }
    }
}