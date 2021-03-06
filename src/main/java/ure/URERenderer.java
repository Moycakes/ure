package ure;

import ure.actors.UREActor;
import ure.terrain.URETerrain;
import ure.things.UREThing;
import ure.ui.UIModal;

import java.awt.image.BufferedImage;
import java.awt.*;
import java.util.HashMap;
import java.util.Iterator;

public class URERenderer {
    private int fontSize = 14;
    private int outlineWidth = 2;
    private int fontPadX = 3;
    private int fontPadY = 1;
    private int cellPadX = 0;
    private int cellPadY = 1;
    public Font font;
    private boolean smoothGlyphs = true;

    public boolean rendering;

    private HashMap<Character,BufferedImage> glyphCache;
    private HashMap<Character,BufferedImage> outlineCache;
    private Color backgroundColor = Color.BLACK;

    String UIframeTiles = "+-+|+-+|";
    UColor UIframeColor;
    public UColor UItextColor;

    public URERenderer(Font thefont) {
        font = thefont;
        fontSize = font.getSize();
        glyphCache = new HashMap<Character,BufferedImage>();
        outlineCache = new HashMap<Character,BufferedImage>();
        UIframeColor = new UColor(0.7f, 0.7f, 0.7f);
        UItextColor = new UColor(1f, 1f, 1f);
    }

    public int cellWidth() {
        return fontSize+cellPadX;
    }
    public int cellHeight() {
        return fontSize+cellPadY;
    }

    public void renderCamera(URECamera camera) {
        camera.rendering = true;
        rendering = true;
        int cellw = cellWidth();
        int cellh = cellHeight();
        int camw = camera.getWidthInCells();
        int camh = camera.getHeightInCells();
        Graphics g = camera.getGraphics();
        BufferedImage cameraImage = camera.getImage();
        g.setColor(backgroundColor);
        g.fillRect(0,0,camw*cellw, camh*cellh);
        for (int x=0;x<camw;x++) {
            for (int y=0;y<camh;y++) {
                renderCell(camera, x, y, cellw, cellh, g, cameraImage);
            }
        }
        camera.rendering = false;
        rendering = false;
    }

    void renderCell(URECamera camera, int x, int y) {
        renderCell(camera, x, y, cellWidth(), cellHeight(), camera.getGraphics(), camera.getImage());
    }
    void renderCell(URECamera camera, int x, int y, int cellw, int cellh, Graphics g, BufferedImage image) {
        float vis = camera.visibilityAt(x,y);
        float visSeen = camera.getSeenOpacity();
        UColor light = camera.lightAt(x,y);
        URETerrain t = camera.terrainAt(x,y);
        if (t != null) {
            float tOpacity = vis;
            if ((vis < visSeen) && camera.area.seenCell(x + camera.x1, y + camera.y1))
                tOpacity = visSeen;
            UColor terrainLight = light;
            if (t.glow)
                terrainLight.set(1f,1f,1f);
            t.bgColorBuffer.set(t.bgColor.r, t.bgColor.g, t.bgColor.b);
            t.bgColorBuffer.illuminateWith(terrainLight, tOpacity);
            g.setColor(t.bgColorBuffer.makeAWTColor());
            g.fillRect(x*cellw, y*cellh, cellw, cellh);
            BufferedImage tGlyph = charToGlyph(t.glyph(x + camera.x1, y + camera.y1), font);
            t.fgColorBuffer.set(t.fgColor.r, t.fgColor.g, t.fgColor.b);
            t.fgColorBuffer.illuminateWith(terrainLight, tOpacity);
            stampGlyph(tGlyph, image, x*cellw, y*cellh, t.fgColorBuffer, t.glyphOffsetX(), t.glyphOffsetY());
        }
        if (vis < 0.3f)
            return;
        Iterator<UREThing> things = camera.thingsAt(x,y);
        if (things != null) {
            while (things.hasNext()) {
                UREThing thing = things.next();
                char icon = thing.getGlyph();
                UColor color = new UColor(thing.getGlyphColor());
                if (thing.drawGlyphOutline())
                    stampGlyph(charToOutline(icon, font), image, x * cellw, y * cellh, UColor.COLOR_BLACK, 0, 0);
                color.illuminateWith(light, vis);
                stampGlyph(charToGlyph(icon, font), image, x * cellw, y * cellh, color, 0, 0);
            }
        }
        UREActor actor = camera.actorAt(x,y);
        if (actor != null) {
            char icon = actor.getGlyph();
            UColor color = new UColor(actor.getGlyphColor());
            if (actor.drawGlyphOutline())
                stampGlyph(charToOutline(icon, font), image, x*cellw,y*cellh,UColor.COLOR_BLACK, 0, 0);
            color.illuminateWith(light,vis);
            stampGlyph(charToGlyph(icon, font), image, x*cellw,y*cellh,color,0,0);
        }
    }


    public void stampGlyph(BufferedImage srcImage, BufferedImage dstImage, int destx, int desty, UColor tint, int offX, int offY) {
        int width = srcImage.getWidth();
        int height = srcImage.getHeight();
        int[] srcData = new int[width*height];
        int[] dstData = new int[width*height];
        srcData = srcImage.getRGB(0, 0, width, height, srcData, 0, width);
        dstData = dstImage.getRGB(destx, desty, width, height, dstData, 0, width);
        for (int y=0;y<height;y++) {
            int offset = y*width;
            for (int x=0;x<width;x++) {
                if ((destx + x < dstImage.getWidth()) && (desty + y < dstImage.getHeight()) && (destx + x >= 0) && (desty + y >= 0)) {
                    int pixeli = x+offset+offX+offY*width;
                    if (pixeli >=0 && pixeli < srcData.length) {
                        int srcRGB = srcData[x + offset + offX + offY * width];
                        int dstRGB = dstData[x + offset];
                        int r1 = (srcRGB & 0xff0000) >> 16;
                        int g1 = (srcRGB & 0xff00) >> 8;
                        int b1 = (srcRGB & 0xff);
                        float alpha1 = (float) (r1 + g1 + b1) / 765f;
                        r1 = (int) ((float) tint.iR() * alpha1);
                        g1 = (int) ((float) tint.iG() * alpha1);
                        b1 = (int) ((float) tint.iB() * alpha1);
                        int r2 = (dstRGB & 0xff0000) >> 16;
                        int g2 = (dstRGB & 0xff00) >> 8;
                        int b2 = (dstRGB & 0xff);
                        float alpha2 = 1.0f - alpha1;
                        int r3 = r1 + (int) ((float) r2 * alpha2);
                        int g3 = g1 + (int) ((float) g2 * alpha2);
                        int b3 = b1 + (int) ((float) b2 * alpha2);
                        int finalRGB = ((r3 & 0x0ff) << 16) | ((g3 & 0x0ff) << 8) | (b3 & 0x0ff);
                        dstData[x + offset] = finalRGB;
                    }
                }
            }
        }
        dstImage.setRGB(destx, desty, width, height, dstData, 0, width);
    }

    BufferedImage charToGlyph(char c, Font font) {
        if (glyphCache.containsKey((Character)c)) {
            return glyphCache.get((Character)c);
        }
        int cellw = cellWidth();
        int cellh = cellHeight();
        BufferedImage glyph = new BufferedImage(cellw, cellh, BufferedImage.TYPE_INT_RGB);
        Graphics g = glyph.getGraphics();
        if (smoothGlyphs) {
            ((Graphics2D)g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        }
        g.setColor(Color.BLACK);
        g.fillRect(0,0,cellw,cellh);
        g.setFont(font);
        g.setColor(Color.WHITE);
        g.drawString(Character.toString(c), fontPadX, fontSize - fontPadY);
        glyphCache.put((Character)c, glyph);
        System.out.println("cached a glyph for " + Character.toString(c));
        return glyph;
    }

    BufferedImage charToOutline(char c, Font font) {
        if (outlineCache.containsKey((Character)c)) {
            return outlineCache.get((Character)c);
        }
        BufferedImage src = charToGlyph(c, font);
        int cellw = src.getWidth();
        int cellh = src.getHeight();
        BufferedImage outline = new BufferedImage(cellw, cellh, BufferedImage.TYPE_INT_RGB);
        Graphics g = outline.getGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0,0,cellw,cellh);
        for (int y=0;y<cellh;y++) {
            for (int x=0;x<cellw;x++) {
                int pixel = src.getRGB(x,y);
                for (int offy=-outlineWidth;offy<=outlineWidth;offy++) {
                    for (int offx=-outlineWidth;offx<=outlineWidth;offx++) {
                        int tx = x+offx;
                        int ty = y+offy;
                        if (tx >=0 && ty >=0 && tx < cellw && ty < cellh) {
                            int destpixel = outline.getRGB(tx,ty);
                            if ((destpixel & 0xff) < (pixel & 0xff)) {
                                outline.setRGB(tx, ty, pixel);
                            }
                        }
                    }
                }
            }
        }
        outlineCache.put((Character)c, outline);
        System.out.println("cached a glyph outline for " + Character.toString(c));
        return outline;
    }

    public void renderUIFrame(UIModal modal) {
        Graphics g = modal.getGraphics();
        g.setColor(modal.bgColor.makeAWTColor());
        g.fillRect(0,0,modal.pixelWidth,modal.pixelHeight);
        for (int x=0;x<modal.width;x++) {
            for (int y=0;y<modal.height;y++) {
                char c = 0;
                if (x == 0 && y == 0) {
                    c = UIframeTiles.charAt(0);
                } else if (y == 0 && x < modal.width - 1) {
                    c = UIframeTiles.charAt(1);
                } else if (y == 0) {
                    c = UIframeTiles.charAt(2);
                } else if (x == modal.width - 1 && y < modal.height - 1) {
                    c = UIframeTiles.charAt(3);
                } else if (x == modal.width - 1 && y == modal.height - 1) {
                    c = UIframeTiles.charAt(4);
                } else if (x > 0 && x < modal.width - 1 && y == modal.height - 1) {
                    c = UIframeTiles.charAt(5);
                } else if (x == 0 && y == modal.height - 1) {
                    c = UIframeTiles.charAt(6);
                } else if (x == 0 && y < modal.height - 1) {
                    c = UIframeTiles.charAt(7);
                }
                if (c != 0) {
                    stampGlyph(charToGlyph(c, font), modal.getImage(), x*cellWidth(), y*cellHeight(), UIframeColor, 0, 0);
                }
            }
        }
    }
}
