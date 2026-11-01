package com.mrh0.createaddition.energy;

import com.zurrtum.create.api.behaviour.movement.MovementBehaviour;
import com.zurrtum.create.content.contraptions.behaviour.MovementContext;

public class NodeMovementBehaviour extends MovementBehaviour {
	
	@Override
	public void startMoving(MovementContext context) {
		// Mark this tileentity as a contraption.
		context.blockEntityData.putBoolean("contraption", true);
	}
}
