package com.mrh0.createaddition.blocks.liquid_blaze_burner;

import java.util.function.Consumer;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Movement behaviour of the liquid blaze burner, like Create's BlazeBurnerMovementBehaviour: the block entity is not
 * rendered on contraptions, LiquidBlazeBurnerMovementRender (client, attached in CreateAdditionClient) draws the head
 * and animates it. Upstream had no movement behaviour here (the virtual block entity was rendered instead).
 */
public class LiquidBlazeBurnerMovement extends MovementBehaviour {

	/** Set by the client entrypoint; the common class must not reference client classes. */
	public static Consumer<MovementContext> clientTick = ctx -> {};

	@Override
	@Nullable
	public ItemStack canBeDisabledVia(MovementContext context) {
		return null;
	}

	@Override
	public void tick(MovementContext context) {
		if (!context.world.isClientSide()) return;
		clientTick.accept(context);
	}

	@Override
	public boolean disableBlockEntityRendering() {
		return true;
	}
}
