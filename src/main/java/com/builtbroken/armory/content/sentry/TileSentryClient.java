package com.builtbroken.armory.content.sentry;

import com.builtbroken.armory.Armory;
import com.builtbroken.armory.content.sentry.gui.GuiSentry;
import com.builtbroken.mc.client.SharedAssets;
import com.builtbroken.mc.core.References;
import com.builtbroken.mc.core.network.packet.PacketType;
import com.builtbroken.mc.imp.transform.vector.Pos;
import com.builtbroken.mc.lib.render.RenderUtility;
import com.builtbroken.mc.lib.render.model.loader.EngineModelLoader;
import com.builtbroken.mc.prefab.tile.Tile;
import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;
import org.lwjgl.opengl.GL11;

import java.awt.*;

/**
 * @see <a href="https://github.com/BuiltBrokenModding/VoltzEngine/blob/development/license.md">License</a> for what you can and can't do with the code.
 * Created by Dark(DarkGuardsman, Robert) on 4/10/2017.
 */
public class TileSentryClient extends TileSentry
{
    private IModelCustom sentryBackupModel = EngineModelLoader.loadModel(new ResourceLocation(Armory.DOMAIN, References.MODEL_PATH + "test.turret.tcn"));

    @Override
    public Tile newTile()
    {
        return new TileSentryClient();
    }

    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player)
    {
        return new GuiSentry(player, this);
    }

    @Override
    public boolean read(ByteBuf buf, int id, EntityPlayer player, PacketType type)
    {
        if (!super.read(buf, id, player, type))
        {
            if (id == 2)
            {
                int entityID = buf.readInt();
                setEntity(entityID);
                return true;
            }
            return false;
        }
        return true;
    }


    @Override
    public void readDescPacket(ByteBuf buf)
    {
        super.readDescPacket(buf);
        int entityID = buf.readInt();
        setEntity(entityID);
    }

    protected void setEntity(int entityID)
    {
        if (entityID >= 0)
        {
            Entity entity = world().getEntityByID(entityID);
            if (entity instanceof EntitySentry)
            {
                setSentry((EntitySentry) entity);
            }
        }
        else
        {
            setSentry(null);
        }
    }

    @SideOnly(Side.CLIENT)
    public void renderDynamic(Pos pos, float frame, int pass)
    {
        double rx = pos.x() + 0.5;
        double ry = pos.y() + 0.5;
        double rz = pos.z() + 0.5;

        final String[] parts = new String[]{"LeftFace", "RightFace", "Head", "Barrel", "BarrelBrace", "BarrelCap"};
        if (getSentry() != null)
        {
            RenderUtility.renderFloatingText("Yaw: " + getSentry().rotationYaw, rx, ry + 1.3, rz, Color.red.getRGB());
            RenderUtility.renderFloatingText("Pitch: " + getSentry().rotationPitch, rx, ry + 1, rz, Color.red.getRGB());

            GL11.glPushMatrix();
            GL11.glTranslated(rx, ry, rz);
            FMLClientHandler.instance().getClient().renderEngine.bindTexture(SharedAssets.GREY_TEXTURE);


            //Render base
            sentryBackupModel.renderAllExcept(parts);

            //Render turret
            GL11.glRotated(getSentry().rotationYaw, 0, 1, 0);
            GL11.glRotated(getSentry().rotationPitch, 1, 0, 0);
            sentryBackupModel.renderOnly(parts);

            GL11.glPopMatrix();
        }
    }
}
