package com.xtoxray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import net.minecraft.core.BlockPos;
import com.xtoxray.client.gui.XrayConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=XtoXray.MOD_ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class XrayClient {
    public static final Lazy<KeyMapping> TOGGLE_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_X,"key.categories.xtoxray"));
    public static final Lazy<KeyMapping> VEIN_MINER_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.vein_miner",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.xtoxray"));
    public static final Lazy<KeyMapping> OPEN_MENU_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.open_config",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"key.categories.xtoxray"));
    public static final Lazy<KeyMapping> COLOR_MODE_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.color_mode",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_C,"key.categories.xtoxray"));
    private static long lastX=Long.MIN_VALUE,lastZ=Long.MIN_VALUE; private static int lastY=Integer.MIN_VALUE;
    private static boolean hasRenderCenter=false;
    private XrayClient(){}
    @SubscribeEvent public static void onClientTick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        while(OPEN_MENU_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)mc.setScreen(new XrayConfigScreen(null));
        while(TOGGLE_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)toggleXray(mc);
        while(VEIN_MINER_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)toggleVeinMiner(mc);
        while(COLOR_MODE_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)toggleColorMode();
        if(mc.level!=null&&mc.player!=null){
            XrayState state=XrayState.getInstance();
            BlockPos pos=mc.player.blockPosition();

            if(state.isActive() && hasRenderCenter){
                int oldCenterX=state.getRenderCenterX();
                int oldCenterY=state.getRenderCenterY();
                int oldCenterZ=state.getRenderCenterZ();

                long x=pos.getX()>>4;
                int y=pos.getY()>>4;
                long z=pos.getZ()>>4;

                if(x!=lastX||y!=lastY||z!=lastZ){
                    lastX=x;
                    lastY=y;
                    lastZ=z;

                    rebuildChangedSections(mc,
                            oldCenterX,oldCenterY,oldCenterZ,
                            pos.getX(),pos.getY(),pos.getZ(),
                            state.getOreRenderDistance());
                }
            }

            state.updateRenderCenter(pos);
            hasRenderCenter=true;
        }

        ColorXrayRenderer.tick(mc);
    }

    public static void toggleXrayFromGui(Minecraft mc){if(mc.player!=null&&mc.level!=null)toggleXray(mc);}
    public static void toggleVeinMinerFromGui(Minecraft mc){if(mc.player!=null&&mc.level!=null)toggleVeinMiner(mc);}
    public static void toggleColorModeFromGui(){toggleColorMode();}

    private static void toggleXray(Minecraft mc){
        XrayState s=XrayState.getInstance();s.toggle();LocalPlayer p=mc.player;
        hasRenderCenter=false;
        if(s.isActive()){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,-1,0,false,false,false));p.displayClientMessage(Component.translatable("message.xtoxray.xray_on"),true);}
        else{p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);p.displayClientMessage(Component.translatable("message.xtoxray.xray_off"),true);}
        lastX=Long.MIN_VALUE;lastY=Integer.MIN_VALUE;lastZ=Long.MIN_VALUE;rebuildAll(mc);
    }

    private static void toggleColorMode(){
        XrayState state=XrayState.getInstance();
        state.toggleColorMode();
        ColorXrayRenderer.requestFullRescan();
        Minecraft.getInstance().levelRenderer.allChanged();
        Minecraft.getInstance().player.displayClientMessage(
                Component.literal(state.isColorMode() ? "Цветовой X-Ray: ВКЛ" : "Цветовой X-Ray: ВЫКЛ"),
                true);
    }

    private static void toggleVeinMiner(Minecraft mc){XrayState s=XrayState.getInstance();s.setVeinMiner(!s.isVeinMiner());mc.player.displayClientMessage(s.isVeinMiner() ? Component.translatable("message.xtoxray.vein_on") : Component.translatable("message.xtoxray.vein_off"),true);}

    @SubscribeEvent
    public static void hideMobs(RenderLivingEvent.Pre<?, ?> event) {
        if (XrayState.getInstance().isActive() && event.getEntity() instanceof Mob) {
            event.setCanceled(true);
        }
    }

    public static void rebuildAll(Minecraft mc){if(mc.levelRenderer!=null)mc.levelRenderer.allChanged();}

    private static void rebuildChangedSections(Minecraft mc,int oldX,int oldY,int oldZ,int newX,int newY,int newZ,int radius) {
        if(mc.levelRenderer==null || radius<=0)return;
        long r=radius; long radiusSq=r*r;
        int minX=Math.floorDiv(Math.min(oldX,newX)-(radius+16),16);
        int maxX=Math.floorDiv(Math.max(oldX,newX)+(radius+16),16);
        int minY=Math.floorDiv(Math.min(oldY,newY)-(radius+16),16);
        int maxY=Math.floorDiv(Math.max(oldY,newY)+(radius+16),16);
        int minZ=Math.floorDiv(Math.min(oldZ,newZ)-(radius+16),16);
        int maxZ=Math.floorDiv(Math.max(oldZ,newZ)+(radius+16),16);
        for(int sx=minX;sx<=maxX;sx++){
            for(int sy=minY;sy<=maxY;sy++){
                for(int sz=minZ;sz<=maxZ;sz++){
                    int x0=sx*16,y0=sy*16,z0=sz*16,x1=x0+15,y1=y0+15,z1=z0+15;
                    long oldMin=distanceSqToBox(oldX,oldY,oldZ,x0,y0,z0,x1,y1,z1);
                    long oldMax=distanceSqToBoxFarthest(oldX,oldY,oldZ,x0,y0,z0,x1,y1,z1);
                    long newMin=distanceSqToBox(newX,newY,newZ,x0,y0,z0,x1,y1,z1);
                    long newMax=distanceSqToBoxFarthest(newX,newY,newZ,x0,y0,z0,x1,y1,z1);
                    boolean oldInside=oldMax<=radiusSq, oldOutside=oldMin>radiusSq, newInside=newMax<=radiusSq, newOutside=newMin>radiusSq;
                    if((oldInside&&newInside)||(oldOutside&&newOutside))continue;
                    mc.levelRenderer.setSectionDirty(sx,sy,sz);
                }
            }
        }
    }

    private static long distanceSqToBox(int px,int py,int pz,int x0,int y0,int z0,int x1,int y1,int z1) {
        long dx=px< x0 ? (long)x0-px : px>x1 ? (long)px-x1 : 0L;
        long dy=py< y0 ? (long)y0-py : py>y1 ? (long)py-y1 : 0L;
        long dz=pz< z0 ? (long)z0-pz : pz>z1 ? (long)pz-z1 : 0L;
        return dx*dx+dy*dy+dz*dz;
    }

    private static long distanceSqToBoxFarthest(int px,int py,int pz,int x0,int y0,int z0,int x1,int y1,int z1) {
        long dx=Math.max(Math.abs((long)px-x0),Math.abs((long)px-x1));
        long dy=Math.max(Math.abs((long)py-y0),Math.abs((long)py-y1));
        long dz=Math.max(Math.abs((long)pz-z0),Math.abs((long)pz-z1));
        return dx*dx+dy*dy+dz*dz;
    }

    @SubscribeEvent public static void addPauseButton(ScreenEvent.Init.Post e){
        if(!(e.getScreen() instanceof PauseScreen screen))return;
        int screenWidth=Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight=Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int width=200,height=20,x=screenWidth/2-width/2;
        int maxBottom=0;
        for(var child:screen.children())if(child instanceof Button button)maxBottom=Math.max(maxBottom,button.getY()+button.getHeight());
        int y=maxBottom+4;
        if(y+height>screenHeight-4)y=screenHeight-height-4;
        eventButton(e,screen,x,y,width);
    }

    private static void eventButton(ScreenEvent.Init.Post e,PauseScreen screen,int x,int y,int width){
        e.addListener(Button.builder(Component.literal("X to Xray"),b->Minecraft.getInstance().setScreen(new XrayConfigScreen(screen))).bounds(x,y,width,20).build());
    }
}