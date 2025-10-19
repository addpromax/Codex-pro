package net.Indyuce.mmoitems.stat.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Version string is MAJOR.MINOR.PATCH
 * <p>
 * This annotation indicates the LOWEST VERSION at which
 * the given feature is available. Usually, it's the
 * version where some non-backwards compatible feature was
 * implemented into Minecraft or Spigot.
 *
 * @author Jules
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionDependant {

    public int[] version();
}
