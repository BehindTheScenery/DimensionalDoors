package org.dimdev.dimdoors.shared.blocks;

import net.minecraft.block.state.IBlockState;
import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.shared.items.ModItems;
import org.dimdev.dimdoors.shared.rifts.AvailableLink;
import org.dimdev.dimdoors.shared.rifts.WeightedRiftDestination;
import org.dimdev.dimdoors.shared.rifts.destinations.AvailableLinkDestination;
import org.dimdev.dimdoors.shared.rifts.destinations.NewPublicDestination;
import org.dimdev.dimdoors.shared.tileentities.TileEntityEntranceRift;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;

import java.util.*;

public class BlockDimensionalDoorGold extends BlockDimensionalDoor {

    public static final String ID = "gold_dimensional_door";

    public BlockDimensionalDoorGold() {
        super(Material.IRON);
        setHardness(1.0F);
        setUnlocalizedName(ID);
        setRegistryName(new ResourceLocation(DimDoors.MODID, ID));
    }

    @Override
    public Item getItem() {
        return ModItems.GOLD_DIMENSIONAL_DOOR;
    }

    @Override
    public Item getItemDropped(IBlockState state, Random rand, int fortune) {
        return ModBlocks.GOLD_DOOR.getItemDropped(state, rand, fortune);
    }

    @Override
    public void setupRift(TileEntityEntranceRift rift) {
        AvailableLink link = AvailableLink.builder()
                .groups(new HashSet<>(Arrays.asList(0, 1)))
                .linksRemaining(1)
                .replaceDestination(UUID.randomUUID()).build();
        rift.addAvailableLink(link);
        AvailableLinkDestination destination = AvailableLinkDestination.builder()
                .acceptedGroups(Collections.singleton(0))
                .coordFactor(1)
                .negativeDepthFactor(10000)
                .positiveDepthFactor(80)
                .weightMaximum(100)
                .linkId(link.id)
                .noLink(false)
                .newRiftWeight(1).build();
        rift.addWeightedDestination(new WeightedRiftDestination(destination, 1, 0, null, link.replaceDestination));
    }

    @Override
    public boolean canBePlacedOnRift() {
        return true;
    }
}
