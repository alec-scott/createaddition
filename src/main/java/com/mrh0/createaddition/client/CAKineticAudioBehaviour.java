package com.mrh0.createaddition.client;

import com.zurrtum.create.client.foundation.blockEntity.behaviour.audio.KineticAudioBehaviour;
import com.zurrtum.create.content.kinetics.base.KineticBlockEntity;

import java.util.function.Consumer;

/** PORT-NOTE(26.1): Create Fly's kinetic audio (a client behaviour) plus the block entity's own sound (was tickAudio). */
public class CAKineticAudioBehaviour<T extends KineticBlockEntity> extends KineticAudioBehaviour<T> {
    private final Consumer<T> extra;

    public CAKineticAudioBehaviour(T be, Consumer<T> extra) {
        super(be);
        this.extra = extra;
    }

    @Override
    public void tickAudio() {
        super.tickAudio();
        extra.accept(blockEntity);
    }
}
