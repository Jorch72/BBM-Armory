package com.builtbroken.armory.content.sentry;

import com.builtbroken.armory.Armory;
import com.builtbroken.armory.content.sentry.ai.SentryEntityTargetSorter;
import com.builtbroken.armory.content.sentry.imp.ISentryHost;
import com.builtbroken.armory.data.ranged.GunInstance;
import com.builtbroken.armory.data.sentry.SentryData;
import com.builtbroken.armory.data.user.IWeaponUser;
import com.builtbroken.jlib.data.network.IByteBufReader;
import com.builtbroken.jlib.data.network.IByteBufWriter;
import com.builtbroken.mc.api.ISave;
import com.builtbroken.mc.api.IWorldPosition;
import com.builtbroken.mc.api.tile.provider.IInventoryProvider;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.core.network.packet.PacketSpawnParticle;
import com.builtbroken.mc.imp.transform.rotation.EulerAngle;
import com.builtbroken.mc.imp.transform.rotation.IRotation;
import com.builtbroken.mc.imp.transform.vector.Pos;
import com.builtbroken.mc.prefab.entity.selector.EntitySelectors;
import io.netty.buffer.ByteBuf;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import java.util.Collections;
import java.util.List;

/**
 * Actual sentry object that handles most of the functionality of the sentry
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/11/2017.
 */
public class Sentry implements IWorldPosition, IRotation, IWeaponUser, ISave, IByteBufReader, IByteBufWriter
{
    //TODO implement log system (enemy detected, enemy killed, ammo consumed, power failed, etc. with time stamps and custom log limits)
    /** Desired aim angle, updated every tick if target != null */
    protected final EulerAngle aim = new EulerAngle(0, 0, 0);
    /** Current aim angle, updated each tick */
    protected final EulerAngle currentAim = new EulerAngle(0, 0, 0);
    /** Default aim to use when not targeting things */
    protected final EulerAngle defaultAim = new EulerAngle(0, 0, 0);
    /** Data that defines this sentry instance */
    protected final SentryData sentryData;
    /** Current host of this sentry */
    protected ISentryHost host;


    public Pos center;
    public Pos aimPoint;
    public Pos bulletSpawnOffset;

    protected Entity target;
    public GunInstance gunInstance;

    protected int targetSearchTimer = 0;
    protected int targetingDelay = 0;
    protected int targetingLoseTimer = 0;

    /** Areas to search for targets */
    public AxisAlignedBB searchArea;

    /** Offset to use to prevent clipping self with ray traces */
    public double halfWidth = 0;

    //States
    public String status;
    public boolean reloading = false;
    public boolean running = false;
    public boolean turnedOn = true;
    public boolean sentryHasAmmo = false;
    public boolean sentryIsAlive = false;

    public Sentry(SentryData sentryData)
    {
        this.sentryData = sentryData;
    }

    public boolean update(int ticks, float deltaTime)
    {
        //Update logic every other tick
        if (!world().isRemote && ticks % 2 == 0)
        {
            //Calculate bullet offset
            calculateBulletSpawnOffset();

            status = "unknown";

            //Invalid entity
            if (host == null || getSentryData() == null)
            {
                status = "invalid";
            }
            else
            {
                //Reset state system
                running = false;
                sentryHasAmmo = false;
                sentryIsAlive = false;
                //Debug
                if (Engine.runningAsDev)
                {
                    Pos hand = center.add(bulletSpawnOffset);
                    PacketSpawnParticle packetSpawnParticle = new PacketSpawnParticle("smoke", world().provider.dimensionId, hand.x(), hand.y(), hand.z(), 0, 0, 0);
                    Engine.instance.packetHandler.sendToAll(packetSpawnParticle);

                    packetSpawnParticle = new PacketSpawnParticle("flame", world().provider.dimensionId, center.x(), center.y(), center.z(), 0, 0, 0);
                    Engine.instance.packetHandler.sendToAll(packetSpawnParticle);
                }

                //Create gun instance if null
                if (gunInstance == null && getSentryData() != null && getSentryData().getGunData() != null)
                {
                    //TODO get real gun instance
                    gunInstance = new GunInstance(new ItemStack(Armory.blockSentry), this, getSentryData().getGunData());
                    if (Engine.runningAsDev)
                    {
                        gunInstance.doDebugRayTracesOnTthisGun = true;
                    }
                }

                //Can only function if we have a gun
                if (gunInstance != null)
                {
                    if (!gunInstance.getGunData().getReloadType().requiresItems() && sentryData.getAmmoData() != null)
                    {
                        gunInstance.chamberedRound = sentryData.getAmmoData();
                    }
                    //Trigger reload mod if out of ammo
                    if (!gunInstance.hasAmmo() && gunInstance.getChamberedRound() == null)
                    {
                        reloading = true;
                    }

                    if (reloading)
                    {
                        status = "reloading";
                        loadAmmo();
                        if (gunInstance.isFullOnAmmo())
                        {
                            reloading = false;
                        }
                    }
                    else
                    {
                        //If no target try to find one
                        if (target == null)
                        {
                            status = "searching";
                            targetingDelay = 0;
                            targetingLoseTimer = 0;

                            if (targetSearchTimer++ >= getSentryData().getTargetSearchDelay())
                            {
                                targetSearchTimer = 0;
                                findTargets();
                            }
                        }
                        //If target and valid try to attack
                        else if (isValidTarget(target))
                        {
                            status = "aiming";
                            //Delay before attack
                            if (targetingDelay >= getSentryData().getTargetAttackDelay())
                            {
                                //Update aim point
                                aimPoint = getAimPoint(target);

                                aim.set(center.toEulerAngle(aimPoint).clampTo360());

                                if (isAimed())
                                {
                                    status = "attacking";
                                    fireAtTarget();
                                }
                                else
                                {
                                    aimAtTarget(deltaTime);
                                }
                            }
                            else
                            {
                                targetingDelay++;
                            }
                        }
                        //If target is not null and invalid, count until invalidated
                        else if (target != null && targetingLoseTimer++ >= getSentryData().getTargetLossTimer())
                        {
                            status = "target lost";
                            target = null;
                            targetingLoseTimer = 0;
                        }
                    }
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Callculates the offset point to use
     * for ray tracing and bullet spawning
     */
    protected void calculateBulletSpawnOffset()
    {
        float yaw = (float) currentAim.yaw();
        while (yaw < 0)
        {
            yaw += 360;
        }
        while (yaw > 360)
        {
            yaw -= 360;
        }
        final double radianYaw = Math.toRadians(-yaw - 45 - 90);

        float pitch = (float) currentAim.pitch();
        while (pitch < 0)
        {
            pitch += 360;
        }
        while (pitch > 360)
        {
            pitch -= 360;
        }
        final double radianPitch = Math.toRadians(pitch);

        float width = (float) Math.max(sentryData != null ? sentryData.getBarrelLength() : 0, halfWidth);

        bulletSpawnOffset = new Pos(
                (Math.cos(radianYaw) - Math.sin(radianYaw)) * width,
                (Math.sin(radianYaw) * Math.sin(radianPitch)) * width,
                (Math.sin(radianYaw) + Math.cos(radianYaw)) * width
        );

        if (sentryData != null && sentryData.getBarrelOffset() != null)
        {
            bulletSpawnOffset = bulletSpawnOffset.add(sentryData.getBarrelOffset());
        }
    }

    protected void loadAmmo()
    {
        if (gunInstance != null && getInventory() != null)
        {
            if (!gunInstance.reloadWeapon(getInventory(), true))
            {
                reloading = false;
            }
        }
    }

    protected void findTargets()
    {
        //TODO thread
        if (searchArea == null)
        {
            searchArea = AxisAlignedBB.getBoundingBox(x(), y(), z(), x(), y(), z()).expand(getSentryData().getRange(), getSentryData().getRange(), getSentryData().getRange());
        }

        List<Entity> entityList = world().getEntitiesWithinAABBExcludingEntity(host instanceof Entity ? (Entity) host : null, searchArea, getEntitySelector());
        Collections.sort(entityList, new SentryEntityTargetSorter(center));

        if (entityList != null && entityList.size() > 0)
        {
            //TODO sort by distance
            //TODO sort by hp & armor
            //TODO sort by threat
            //TODO add settings to control sorting
            for (Entity entity : entityList)
            {
                if (entity.isEntityAlive() && isValidTarget(entity))
                {
                    target = entity;
                    break;
                }
            }
        }
    }

    protected IEntitySelector getEntitySelector()
    {
        return EntitySelectors.MOB_SELECTOR.selector();
    }

    /**
     * Checks if the entity is valid
     * <p>
     * Checks distance
     * Checks ray trace
     * Checks for life
     * Checks if can attack
     * Checks if matches target type
     *
     * @param entity - potential or existing target
     * @return true if valid
     */
    protected boolean isValidTarget(Entity entity)
    {
        if (entity != null && entity.isEntityAlive())
        {
            //Get aim position of entity
            final Pos aimPoint = getAimPoint(entity); //TODO retry with lower and higher aim value

            //Check to ensure we are in range
            double distance = center.distance(aimPoint);
            if (distance <= getSentryData().getRange())
            {
                //Trace to make sure no blocks are between shooter and target
                EulerAngle aim = center.toEulerAngle(aimPoint).clampTo360();
                MovingObjectPosition hit = center.add(aim.toPos().multiply(1.3)).rayTraceBlocks(world(), aimPoint);

                return hit == null || hit.typeOfHit == MovingObjectPosition.MovingObjectType.MISS;
            }
        }
        return false;
    }

    /**
     * Gets the point to aim at the target
     *
     * @param entity
     * @return
     */
    protected Pos getAimPoint(Entity entity)
    {
        float height = (entity.height / 2f);
        return new Pos(entity.posX, entity.posY + height, entity.posZ);
    }

    /**
     * Called to aim at the current target
     */
    protected void aimAtTarget(float deltaTime)
    {
        //Aim at point
        currentAim.moveTowards(aim, getSentryData().getRotationSpeed(), deltaTime).clampTo360();
    }

    /**
     * Called to check if we are aimed at the target
     *
     * @return
     */
    protected boolean isAimed()
    {
        return aim.isWithin(currentAim, getSentryData().getRotationSpeed());
    }

    /**
     * Called to fire at the target
     */
    protected void fireAtTarget()
    {
        //Debug
        gunInstance.debugRayTrace(center, aim.toPos(), aimPoint, bulletSpawnOffset, (float) currentAim.yaw(), (float) currentAim.pitch());

        //Check fi has ammo, then fire
        if (gunInstance.hasAmmo())
        {
            gunInstance.fireWeapon(world(), 1, aimPoint, aim.toPos()); //TODO get firing ticks
        }
    }

    public SentryData getSentryData()
    {
        return sentryData;
    }

    @Override
    public World world()
    {
        return host != null ? host.world() : null;
    }

    @Override
    public double x()
    {
        return host != null ? host.x() : 0;
    }

    @Override
    public double y()
    {
        return host != null ? host.y() : 0;
    }

    @Override
    public double z()
    {
        return host != null ? host.z() : 0;
    }

    @Override
    public double yaw()
    {
        return currentAim.yaw();
    }

    @Override
    public double pitch()
    {
        return currentAim.pitch();
    }

    @Override
    public double roll()
    {
        return currentAim.roll();
    }

    @Override
    public Entity getShooter()
    {
        return host instanceof Entity ? (Entity) host : null;
    }

    @Override
    public Pos getEntityPosition()
    {
        return center;
    }

    @Override
    public IInventory getInventory()
    {
        if (host instanceof IInventory)
        {
            return (IInventory) host;
        }
        else if (host instanceof IInventoryProvider)
        {
            return ((IInventoryProvider) host).getInventory();
        }
        return null;
    }

    @Override
    public boolean isAmmoSlot(int slot)
    {
        return slot >= getSentryData().getInventoryAmmoStart() && slot <= getSentryData().getInventoryAmmoEnd();
    }

    @Override
    public void load(NBTTagCompound nbt)
    {

    }

    @Override
    public NBTTagCompound save(NBTTagCompound nbt)
    {
        //TODO save state
        //TODO save gun
        return nbt;
    }

    @Override
    public Sentry readBytes(ByteBuf buf)
    {
        return this;
    }

    @Override
    public ByteBuf writeBytes(ByteBuf buf)
    {
        //TODO Sync state(s)
        //TODO Sync ammo
        return buf;
    }
}
