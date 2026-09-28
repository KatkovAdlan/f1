package com.xtoxray.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.xtoxray.XrayState;
import com.xtoxray.XtoXray;
import com.xtoxray.client.XrayClient;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import java.util.List;

public final class XrayConfigScreen extends Screen {
    private static final int PANEL_W = 568, PANEL_H = 438, SIDEBAR_W = 114, HEADER_H = 37;
    private enum Page { XRAY, VEIN, KEYBINDS }
    private final Screen parent;
    private final XrayState state = XrayState.getInstance();
    private Page page = Page.XRAY;
    private int left, top, panelW, panelH;
    private int listening = -1;
    private EditBox whitelistSearch;
    private int originalBlur = -1;

    public XrayConfigScreen(Screen parent) {
        super(Component.literal("XtoXray"));
        this.parent = parent;
    }

    @Override protected void init() {
        if (originalBlur < 0) {
            originalBlur = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        }
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(0);

        panelW = Math.min(PANEL_W, width - 12);
        panelH = Math.min(PANEL_H, height - 12);
        left = (width - panelW) / 2;
        top = Math.max(6, (height - panelH) / 2);
        clearWidgets();

        int x = contentLeft(), w = Math.min(258, contentWidth());
        if (page == Page.XRAY) {
            addRenderableWidget(new DistanceSlider(x, top + 49, w, 25, state.getOreRenderDistance()));
            addWhitelistSearch(x, top + 82, w);
        }
        if (page == Page.VEIN) {
            addRenderableWidget(new DurabilitySlider(x, top + 87, w, 25, state.getVeinMinerDurabilityPerBlock()));
            addWhitelistSearch(x, top + 119, w);
        }
        if (page == Page.KEYBINDS) addKeybindWidgets();
    }

    private int contentLeft() { return left + SIDEBAR_W + 15; }
    private int contentWidth() { return panelW - SIDEBAR_W - 30; }

    private void addKeybindWidgets() {
        int x = contentLeft(), w = Math.min(258, contentWidth()), y = top + HEADER_H + 12;
        addRenderableWidget(keyButton(x, y, w, "Рентген", XrayClient.TOGGLE_KEY.get(), 0));
        addRenderableWidget(keyButton(x, y + 34, w, "Добыча жил", XrayClient.VEIN_MINER_KEY.get(), 1));
        addRenderableWidget(keyButton(x, y + 68, w, "Открыть меню", XrayClient.OPEN_MENU_KEY.get(), 2));
    }

    private Button keyButton(int x, int y, int w, String label, KeyMapping mapping, int index) {
        Component text;
        if (mapping == null) {
            text = Component.literal(label + ": [Не реализовано]");
        } else if (listening == index) {
            text = Component.literal(label + ": [Нажмите клавишу]");
        } else {
            text = Component.literal(label + ": [" + mapping.getTranslatedKeyMessage().getString() + "]");
        }
        Button b = Button.builder(text, btn -> {
            if (mapping != null) { listening = index; init(); }
        }).bounds(x, y, w, 25).build();
        if (mapping == null) b.active = false;
        return b;
    }

    @Override public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Vanilla Screen.renderBackground запускает GameRenderer.renderBlur().
        // Здесь фон рисуется самим экраном, поэтому blur полностью отсутствует.
    }

    @Override public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Намеренно не вызываем renderBackground/renderBlurredBackground:
        // фон меню XtoXray должен оставаться резким.
        g.fill(0, 0, width, height, 0x66000000);
        panelW = Math.min(PANEL_W, width - 12); panelH = Math.min(PANEL_H, height - 12);
        left = (width - panelW) / 2; top = Math.max(6, (height - panelH) / 2);
        drawFrame(g); drawHeader(g); drawSidebar(g, mouseX, mouseY);
        switch (page) {
            case XRAY -> drawXray(g, mouseX, mouseY);
            case VEIN -> drawVein(g, mouseX, mouseY);
            case KEYBINDS -> drawKeybindsHint(g);
        }
        String footer = "© Random Pixel Studios";
        g.drawString(font, Component.literal(footer), left + panelW - font.width(footer) - 12, top + panelH - 15, 0xFF858585, false);
        super.render(g, mouseX, mouseY, partialTick);
    }

    private void drawFrame(GuiGraphics g) {
        int r=left+panelW,b=top+panelH;
        g.fill(left-3,top-3,r+3,b+3,0x99000000);
        g.fill(left,top,r,b,0xE6080908);
        g.hLine(left,r,top,0xFF444444); g.hLine(left,r,b,0xFF444444);
        g.vLine(left,top,b,0xFF444444); g.vLine(r,top,b,0xFF444444);
        g.hLine(left,r,top+HEADER_H,0xFF3B3B3B); g.vLine(left+SIDEBAR_W,top,b,0xFF454545);
    }

    private void drawHeader(GuiGraphics g) {
        g.drawString(font, Component.literal("XtoXray"), left+12, top+12, 0xFFFFFFFF, false);
        String version="Порт NeoForge "+XtoXray.getModVersion();
        g.drawString(font, Component.literal(version), left+panelW-font.width(version)-12, top+12, 0xFF4EE0B3, false);
    }

    private void drawSidebar(GuiGraphics g,int mx,int my) {
        String[] labels={"Рентген","Добыча жил","Клавиши"};
        for(int i=0;i<labels.length;i++){
            int y=top+HEADER_H+i*29;
            boolean sel=page.ordinal()==i, hov=mx>=left+2&&mx<left+SIDEBAR_W-2&&my>=y&&my<y+29;
            if(sel||hov) g.fill(left+2,y+1,left+SIDEBAR_W-2,y+28,sel?0xFF333333:0xFF262626);
            g.drawString(font,Component.literal(labels[i]),left+12,y+10,0xFFE6E6E6,false);
        }
    }

    private void addWhitelistSearch(int x, int y, int w) {
        whitelistSearch = addRenderableWidget(new EditBox(font, x, y, w, 20, Component.literal("Поиск")));
        whitelistSearch.setHint(Component.literal("Поиск по выбранным блокам..."));
        whitelistSearch.setMaxLength(128);
        whitelistSearch.setValue("");
        whitelistSearch.setResponder(value -> {});
    }

    private List<Block> getWhitelistBlocks() {
        return page == Page.VEIN
            ? state.getVeinMinerWhitelistSorted()
            : state.getXrayWhitelistSorted();
    }

    private int getWhitelistSize() {
        return page == Page.VEIN
            ? state.getVeinMinerWhitelistSize()
            : state.getXrayWhitelistSize();
    }

    private List<Block> getFilteredWhitelist() {
        List<Block> blocks = getWhitelistBlocks();
        if (whitelistSearch == null) return blocks;

        String query = whitelistSearch.getValue().toLowerCase(java.util.Locale.ROOT).trim();
        if (query.isEmpty()) return blocks;

        List<Block> result = new java.util.ArrayList<>();
        for (Block block : blocks) {
            String name = block.getName().getString().toLowerCase(java.util.Locale.ROOT);
            String id = String.valueOf(net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block))
                .toLowerCase(java.util.Locale.ROOT);
            if (name.contains(query) || id.contains(query)) result.add(block);
        }
        return result;
    }

    private void drawXray(GuiGraphics g,int mx,int my) {
        int x=contentLeft(), y=top+HEADER_H+112;
        g.drawString(font,Component.literal("Белый список"),x,y+1,0xFFFFFFFF,false);
        String count = getFilteredWhitelist().size() + " из " + getWhitelistSize();
        g.drawString(font,Component.literal(count),x+contentWidth()-font.width(count),y+1,0xFF888888,false);
        drawWhitelist(g,x,y+17,mx,my);
    }

    private void drawVein(GuiGraphics g,int mx,int my) {
        int x=contentLeft(), w=Math.min(258,contentWidth());
        drawToggle(g,x,top+49,w,state.isVeinMiner(),"Добыча жил",0xFF4A1F1E);
        g.drawString(font,Component.literal("Белый список"),x,top+147,0xFFFFFFFF,false);
        String count = getFilteredWhitelist().size() + " из " + getWhitelistSize();
        g.drawString(font,Component.literal(count),x+contentWidth()-font.width(count),top+148,0xFF888888,false);
        drawWhitelist(g,x,top+164,mx,my);
    }

    private void drawWhitelist(GuiGraphics g,int x,int y,int mx,int my) {
        List<Block> blocks=getFilteredWhitelist();
        int cell=26,gap=3,cols=Math.max(1,Math.min(15,contentWidth()/29)),max=cols*6-1;
        int shown=Math.min(blocks.size(),max);
        for(int i=0;i<shown;i++) drawBlockCell(g,blocks.get(i),x+(i%cols)*(cell+gap),y+(i/cols)*(cell+gap),mx,my);
        int ai=shown, ax=x+(ai%cols)*(cell+gap), ay=y+(ai/cols)*(cell+gap);
        g.fill(ax,ay,ax+cell,ay+cell,0xFF18381E);
        g.hLine(ax,ax+cell,ay,0xFF2D5B34); g.hLine(ax,ax+cell,ay+cell,0xFF102312);
        g.vLine(ax,ay,ay+cell,0xFF2D5B34); g.vLine(ax+cell,ay,ay+cell,0xFF102312);
        g.drawCenteredString(font,Component.literal("+"),ax+cell/2,ay+6,0xFFD7FFDD);
        if(mx>=ax&&mx<ax+cell&&my>=ay&&my<ay+cell) g.renderTooltip(font,Component.literal("Добавить блок"),mx,my);
    }

    private void drawBlockCell(GuiGraphics g,Block block,int x,int y,int mx,int my) {
        boolean h=mx>=x&&mx<x+26&&my>=y&&my<y+26;
        g.fill(x,y,x+26,y+26,h?0xFF3B3B3B:0xFF242424);
        g.hLine(x,x+26,y,0xFF414141); g.hLine(x,x+26,y+26,0xFF101010);
        g.vLine(x,y,y+26,0xFF414141); g.vLine(x+26,y,y+26,0xFF101010);
        ItemStack s=block.asItem().getDefaultInstance();
        if(!s.isEmpty()){ g.renderItem(s,x+5,y+5); if(h) g.renderTooltip(font,s,mx,my); }
        if(h) g.drawString(font,"×",x+18,y+1,0xFFFFFFFF,false);
    }

    private void drawToggle(GuiGraphics g,int x,int y,int w,boolean on,String label,int offColor) {
        int c=on?0xFF1D5A38:offColor;
        g.fill(x,y,x+w,y+25,c);
        g.hLine(x,x+w,y,on?0xFF4A9F72:0xFF9C302B); g.hLine(x,x+w,y+25,0xFF5A1513);
        g.vLine(x,y,y+25,on?0xFF4A9F72:0xFF9C302B); g.vLine(x+w,y,y+25,0xFF5A1513);
        g.drawCenteredString(font,Component.literal(label+": "+(on?"ВКЛ":"ВЫКЛ")),x+w/2,y+7,0xFFFFFFFF);
    }

    private void drawKeybindsHint(GuiGraphics g){
        int x=contentLeft(), y=top+HEADER_H+179;
        g.drawString(font,Component.literal("Настройки сохраняются автоматически."),x,y,0xFF999999,false);
        g.drawString(font,Component.literal("Назначение: Escape отменяет ввод, Backspace снимает клавишу."),x,y+14,0xFF777777,false);
    }

    private boolean clickNav(double mx,double my){
        if(mx<left+2||mx>=left+SIDEBAR_W-2) return false;
        int i=(int)((my-(top+HEADER_H))/29);
        if(i<0||i>=Page.values().length) return false;
        Page p=Page.values()[i];
        if(p!=page){page=p;listening=-1;init();}
        return true;
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(clickNav(mx,my)) return true;
        if((page==Page.XRAY||page==Page.VEIN)){
            int x=contentLeft(), y=page==Page.XRAY?top+HEADER_H+129:top+164;
            int cell=26,gap=3,cols=Math.max(1,Math.min(15,contentWidth()/29)),max=cols*6-1;
            List<Block> blocks=getFilteredWhitelist();
            for(int i=0;i<Math.min(blocks.size(),max);i++){
                int bx=x+(i%cols)*(cell+gap), by=y+(i/cols)*(cell+gap);
                if(mx>=bx&&mx<bx+cell&&my>=by&&my<by+cell){
                    if (page == Page.VEIN) {
                        state.removeVeinMinerBlock(blocks.get(i));
                    } else {
                        state.removeXrayBlock(blocks.get(i));
                    }
                    return true;
                }
            }
            int ai=Math.min(blocks.size(),max), ax=x+(ai%cols)*(cell+gap), ay=y+(ai/cols)*(cell+gap);
            if(mx>=ax&&mx<ax+cell&&my>=ay&&my<ay+cell){
                Minecraft.getInstance().setScreen(new XrayBlockPickerScreen(this, page == Page.VEIN));
                return true;
            }
            if(page==Page.VEIN){
                int ty=top+49,w=Math.min(258,contentWidth());
                if(mx>=x&&mx<x+w&&my>=ty&&my<ty+25){XrayClient.toggleVeinMinerFromGui(Minecraft.getInstance());return true;}
            }
        }
        return super.mouseClicked(mx,my,button);
    }

    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){
        if(page==Page.KEYBINDS&&listening>=0){
            if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE){listening=-1;init();return true;}
            int i=listening;
            if(keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) setKey(i,InputConstants.UNKNOWN);
            else setKey(i,InputConstants.getKey(keyCode,scanCode));
            return true;
        }
        if(super.keyPressed(keyCode,scanCode,modifiers)) return true;
        return keyCode==org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
    }

    private void setKey(int i,InputConstants.Key key){
        KeyMapping m=i==0?XrayClient.TOGGLE_KEY.get():i==1?XrayClient.VEIN_MINER_KEY.get():XrayClient.OPEN_MENU_KEY.get();
        m.setKey(key); KeyMapping.resetMapping(); Minecraft.getInstance().options.save(); listening=-1; init();
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

    private static final class DistanceSlider extends AbstractSliderButton {
        DistanceSlider(int x,int y,int w,int h,int d){super(x,y,w,h,Component.empty(),(d-32)/480.0D);updateMessage();}
        protected void updateMessage(){int d=32+(int)Math.round(value*480.0D);setMessage(Component.literal("Дальность: "+d+" блоков"));}
        protected void applyValue(){int d=32+(int)Math.round(value*480.0D);XrayState.getInstance().setOreRenderDistance(d);XrayClient.rebuildAll(Minecraft.getInstance());}
    }
    private static final class DurabilitySlider extends AbstractSliderButton {
        DurabilitySlider(int x,int y,int w,int h,int v){super(x,y,w,h,Component.empty(),(v-1)/9.0D);updateMessage();}
        protected void updateMessage(){int v=1+(int)Math.round(value*9.0D);setMessage(Component.literal("Прочность на блок: "+v));}
        protected void applyValue(){XrayState.getInstance().setVeinMinerDurabilityPerBlock(1+(int)Math.round(value*9.0D));}
    }
}