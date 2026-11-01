package com.mrh0.createaddition.ponder;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.index.CAPonders;
import com.zurrtum.create.client.foundation.ponder.PonderWorldBlockEntityFix;

import com.zurrtum.create.client.ponder.api.level.PonderLevel;
import com.zurrtum.create.client.ponder.api.registration.IndexExclusionHelper;
import com.zurrtum.create.client.ponder.api.registration.PonderPlugin;
import com.zurrtum.create.client.ponder.api.registration.PonderSceneRegistrationHelper;
import com.zurrtum.create.client.ponder.api.registration.PonderTagRegistrationHelper;
import com.zurrtum.create.client.ponder.api.registration.SharedTextRegistrationHelper;
import net.minecraft.resources.Identifier;

public class CAPonderPlugin implements PonderPlugin {

    @Override
    public String getModId() {
        return CreateAddition.MODID;
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<Identifier> helper) {
        CAPonders.registerScenes(helper);
    }

    @Override
    public void registerTags(PonderTagRegistrationHelper<Identifier> helper) {
        CAPonders.registerTags(helper);
    }

    @Override
    public void registerSharedText(SharedTextRegistrationHelper helper) {
        // helper.registerSharedText("rpm8", "8 RPM");
    }

    @Override
    public void indexExclusions(IndexExclusionHelper helper) {
        // helper.excludeBlockVariants(ValveHandleBlock.class, AllBlocks.COPPER_VALVE_HANDLE.get());
    }
}
