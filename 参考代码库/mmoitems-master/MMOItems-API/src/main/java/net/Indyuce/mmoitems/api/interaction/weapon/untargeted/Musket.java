package net.Indyuce.mmoitems.api.interaction.weapon.untargeted;

import io.lumine.mythic.lib.UtilityMethods;
import io.lumine.mythic.lib.api.item.NBTItem;
import io.lumine.mythic.lib.api.player.EquipmentSlot;
import io.lumine.mythic.lib.comp.interaction.InteractionType;
import io.lumine.mythic.lib.damage.DamageType;
import io.lumine.mythic.lib.player.PlayerMetadata;
import io.lumine.mythic.lib.util.RayTrace;
import io.lumine.mythic.lib.version.Sounds;
import net.Indyuce.mmoitems.MMOItems;
import net.Indyuce.mmoitems.api.interaction.weapon.Weapon;
import net.Indyuce.mmoitems.api.player.PlayerData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.entity.Player;

@Deprecated
public class Musket extends Weapon implements LegacyWeapon {
    @Deprecated
    public Musket(Player player, NBTItem item) {
        super(player, item);
    }

    public Musket(PlayerData player, NBTItem item) {
        super(player, item);
    }

    @Override
    public boolean canAttack(boolean rightClick, EquipmentSlot slot) {
        return rightClick;
    }

    @Override
    public void applyAttackEffect(PlayerMetadata stats, EquipmentSlot slot) {
        final double range = requireNonZero(stats.getStat("RANGE"), MMOItems.plugin.getConfig().getDouble("default.range"));
        final double recoil = requireNonZero(stats.getStat("RECOIL"), MMOItems.plugin.getConfig().getDouble("default.recoil"));

        // knockback
        final double knockback = stats.getStat("KNOCKBACK");
        if (knockback > 0)
            getPlayer().setVelocity(getPlayer().getVelocity()
                    .add(getPlayer().getEyeLocation().getDirection().setY(0).normalize().multiply(-1 * knockback).setY(-.2)));

        final double a = Math.toRadians(getPlayer().getEyeLocation().getYaw() + 90 + 45 * (slot == EquipmentSlot.MAIN_HAND ? 1 : -1));
        final Location loc = getPlayer().getLocation().add(Math.cos(a) * .5, 1.5, Math.sin(a) * .5);
        loc.setPitch((float) (loc.getPitch() + (RANDOM.nextDouble() - .5) * 2 * recoil));
        loc.setYaw((float) (loc.getYaw() + (RANDOM.nextDouble() - .5) * 2 * recoil));

        final RayTrace trace = new RayTrace(loc, loc.getDirection(), range, entity -> UtilityMethods.canTarget(stats.getPlayer(), entity, InteractionType.OFFENSE_ACTION));
        if (trace.hasHit())
            stats.attack(trace.getHit(), stats.getStat("ATTACK_DAMAGE"), DamageType.WEAPON, DamageType.PHYSICAL, DamageType.PROJECTILE);

        trace.draw(.5, Color.BLACK);
        getPlayer().getWorld().playSound(getPlayer().getLocation(), Sounds.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2, 2);
    }
}
