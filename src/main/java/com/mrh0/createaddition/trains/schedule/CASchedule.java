package com.mrh0.createaddition.trains.schedule;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.trains.schedule.condition.EnergyThresholdCondition;
import com.zurrtum.create.AllSchedules;
import net.minecraft.resources.Identifier;
import com.zurrtum.create.content.trains.schedule.condition.ScheduleWaitCondition;
import com.zurrtum.create.catnip.data.Pair;

import java.util.function.Function;

public class CASchedule {
    static {
        registerCondition("energy_threshold", EnergyThresholdCondition::new);
    }

    private static void registerCondition(String name, Function<Identifier, ? extends ScheduleWaitCondition> factory) {
        AllSchedules.CONDITION_TYPES.add(Pair.of(CreateAddition.asResource(name), factory));
    }

    public static void register() {}

    // PORT-NOTE(26.1): client renders for schedule entries (Create Fly's AllScheduleRenders), called from CreateAdditionClient
    public static void registerRenders() {
        com.zurrtum.create.client.AllScheduleRenders.ALL.put(CreateAddition.asResource("energy_threshold"),
                new com.mrh0.createaddition.trains.schedule.condition.EnergyThresholdConditionRender());
    }
}
