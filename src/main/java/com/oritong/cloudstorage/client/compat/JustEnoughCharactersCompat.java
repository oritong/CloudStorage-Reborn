package com.oritong.cloudstorage.client.compat;

import com.oritong.cloudstorage.CloudStorage;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;

public final class JustEnoughCharactersCompat {
    private static Method matcher = findMatcher();

    private JustEnoughCharactersCompat() {
    }

    private static Method findMatcher() {
        if (!ModList.get().isLoaded("jecharacters")) return null;
        try {
            return Class.forName("me.towdium.jecharacters.utils.Match")
                    .getMethod("contains", String.class, CharSequence.class);
        } catch (ReflectiveOperationException | LinkageError e) {
            CloudStorage.LOGGER.warn("Could not enable Just Enough Characters search; using normal search", e);
            return null;
        }
    }

    public static boolean isAvailable() {
        return matcher != null;
    }

    public static boolean contains(String text, String query) {
        if (text.contains(query)) return true;
        if (matcher != null) {
            try {
                return (boolean) matcher.invoke(null, text, query);
            } catch (ReflectiveOperationException | LinkageError e) {
                matcher = null;
                CloudStorage.LOGGER.warn("Just Enough Characters search failed; using normal search", e);
            }
        }
        return false;
    }
}
