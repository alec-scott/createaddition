package com.mrh0.createaddition.index;

import com.mrh0.createaddition.CreateAddition;
import com.mrh0.createaddition.blocks.alternator.AlternatorBlockEntity;
import com.mrh0.createaddition.item.CapacitorItem;
import com.mrh0.createaddition.blocks.connector.LargeConnectorBlockEntity;
import com.mrh0.createaddition.blocks.connector.SmallConnectorBlockEntity;
import com.mrh0.createaddition.blocks.connector.SmallLightConnectorBlockEntity;
import com.mrh0.createaddition.blocks.creative_energy.CreativeEnergyBlockEntity;
import com.mrh0.createaddition.blocks.electric_motor.ElectricMotorBlockEntity;
import com.mrh0.createaddition.blocks.electric_pump.ElectricPumpBlockEntity;
import com.mrh0.createaddition.blocks.servo_motor.ServoMotorBlockEntity;
import com.mrh0.createaddition.blocks.liquid_blaze_burner.LiquidBlazeBurnerBlockEntity;
import com.mrh0.createaddition.blocks.modular_accumulator.ModularAccumulatorBlockEntity;
import com.mrh0.createaddition.blocks.portable_energy_interface.PortableEnergyInterfaceBlockEntity;
import com.mrh0.createaddition.blocks.rolling_mill.RollingMillBlockEntity;
import com.mrh0.createaddition.blocks.tesla_coil.TeslaCoilBlockEntity;

import com.mrh0.createaddition.port.neoforge.capabilities.RegisterCapabilitiesEvent;

public class CACapabilities {
    // PORT-NOTE(26.1): called from CreateAddition.onInitialize (was a mod-bus RegisterCapabilitiesEvent listener)
    public static void register() {
        RegisterCapabilitiesEvent event = RegisterCapabilitiesEvent.INSTANCE;
        AlternatorBlockEntity.registerCapabilities(event);
        LargeConnectorBlockEntity.registerCapabilities(event);
        SmallConnectorBlockEntity.registerCapabilities(event);
        SmallLightConnectorBlockEntity.registerCapabilities(event);
        ElectricMotorBlockEntity.registerCapabilities(event);
        ElectricPumpBlockEntity.registerCapabilities(event);
        ServoMotorBlockEntity.registerCapabilities(event);
        CreativeEnergyBlockEntity.registerCapabilities(event);
        ModularAccumulatorBlockEntity.registerCapabilities(event);
        PortableEnergyInterfaceBlockEntity.registerCapabilities(event);
        TeslaCoilBlockEntity.registerCapabilities(event);
        RollingMillBlockEntity.registerCapabilities(event);
        LiquidBlazeBurnerBlockEntity.registerCapability(event);
        CapacitorItem.registerCapabilities(event);
        if (CreateAddition.CC_ACTIVE) com.mrh0.createaddition.compat.computercraft.Peripherals.registerPeripheralCapabilities();
        // PORT-NOTE(26.1): Simulated capabilities are excluded for now
    }
}
