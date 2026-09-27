package com.xtoxray.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import com.xtoxray.client.gui.XrayConfigScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.Lazy;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(modid=XtoXray.MOD_ID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class XrayClient {
    public static final Lazy<KeyMapping> TOGGLE_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.toggle",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_X,"key.categories.xtoxray"));
    public static final Lazy<KeyMapping> VEIN_MINER_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.vein_miner",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_V,"key.categories.xtoxray"));
    public static final Lazy<KeyMapping> OPEN_MENU_KEY=Lazy.of(()->new KeyMapping("key.xtoxray.open_config",InputConstants.Type.KEYSYM,GLFW.GLFW_KEY_RIGHT_SHIFT,"key.categories.xtoxray"));
    private static long lastX=Long.MIN_VALUE,lastZ=Long.MIN_VALUE; private static int lastY=Integer.MIN_VALUE;
    private XrayClient(){}
    @SubscribeEvent public static void onClientTick(ClientTickEvent.Post e){
        Minecraft mc=Minecraft.getInstance();
        while(OPEN_MENU_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)mc.setScreen(new XrayConfigScreen(null));
        while(TOGGLE_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)toggleXray(mc);
        while(VEIN_MINER_KEY.get().consumeClick())if(mc.level!=null&&mc.player!=null&&mc.screen==null)toggleVeinMiner(mc);
        if(mc.level!=null&&mc.player!=null){
            XrayState.getInstance().updateRenderCenter(mc.player.blockPosition());
        }

        if(XrayState.getInstance().isActive()&&mc.level!=null&&mc.player!=null){
            long x=mc.player.blockPosition().getX()>>4;int y=mc.player.blockPosition().getY()>>4;long z=mc.player.blockPosition().getZ()>>4;
            if(x!=lastX||y!=lastY||z!=lastZ){lastX=x;lastY=y;lastZ=z;rebuildAll(mc);}
        }
    }
    public static void toggleXrayFromGui(Minecraft mc){if(mc.player!=null&&mc.level!=null)toggleXray(mc);}
    public static void toggleVeinMinerFromGui(Minecraft mc){if(mc.player!=null&&mc.level!=null)toggleVeinMiner(mc);}
    private static void toggleXray(Minecraft mc){
        XrayState s=XrayState.getInstance();s.toggle();LocalPlayer p=mc.player;
        if(s.isActive()){p.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION,-1,0,false,false,false));p.displayClientMessage(Component.translatable("message.xtoxray.xray_on"),true);}
        else{p.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);p.displayClientMessage(Component.translatable("message.xtoxray.xray_off"),true);}
        lastX=Long.MIN_VALUE;lastY=Integer.MIN_VALUE;lastZ=Long.MIN_VALUE;rebuildAll(mc);
    }
    private static void toggleVeinMiner(Minecraft mc){XrayState s=XrayState.getInstance();s.setVeinMiner(!s.isVeinMiner());mc.player.displayClientMessage(s.isVeinMiner() ? Component.translatable("message.xtoxray.vein_on") : Component.translatable("message.xtoxray.vein_off"),true);}
    public static void rebuildAll(Minecraft mc){if(mc.levelRenderer!=null)mc.levelRenderer.allChanged();}
    @SubscribeEvent public static void addPauseButton(ScreenEvent.Init.Post e){
        if(!(e.getScreen() instanceof PauseScreen screen))return;

        int screenWidth=Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight=Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int width=200;
        int height=20;
        int x=screenWidth/2-width/2;

        // Не привязываемся к конкретной кнопке. Берём самую нижнюю существующую кнопку.
        // Поэтому сторонние моды, добавляющие новые кнопки ниже, автоматически сдвигают X to Xray.
        int maxBottom=0;
        for(var child:screen.children()){
            if(child instanceof Button button){
                maxBottom=Math.max(maxBottom,button.getY()+button.getHeight());
            }
        }

        int y=maxBottom+4;

        // На обычном экране места достаточно. Если меню необычно переполнено,
        // не допускаем выход нашей кнопки за нижнюю границу.
        if(y+height>screenHeight-4){
            y=screenHeight-height-4;
        }

        eventButton(e,screen,x,y,width);
    }

    private static void eventButton(ScreenEvent.Init.Post e,PauseScreen screen,int x,int y,int width){
        e.addListener(Button.builder(
            Component.literal("X to Xray"),
            b->Minecraft.getInstance().setScreen(new XrayConfigScreen(screen))
        ).bounds(x,y,width,20).build());
    }
}