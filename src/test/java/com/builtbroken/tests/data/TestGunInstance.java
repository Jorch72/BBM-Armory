package com.builtbroken.tests.data;

import com.builtbroken.armory.content.items.ItemAmmo;
import com.builtbroken.armory.content.items.ItemGun;
import com.builtbroken.armory.data.ArmoryDataHandler;
import com.builtbroken.armory.data.ammo.AmmoData;
import com.builtbroken.armory.data.ammo.AmmoType;
import com.builtbroken.armory.data.ammo.ClipData;
import com.builtbroken.armory.data.projectiles.EnumProjectileTypes;
import com.builtbroken.armory.data.ranged.GunData;
import com.builtbroken.armory.data.ranged.GunInstance;
import com.builtbroken.mc.api.data.weapon.IAmmoData;
import com.builtbroken.mc.api.data.weapon.ReloadType;
import com.builtbroken.mc.prefab.inventory.BasicInventory;
import com.builtbroken.mc.testing.junit.AbstractTest;
import com.builtbroken.mc.testing.junit.VoltzTestRunner;
import com.builtbroken.mc.testing.junit.world.FakeWorld;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.junit.runner.RunWith;

import java.io.File;

/**
 * Tests functionality of the gun instance with a revolver like gun
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 11/21/2016.
 */
@RunWith(VoltzTestRunner.class)
public class TestGunInstance extends AbstractTest
{
    static ItemGun itemGun;
    static ItemAmmo itemAmmo;
    static GunData gunData;

    public void testInit()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
        assertSame(gunData, instance.getGunData());
        assertNotNull(instance.getLoadedClip());
        assertEquals(0, instance.getLoadedClip().getAmmoCount());
    }

    public void testFireWeapon()
    {
        FakeWorld world = FakeWorld.newWorld("gunInstanceWeaponFireTest");
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, world);
        instance.getLoadedClip().loadAmmo((IAmmoData) ArmoryDataHandler.INSTANCE.get("ammo").get("ammo0"), 6);

        for (int i = 0; i < 6; i++)
        {
            instance.lastTimeFired = 0L;
            instance.fireWeapon(stack, world, i);
            if (i != 5)
            {
                assertEquals("Failed to decrease ammo for shot " + i, 4 - i, instance.getLoadedClip().getAmmoCount());
                assertNotNull(instance.chamberNextRound());
            }
            else
            {
                assertEquals(0, instance.getLoadedClip().getAmmoCount());
            }
        }
    }

    public void testHasAmmo()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
        assertFalse(instance.hasAmmo());
        instance.getLoadedClip().loadAmmo((IAmmoData) ArmoryDataHandler.INSTANCE.get("ammo").get("ammo0"), 1);
        assertTrue(instance.hasAmmo());
    }

    public void testReloadWeapon()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        //Over tested because I'm bored
        for (int i = 0; i < 30; i++)
        {
            GunInstance instance = newInstance(stack, null);
            assertFalse("" + i, instance.hasAmmo());

            IInventory inventory = new BasicInventory(30);
            inventory.setInventorySlotContents(i, new ItemStack(itemAmmo, 64, 0));
            assertEquals("" + i, 64, inventory.getStackInSlot(i).stackSize);
            instance.reloadWeapon(inventory);

            assertTrue("" + i, instance.hasAmmo());
            assertTrue("" + i, instance.getLoadedClip().getAmmoCount() == instance.getLoadedClip().getMaxAmmo());
            assertEquals("" + i, 64 - instance.getLoadedClip().getMaxAmmo(), inventory.getStackInSlot(i).stackSize);
        }

        //Over tested because I'm bored
        for (int i = 0; i < 20; i++)
        {
            GunInstance instance = newInstance(stack, null);
            assertFalse(instance.hasAmmo());

            IInventory inventory = new BasicInventory(30);
            inventory.setInventorySlotContents(i, new ItemStack(itemAmmo, 1, 0));
            inventory.setInventorySlotContents(i + 2, new ItemStack(itemAmmo, 1, 0));
            inventory.setInventorySlotContents(i + 4, new ItemStack(itemAmmo, 1, 0));
            inventory.setInventorySlotContents(i + 6, new ItemStack(itemAmmo, 1, 0));
            inventory.setInventorySlotContents(i + 8, new ItemStack(itemAmmo, 1, 0));
            instance.reloadWeapon(inventory);

            assertTrue("" + i, instance.hasAmmo());
            assertTrue("" + i, instance.getLoadedClip().getAmmoCount() == 5);
            assertNull("" + i, inventory.getStackInSlot(i));
            assertNull("" + i, inventory.getStackInSlot(i + 2));
            assertNull("" + i, inventory.getStackInSlot(i + 4));
            assertNull("" + i, inventory.getStackInSlot(i + 6));
            assertNull("" + i, inventory.getStackInSlot(i + 8));
        }

        //Over tested because I'm bored
        for (int i = 0; i < 20; i++)
        {
            GunInstance instance = newInstance(stack, null);
            assertFalse("" + i, instance.hasAmmo());

            IInventory inventory = new BasicInventory(30);
            inventory.setInventorySlotContents(i, new ItemStack(itemAmmo, 2, 0));
            inventory.setInventorySlotContents(i + 8, new ItemStack(itemAmmo, 3, 0));
            instance.reloadWeapon(inventory);

            assertTrue("" + i, instance.hasAmmo());
            assertTrue("" + i, instance.getLoadedClip().getAmmoCount() == 5);
            assertNull("" + i, inventory.getStackInSlot(i));
            assertNull("" + i, inventory.getStackInSlot(i + 8));
        }
    }

    public void testUnloadWeapon()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
    }

    public void testIsManuallyFeedClip()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
    }

    public void testSave()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
    }

    public void testLoad()
    {
        ItemStack stack = new ItemStack(itemGun, 1, 0);
        GunInstance instance = newInstance(stack, null);
    }

    private GunInstance newInstance(ItemStack stack, World world)
    {
        return new GunInstance(stack, new EntityZombie(world), gunData);
    }

    @Override
    public void setUpForEntireClass()
    {
        itemGun = new ItemGun();
        itemAmmo = new ItemAmmo();

        File folder = new File(System.getProperty("user.dir"), "tmp");
        ArmoryDataHandler.INSTANCE.add(new ArmoryDataHandler.ArmoryData(folder, "ammo"));
        ArmoryDataHandler.INSTANCE.add(new ArmoryDataHandler.ArmoryData(folder, "ammoType"));
        ArmoryDataHandler.INSTANCE.add(new ArmoryDataHandler.ArmoryData(folder, "gun"));
        ArmoryDataHandler.INSTANCE.add(new ArmoryDataHandler.ArmoryData(folder, "clip"));

        AmmoType ammoType = new AmmoType("9mm", "9mm", EnumProjectileTypes.BULLET);
        ArmoryDataHandler.INSTANCE.get("ammoType").add(ammoType);

        gunData = new GunData("gun", "handgun", "revolver", ammoType, ReloadType.HAND_FEED, new ClipData("revolverClip", "revolverClip", ReloadType.HAND_FEED, ammoType, 6));
        itemGun.metaToData.put(0, gunData);
        ArmoryDataHandler.INSTANCE.get("gun").add(gunData);

        for (int i = 0; i < 5; i++)
        {
            AmmoData data = new AmmoData("ammo" + i, "ammo" + i, ammoType, "impact", 5 + i, 100);
            ArmoryDataHandler.INSTANCE.get("ammo").add(data);
            itemAmmo.metaToData.put(i, data);
        }
    }

    @Override
    public void tearDownForEntireClass()
    {
        ArmoryDataHandler.INSTANCE.DATA.remove("ammo");
        ArmoryDataHandler.INSTANCE.DATA.remove("ammoType");
        ArmoryDataHandler.INSTANCE.DATA.remove("gun");
        ArmoryDataHandler.INSTANCE.DATA.remove("clip");
    }
}