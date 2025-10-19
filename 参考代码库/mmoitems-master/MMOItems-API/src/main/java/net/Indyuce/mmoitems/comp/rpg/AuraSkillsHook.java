package net.Indyuce.mmoitems.comp.rpg;

import dev.aurelium.auraskills.api.AuraSkillsApi;
import dev.aurelium.auraskills.api.event.skill.SkillLevelUpEvent;
import dev.aurelium.auraskills.api.event.user.UserLoadEvent;
import dev.aurelium.auraskills.api.skill.Skill;
import dev.aurelium.auraskills.api.skill.Skills;
import dev.aurelium.auraskills.api.stat.StatModifier;
import dev.aurelium.auraskills.api.stat.Stats;
import dev.aurelium.auraskills.api.trait.TraitModifier;
import dev.aurelium.auraskills.api.trait.Traits;
import dev.aurelium.auraskills.api.user.SkillsUser;
import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.ItemStats;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.player.EmptyRPGPlayer;
import net.Indyuce.mmoitems.api.player.PlayerData;
import net.Indyuce.mmoitems.api.player.RPGPlayer;
import net.Indyuce.mmoitems.api.util.message.Message;
import net.Indyuce.mmoitems.stat.type.DoubleStat;
import net.Indyuce.mmoitems.stat.type.ItemStat;
import net.Indyuce.mmoitems.stat.type.RequiredLevelStat;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AuraSkillsHook implements RPGHandler, Listener {
    private AuraSkillsApi aSkills;

    private final Map<Stats, ItemStat> statExtra = new HashMap<>();

    public AuraSkillsHook() {

        aSkills = AuraSkillsApi.get();

        for (Stats stat : Stats.values()) {
            final String statName = UtilityMethods.caseOnWords(stat.name().toLowerCase());
            final ItemStat miStat = new DoubleStat("ADDITIONAL_" + stat.name(), Material.BOOK,
                    "Additional " + statName,
                    new String[]{"Additional " + statName + " (AuraSkills)"},
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
     * AuraSkills stores modifiers using ONE hash map for every stat
     * unlike MythicLib which has several stat instances. Therefore, a
     * valid key for a stat modifier is "mmoitems_<stat_name>".
     * <p>
     * Be careful, ASkills permanently stores modifiers unlike ML
     */
    private static final String MODIFIER_KEY_PREFIX = "mmoitems_";

    @Override
    public void refreshStats(PlayerData data) {
        SkillsUser user = aSkills.getUser(data.getPlayer().getUniqueId());

        user.addTraitModifier(
                new TraitModifier(MODIFIER_KEY_PREFIX + "max_mana", Traits.MAX_MANA, data.getStat(ItemStats.MAX_MANA)));

        double currentMaxMana = user.getMaxMana();

        if (user.getMana() > currentMaxMana) {
            user.setMana(currentMaxMana);
        }

        statExtra.forEach((stat, miStat) -> aSkills.getUser(data.getPlayer().getUniqueId()).addStatModifier(new StatModifier(MODIFIER_KEY_PREFIX + stat.name(), stat, data.getStat(miStat))));
    }

    @Override
    public RPGPlayer getInfo(PlayerData data) {

        /*
         * AuraSkills does not load player data directly on startup, instead we have to
         * listen to the PlayerDataLoadEvent before caching the rpg player data instance.
         *
         * See PlayerDataLoadEvent event handler below.
         */
        return new EmptyRPGPlayer(data);
    }

    @EventHandler
    public void a(UserLoadEvent event) {
        Player player = event.getPlayer();
        PlayerData playerData = PlayerData.get(player);
        playerData.setRPGPlayer(new PlayerWrapper(playerData, event.getUser()));
    }

    private static class PlayerWrapper extends RPGPlayer {
        private final SkillsUser info;

        public PlayerWrapper(PlayerData playerData, SkillsUser rpgPlayerData) {
            super(playerData);

            info = rpgPlayerData;
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
        public void consumeMana(double value) {
            // [BUGFIX] AuraSkill has a "Sorcery" skill, which by default can be levelled
            // up by simply using mana (mana_ability_use xp source). MMOItems's mana usage
            // does not trigger this XP gain.
            // Source: https://gitlab.com/phoenix-dvpmt/mmoitems/-/issues/1807
            info.consumeMana(value);
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

    private class RequiredProfessionStat extends RequiredLevelStat {
        private final Skill skill;

        public RequiredProfessionStat(Skills skill) {
            super(skill.name(), Material.EXPERIENCE_BOTTLE, skill.getDisplayName(Locale.getDefault()),
                    new String[]{"Amount of " + skill.getDisplayName(Locale.getDefault()) + " levels the", "player needs to use the item.", "(AuraSkills)"});

            this.skill = aSkills.getGlobalRegistry().getSkill(skill.getId());
        }

        @Override
        public boolean canUse(RPGPlayer player, NBTItem item, boolean message) {
            final int requirement = item.getInteger(this.getNBTPath());
            if (requirement <= 0) return true;

            final int skillLevel = aSkills.getUser(player.getPlayer().getUniqueId()).getSkillLevel(skill);
            if (skillLevel >= requirement || player.getPlayer().hasPermission("mmoitems.bypass.level")) return true;

            if (message) {
                Message.NOT_ENOUGH_PROFESSION.format(ChatColor.RED, "#profession#", skill.getDisplayName(Locale.getDefault())).send(player.getPlayer());
                player.getPlayer().playSound(player.getPlayer().getLocation(), Sounds.ENTITY_VILLAGER_NO, 1, 1.5f);
            }
            return false;
        }
    }
}