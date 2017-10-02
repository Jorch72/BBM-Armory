package com.builtbroken.armory.data.damage.effect;

import com.builtbroken.armory.Armory;
import com.builtbroken.armory.data.damage.DamageData;
import com.builtbroken.jlib.data.Colors;
import com.builtbroken.mc.core.Engine;
import com.builtbroken.mc.framework.entity.effect.EntityEffect;
import com.builtbroken.mc.framework.entity.effect.EntityEffectHandler;
import com.builtbroken.mc.framework.json.imp.IJsonProcessor;
import com.builtbroken.mc.framework.json.loading.JsonProcessorData;
import com.builtbroken.mc.lib.debug.DebugHelper;
import com.builtbroken.mc.lib.helper.LanguageUtility;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Damage type that applied an entity effect to the entity
 *
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 9/30/2017.
 */
public class DamageEntityEffect extends DamageData
{
    /** Unique lookup id of the entity effect */
    public final String effectID;

    /** Data to encode into the effect */
    @JsonProcessorData(value = "data", type = "nbt")
    public NBTTagCompound effectData;

    /** Unlocalized string to use when displaying the name of the effect, generated by an empty effect object */
    protected String displayText;

    public DamageEntityEffect(IJsonProcessor processor, String id)
    {
        super(processor);
        this.effectID = id;
    }

    @Override
    public boolean onImpact(Entity attacker, Entity entity, double hitX, double hitY, double hitZ, float velocity, float scale)
    {
        if (!attacker.worldObj.isRemote && entity instanceof EntityLivingBase)
        {
            EntityEffect entityEffect = EntityEffectHandler.create(effectID, entity);
            if (entityEffect != null)
            {
                if (effectData != null)
                {
                    entityEffect.load(effectData);
                }
                EntityEffectHandler.applyEffect(entity, entityEffect);
            }
            else
            {
                DebugHelper.outputMethodDebug(Armory.INSTANCE.logger(), "doImpact", "\nnull entityEffect for id '" + effectID + "'", attacker, hitX, hitY, hitZ, velocity, scale);
            }
        }
        return true;
    }

    @Override
    public String getDisplayString()
    {
        if (displayText == null)
        {
            EntityEffect entityEffect = EntityEffectHandler.create(effectID, null);
            if (entityEffect != null)
            {
                displayText = entityEffect.getUnlocalizedName();
            }
            else
            {
                displayText = "null";
            }
        }
        if ("null".equalsIgnoreCase(displayText))
        {
            return Engine.runningAsDev ? Colors.RED.code + "Error: effect " + effectID : null;
        }
        String translation = LanguageUtility.getLocal(displayText);
        if (translation != null)
        {
            translation = translation.trim();
            if (!translation.isEmpty())
            {
                return translation;
            }
        }
        return displayText;
    }

    @Override
    public String toString()
    {
        return "DamageEntityEffect[" + effectID + "]@" + hashCode();
    }
}
