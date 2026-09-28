package com.xtoxray.client.gui;

import com.xtoxray.XrayState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class XrayBlockPickerScreen extends Screen {
    private static final int PANEL_W=568,PANEL_H=438,CELL=26,GAP=3,COLS=15,ROWS=11;
    private final Screen parent;
    private final boolean veinMinerMode;
    private final XrayState state=XrayState.getInstance();
    private final List<Block> all=new ArrayList<>();
    private List<Block> filtered=List.of();
    private EditBox search;
    private int scroll;
    private String status = "";
    private long statusUntil;
    private int originalBlur = -1;

    public XrayBlockPickerScreen(Screen parent, boolean veinMinerMode){
        super(Component.literal(veinMinerMode ? "Добавить блок в Добычу жил" : "Добавить блок в Рентген"));
        this.parent=parent;
        this.veinMinerMode=veinMinerMode;
    }

    @Override protected void init(){
        if (originalBlur < 0) {
            originalBlur = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        }
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(0);

        int panelTop = Math.max(6, (height-PANEL_H)/2);

        all.clear();
        for(Block b:BuiltInRegistries.BLOCK) if(b!=Blocks.AIR) all.add(b);
        all.sort(Comparator.comparing(b->b.getName().getString(),String.CASE_INSENSITIVE_ORDER));
        search=addRenderableWidget(new EditBox(font,width/2-284,panelTop+31,568,20,Component.literal("Поиск")));
        search.setHint(Component.literal("Поиск по названию или ID блока..."));
        search.setMaxLength(128);
        search.setResponder(s->{scroll=0;filter();});
        search.setFocused(true);
        filter();
    }

    private void filter(){
        String q=search==null?"":search.getValue().toLowerCase(Locale.ROOT).trim();
        if(q.isEmpty()){filtered=all;return;}
        List<Block> r=new ArrayList<>();
        for(Block b:all){
            String n=b.getName().getString().toLowerCase(Locale.ROOT);
            String id=String.valueOf(BuiltInRegistries.BLOCK.getKey(b)).toLowerCase(Locale.ROOT);
            if(n.contains(q)||id.contains(q))r.add(b);
        }
        filtered=r;
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // В окне выбора блоков vanilla blur также полностью отключён.
    }

    @Override public void render(GuiGraphics g,int mx,int my,float pt){
        // Никакого blur: фон каталога блоков должен оставаться резким.
        g.fill(0, 0, width, height, 0x66000000);
        int left=(width-PANEL_W)/2,top=Math.max(6,(height-PANEL_H)/2);
        int right=left+PANEL_W,bottom=top+PANEL_H;
        g.fill(left-3,top-3,right+3,bottom+3,0x99000000);
        g.fill(left,top,right,bottom,0xE6080908);
        g.hLine(left,right,top,0xFF444444);g.hLine(left,right,bottom,0xFF444444);
        g.vLine(left,top,bottom,0xFF444444);g.vLine(right,top,bottom,0xFF444444);
        String title = veinMinerMode ? "Добавить блок в Добычу жил" : "Добавить блок в Рентген";
        g.drawString(font,Component.literal(title),left+12,top+12,0xFFFFFFFF,false);
        String resultText = filtered.size()+" найдено";
        g.drawString(font,Component.literal(resultText),right-font.width(resultText)-12,top+12,0xFF999999,false);
        int x=left+12,y=top+57;
        int start=scroll*COLS,end=Math.min(filtered.size(),start+COLS*ROWS);
        for(int i=start;i<end;i++){
            int p=i-start,col=p%COLS,row=p/COLS,bx=x+col*(CELL+GAP),by=y+row*(CELL+GAP);
            Block b=filtered.get(i);boolean h=mx>=bx&&mx<bx+CELL&&my>=by&&my<by+CELL;
            g.fill(bx,by,bx+CELL,by+CELL,h?0xFF3B3B3B:0xFF242424);
            g.hLine(bx,bx+CELL,by,0xFF414141);g.hLine(bx,bx+CELL,by+CELL,0xFF101010);
            g.vLine(bx,by,by+CELL,0xFF414141);g.vLine(bx+CELL,by,by+CELL,0xFF101010);
            ItemStack s=b.asItem().getDefaultInstance();
            if(!s.isEmpty()){
                g.renderItem(s,bx+5,by+5);
                boolean selected = veinMinerMode
                    ? state.isVeinMinerWhitelisted(b)
                    : state.isXrayWhitelisted(b);
                if(selected){
                    g.fill(bx+1,by+1,bx+25,by+25,0x5530A050);
                    g.drawString(font,Component.literal("✓"),bx+16,by+3,0xFF7CFF9A,false);
                }
                if(h)g.renderTooltip(font,s,mx,my);
            }
        }
        if(System.currentTimeMillis() < statusUntil){
            int statusColor = status.startsWith("Добавлен:") ? 0xFF72E69A : 0xFFFFC46B;
            g.drawCenteredString(font,Component.literal(status),width/2,bottom-37,statusColor);
        }
        g.drawCenteredString(font,Component.literal("Escape: назад"),width/2,bottom-22,0xFF777777);
        super.render(g,mx,my,pt);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        int left=(width-PANEL_W)/2,top=Math.max(6,(height-PANEL_H)/2),x=left+12,y=top+57;
        int start=scroll*COLS,end=Math.min(filtered.size(),start+COLS*ROWS);
        for(int i=start;i<end;i++){
            int p=i-start,col=p%COLS,row=p/COLS,bx=x+col*(CELL+GAP),by=y+row*(CELL+GAP);
            if(mx>=bx&&mx<bx+CELL&&my>=by&&my<by+CELL){
                Block selectedBlock = filtered.get(i);
                boolean alreadySelected = veinMinerMode
                    ? state.isVeinMinerWhitelisted(selectedBlock)
                    : state.isXrayWhitelisted(selectedBlock);

                if (alreadySelected) {
                    status = "Уже добавлен: " + selectedBlock.getName().getString();
                } else {
                    if (veinMinerMode) {
                        state.addVeinMinerBlock(selectedBlock);
                    } else {
                        state.addXrayBlock(selectedBlock);
                    }
                    status = "Добавлен: " + selectedBlock.getName().getString();
                }
                statusUntil = System.currentTimeMillis() + 2500L;
                return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }

    @Override public boolean mouseScrolled(double mx,double my,double scrollX,double scrollY){
        int rows=(filtered.size()+COLS-1)/COLS,max=Math.max(0,rows-ROWS);
        if(scrollY != 0){
            scroll=Math.max(0,Math.min(scroll-(int)Math.signum(scrollY),max));
        }
        return true;
    }

    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){onClose();return true;}
        return super.keyPressed(keyCode,scanCode,modifiers);
    }

    @Override public void removed(){
        if (originalBlur >= 0) {
            Minecraft.getInstance().options.menuBackgroundBlurriness().set(originalBlur);
            originalBlur = -1;
        }
        super.removed();
    }

    @Override public void onClose(){
        Minecraft.getInstance().setScreen(parent);
    }
}