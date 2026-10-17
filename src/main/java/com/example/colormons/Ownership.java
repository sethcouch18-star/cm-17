package com.example.colormons;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.client.Minecraft;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Whether the player may colour a Pokemon.
 *
 * Deliberately conservative in only one direction: it will not claim a Pokemon is
 * wild unless it has positive evidence. Blocking the player's own Pokemon is a worse
 * error than letting a wild one through.
 */
public class Ownership {
    public static final int MINE = 0;
    public static final int WILD = 1;
    public static final int OTHER = 2;

    public static int check(Minecraft mc, PokemonEntity entity) {
        if (mc.player == null || entity == null) return OTHER;
        UUID me = mc.player.getUUID();

        // 1. a Pokemon in my party is mine
        if (inMyParty(mc, entity)) return MINE;

        // 2. an explicit owner UUID decides it
        UUID owner = monOwner(entity);
        if (owner != null) return owner.equals(me) ? MINE : OTHER;

        // 3. no owner recorded anywhere: treat as wild, which is the honest answer
        return WILD;
    }

    /** Reads the party from the player, comparing Pokemon UUIDs. Reflection only. */
    private static boolean inMyParty(Minecraft mc, PokemonEntity entity) {
        try {
            UUID target = entity.getPokemon().getUuid();
            if (target == null) return false;

            for (String accessor : new String[]{"getParty", "party"}) {
                Object party = invoke(mc.player, accessor);
                if (party instanceof Iterable<?> it) {
                    for (Object mon : it) {
                        Object u = invoke(mon, "getUuid");
                        if (u instanceof UUID id && id.equals(target)) return true;
                    }
                }
            }
        } catch (Throwable ignored) {}
        return false;
    }

    /** The Pokemon's owner UUID, or null when the API does not give one. */
    private static UUID monOwner(PokemonEntity entity) {
        Pokemon mon = entity.getPokemon();
        if (mon == null) return null;

        // typed call first, so a rename shows up at compile time rather than silently failing
        try {
            UUID u = mon.getOwnerUUID();
            if (u != null) return u;
        } catch (Throwable ignored) {}

        // a couple of plausible alternative names, checked reflectively
        for (String name : new String[]{"getOriginalTrainerUUID", "getOwnerId"}) {
            Object v = invoke(mon, name);
            if (v instanceof UUID u) return u;
            if (v instanceof String s) {
                try { return UUID.fromString(s); } catch (Exception ignored) {}
            }
        }
        return null;
    }

    private static Object invoke(Object target, String method) {
        if (target == null) return null;
        try {
            Method m = target.getClass().getMethod(method);
            return m.invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }
}
