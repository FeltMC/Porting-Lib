package io.github.fabricators_of_create.porting_lib.mixin.common;

import io.github.fabricators_of_create.porting_lib.util.PortingHooks;
import io.github.fabricators_of_create.porting_lib.util.client.ClientHooks;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import io.github.fabricators_of_create.porting_lib.extensions.FluidExtensions;
import io.github.fabricators_of_create.porting_lib.extensions.RegistryNameProvider;
import io.github.fabricators_of_create.porting_lib.util.FluidAttributes;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

@Deprecated(forRemoval = true)
@Mixin(Fluid.class)
public abstract class FluidMixin implements FluidExtensions, RegistryNameProvider {
	@Unique
	private FluidAttributes port_lib$fluidAttributes;

	@Unique
	private ResourceLocation port_lib$registryName = null;

	@Override
	public ResourceLocation getRegistryName() {
		if (port_lib$registryName == null) {
			port_lib$registryName = Registry.FLUID.getKey((Fluid) (Object) this);
		}
		return port_lib$registryName;
	}

	@Unique
	@Override
	public final FluidAttributes getAttributes() {
		if (port_lib$fluidAttributes == null) {
			port_lib$fluidAttributes = createAttributes();
		}

		return port_lib$fluidAttributes;
	}
}
