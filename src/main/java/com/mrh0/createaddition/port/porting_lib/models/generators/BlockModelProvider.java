package com.mrh0.createaddition.port.porting_lib.models.generators;

import com.mrh0.createaddition.port.porting_lib.data.ExistingFileHelper;
import net.minecraft.data.PackOutput;

/** Datagen stub; see {@link com.mrh0.createaddition.port.porting_lib.DatagenStubs}. */
public abstract class BlockModelProvider extends ModelProvider<BlockModelBuilder> {
    public BlockModelProvider(final PackOutput output, final String modid, final ExistingFileHelper existingFileHelper) {
        super(output, modid, BLOCK_FOLDER, BlockModelBuilder::new, existingFileHelper);
    }
}
