package io.github.fabricators_of_create.porting_lib.event;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.player.LocalPlayer;

@Environment(EnvType.CLIENT)
public interface AttackAirCallback {
	Event<AttackAirCallback> EVENT = EventFactory.createArrayBacked(AttackAirCallback.class, callbacks -> (player) -> {
		for (AttackAirCallback callback : callbacks) {
			callback.attackAir(player);
		}
	});

	void attackAir(LocalPlayer player);
}
