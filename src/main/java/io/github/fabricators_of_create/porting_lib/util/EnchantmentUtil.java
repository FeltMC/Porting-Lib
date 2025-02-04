package io.github.fabricators_of_create.porting_lib.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;

public class EnchantmentUtil {
	public static final Map<Item, Set<Supplier<Enchantment>>> ITEMS_TO_ENCHANTS = new HashMap<>();

	public static void addCompat(Item item, Supplier<Enchantment>... enchants) {
		Set<Supplier<Enchantment>> set = ITEMS_TO_ENCHANTS.computeIfAbsent(item, item2 -> new HashSet<>());
		Collections.addAll(set, enchants);
	}
}
