package dev.flagon.sandbox;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * the java.awt and javax.imageio behavior a page must match. the jvm test checks these lines
 * are what the jdk says, and the browser test checks a bundle says the same. pixels here are
 * ones both draw exactly: no antialiased edges, no fonts.
 */
public final class GraphicsChecks {

    /** 4x2 pixels: red, green, blue, white, black, grey, 0x123456, 0xfedcba; the jdk wrote them */
    private static final String BMP = "Qk1OAAAAAAAAADYAAAAoAAAABAAAAAIAAAABABgAAAAAABgAAAAAAAAAA"
            + "AAAAAAAAAAAAAAAAAAAgICAVjQSutz+AAD/AP8A/wAA////";
    /** the same pixels with the last one transparent */
    private static final String GIF = "R0lGODlhBAACAIIAAP8AAAD/AAAA/////wAAAICAgBI0Vv7cuiH5BAE"
            + "AAAcALAAAAAAEAAIAAAgMAAEEEDCAQAEDBwICADs=";
    /** the same pixels, interlaced */
    private static final String PNG = "iVBORw0KGgoAAAANSUhEUgAAAAQAAAACCAIAAAGHzdqiAAAAHElEQVR4"
            + "XmP4zwAC/0Ho/38wp6GhQcgk7N+dXQCC4QqrKBNqAwAAAABJRU5ErkJggg==";
    /** 8x8 of 0x3366cc */
    private static final String JPEG = "/9j/4AAQSkZJRgABAgAAAQABAAD/2wBDAAgGBgcGBQgHBwcJCQgKDBQ"
            + "NDAsLDBkSEw8UHRofHh0aHBwgJC4nICIsIxwcKDcpLDAxNDQ0Hyc5PTgyPC4zNDL/2wBDAQkJCQwLDBgND"
            + "RgyIRwhMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjIyMjL/wAARCAA"
            + "IAAgDASIAAhEBAxEB/8QAHwAAAQUBAQEBAQEAAAAAAAAAAAECAwQFBgcICQoL/8QAtRAAAgEDAwIEAwUFB"
            + "AQAAAF9AQIDAAQRBRIhMUEGE1FhByJxFDKBkaEII0KxwRVS0fAkM2JyggkKFhcYGRolJicoKSo0NTY3ODk"
            + "6Q0RFRkdISUpTVFVWV1hZWmNkZWZnaGlqc3R1dnd4eXqDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKzt"
            + "LW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uHi4+Tl5ufo6erx8vP09fb3+Pn6/8QAHwEAAwEBAQEBAQEBAQA"
            + "AAAAAAAECAwQFBgcICQoL/8QAtREAAgECBAQDBAcFBAQAAQJ3AAECAxEEBSExBhJBUQdhcRMiMoEIFEKRo"
            + "bHBCSMzUvAVYnLRChYkNOEl8RcYGRomJygpKjU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ"
            + "3eHl6goOEhYaHiImKkpOUlZaXmJmaoqOkpaanqKmqsrO0tba3uLm6wsPExcbHyMnK0tPU1dbX2Nna4uPk5"
            + "ebn6Onq8vP09fb3+Pn6/9oADAMBAAIRAxEAPwDBooor7U8E/9k=";

    private interface Check {
        void run(List<String> out) throws Exception;
    }

    private GraphicsChecks() {
    }

    public static List<String> run() {
        List<String> out = new ArrayList<>();
        check(out, "transform", GraphicsChecks::transforms);
        check(out, "type", GraphicsChecks::types);
        check(out, "rectangle", GraphicsChecks::rectangles);
        check(out, "line", GraphicsChecks::lines);
        check(out, "ellipse", GraphicsChecks::ellipses);
        check(out, "round", GraphicsChecks::roundRectangles);
        check(out, "path", GraphicsChecks::paths);
        check(out, "polygon", GraphicsChecks::polygons);
        check(out, "color", GraphicsChecks::colors);
        check(out, "stroke", GraphicsChecks::strokes);
        check(out, "font", GraphicsChecks::fonts);
        check(out, "state", GraphicsChecks::state);
        check(out, "pixels", GraphicsChecks::pixels);
        check(out, "canvas", GraphicsChecks::canvas);
        check(out, "imageio", GraphicsChecks::imageio);
        return out;
    }

    private static void check(List<String> out, String name, Check check) {
        List<String> lines = new ArrayList<>();
        try {
            check.run(lines);
        } catch (Exception failed) {
            lines.add("failed with " + failed);
        }
        for (String line : lines) {
            out.add(name + ": " + line);
        }
    }

    private static void transforms(List<String> out) throws Exception {
        AffineTransform moved = AffineTransform.getTranslateInstance(3, 4);
        moved.scale(2, 2);
        moved.rotate(Math.PI / 2);
        out.add("translate scale rotate " + moved);
        out.add("about a point " + AffineTransform.getRotateInstance(0.7, 3, 4));
        AffineTransform sheared = AffineTransform.getShearInstance(0.5, 0.25);
        sheared.translate(2, 3);
        sheared.rotate(0.5);
        out.add("shear translate rotate " + sheared);
        out.add("inverse " + AffineTransform.getScaleInstance(3, 7).createInverse());
        out.add("inverse of a turn " + AffineTransform.getRotateInstance(0.3, 1, 2)
                .createInverse());
        try {
            new AffineTransform(1, 2, 2, 4, 0, 0).createInverse();
            out.add("a flat transform inverted");
        } catch (NoninvertibleTransformException flat) {
            out.add("flat " + flat.getMessage());
        }
        AffineTransform matrix = new AffineTransform(1, 2, 3, 4, 5, 6);
        out.add("hash " + matrix.hashCode() + " determinant " + matrix.getDeterminant());
        out.add("point " + matrix.transform(new Point2D.Double(1, 2), null) + " "
                + matrix.transform(new Point(1, 2), null) + " delta "
                + matrix.deltaTransform(new Point2D.Double(1, 2), null));
        out.add("back " + matrix.inverseTransform(new Point2D.Double(12, 16), null));
        AffineTransform quarter = new AffineTransform();
        quarter.quadrantRotate(1);
        AffineTransform half = new AffineTransform();
        half.rotate(Math.PI);
        out.add("quarter " + quarter + " half " + half);
        out.add("no turn " + AffineTransform.getRotateInstance(0) + " right angle "
                + AffineTransform.getRotateInstance(Math.PI / 2));
        out.add("by vector " + AffineTransform.getRotateInstance(1, 1) + " "
                + AffineTransform.getRotateInstance(-2, 0));
        AffineTransform joined = new AffineTransform(2, 0.5, -1, 3, 4, 5);
        joined.concatenate(new AffineTransform(1, 2, 3, 4, 5, 6));
        AffineTransform before = new AffineTransform(2, 0.5, -1, 3, 4, 5);
        before.preConcatenate(new AffineTransform(1, 2, 3, 4, 5, 6));
        out.add("concatenate " + joined + " pre " + before);
        double[] points = {0, 0, 1, 0, 0, 1};
        matrix.transform(points, 0, points, 2, 2);
        out.add("in place " + java.util.Arrays.toString(points));
        Shape turned = AffineTransform.getRotateInstance(Math.PI / 2)
                .createTransformedShape(new Rectangle(1, 2, 3, 4));
        out.add("turned " + turned.getBounds2D() + " " + turned.getBounds());
        out.add("identity " + new AffineTransform().isIdentity() + " " + matrix.isIdentity()
                + " equal " + matrix.equals(new AffineTransform(1, 2, 3, 4, 5, 6)));
    }

    private static void types(List<String> out) {
        double[][] matrices = {
            {1, 0, 0, 1, 0, 0}, {1, 0, 0, 1, 0, 7}, {2, 0, 0, 2, 0, 0}, {2, 0, 0, 3, 0, 0},
            {-1, 0, 0, -1, 0, 0}, {-2, 0, 0, -2, 0, 0}, {-2, 0, 0, -3, 0, 0}, {-1, 0, 0, 1, 0, 0},
            {2, 0, 0, -2, 0, 0}, {0, 1, -1, 0, 0, 0}, {0, 1, 1, 0, 0, 0}, {0, -1, -1, 0, 0, 0},
            {0, 2, -3, 0, 0, 0}, {0, 2, 3, 0, 0, 0}, {0, 1, 0, 0, 3, 0}, {0, 0, 0, 0, 0, 0},
            {1, 0, 0, 0, 0, 0}, {1, 1, 0, 1, 5, 0}, {1, 0.5, 0.5, 1, 0, 0},
            {0.6, 0.8, 0.8, -0.6, 0, 0}, {1.2, 1.6, -0.8, 0.6, 0, 0},
            {Math.cos(1), Math.sin(1), -Math.sin(1), Math.cos(1), 0, 0},
            {2 * Math.cos(1), 2 * Math.sin(1), -2 * Math.sin(1), 2 * Math.cos(1), 1, 0},
        };
        StringBuilder types = new StringBuilder();
        for (double[] matrix : matrices) {
            types.append(new AffineTransform(matrix).getType()).append(' ');
        }
        out.add(types.toString().trim());
    }

    private static void rectangles(List<String> out) {
        Rectangle box = new Rectangle(1, 2, 10, 20);
        Rectangle other = new Rectangle(5, -3, 10, 10);
        out.add(box + " " + box.intersection(other) + " " + box.union(other));
        out.add("contains " + box.contains(1, 2) + box.contains(11, 2) + box.contains(10, 21)
                + box.contains(new Rectangle(2, 3, 9, 19)) + box.contains(2, 3, 10, 19)
                + box.contains(2, 3, 0, 1) + " intersects " + box.intersects(other)
                + box.intersects(new Rectangle(11, 2, 3, 3)));
        Rectangle grown = new Rectangle(box);
        grown.add(-4, 30);
        grown.grow(2, 1);
        out.add("grown " + grown + " empty " + new Rectangle(3, 3, 0, 5).isEmpty());
        Rectangle2D fraction = new Rectangle2D.Double(0.5, 1.25, 2.5, 3.5);
        out.add("fraction " + fraction.getBounds() + " " + fraction.createUnion(box) + " "
                + fraction.createIntersection(new Rectangle2D.Float(1, 2, 4, 4)));
        out.add("outcode " + box.outcode(0, 0) + " " + box.outcode(5, 5) + " "
                + box.outcode(20, 30) + " " + fraction.outcode(3, 1));
        out.add("line " + fraction.intersectsLine(0, 0, 5, 5) + " "
                + fraction.intersectsLine(0, 0, 0.4, 10) + " " + box.intersectsLine(0, 25, 12, 25));
        Rectangle set = new Rectangle();
        set.setRect(0.5, 0.5, 2.2, 2.9);
        out.add("set " + set + " equal " + new Rectangle(1, 2, 3, 4).equals(
                new Rectangle2D.Double(1, 2, 3, 4)) + " hash "
                + new Rectangle2D.Double(1, 2, 3, 4).hashCode() + " "
                + new Rectangle(1, 2, 3, 4).hashCode());
    }

    private static void lines(List<String> out) {
        Line2D line = new Line2D.Double(0, 0, 10, 5);
        out.add("ccw " + line.relativeCCW(5, 0) + line.relativeCCW(5, 5) + line.relativeCCW(20, 10)
                + line.relativeCCW(-2, -1) + line.relativeCCW(4, 2));
        out.add("distance " + line.ptSegDist(12, 6) + " " + line.ptLineDist(0, 5) + " "
                + line.ptSegDistSq(5, 0));
        out.add("crosses " + line.intersectsLine(0, 5, 10, 0) + " "
                + Line2D.linesIntersect(0, 0, 1, 1, 2, 2, 3, 3) + " "
                + line.intersects(9, 4, 2, 2) + " bounds "
                + new Line2D.Float(5, 1, 2, 7).getBounds2D());
    }

    private static void ellipses(List<String> out) {
        Ellipse2D oval = new Ellipse2D.Double(1, 2, 8, 4);
        out.add(segments(oval.getPathIterator(null)));
        out.add("contains " + oval.contains(5, 4) + oval.contains(1.2, 2.2) + oval.contains(8.9, 4)
                + " intersects " + oval.intersects(0, 0, 1.5, 2.5) + oval.intersects(8, 3, 4, 1)
                + " bounds " + oval.getBounds());
    }

    private static void roundRectangles(List<String> out) {
        RoundRectangle2D round = new RoundRectangle2D.Double(0, 0, 10, 6, 4, 2);
        out.add(segments(round.getPathIterator(null)));
        out.add("contains " + round.contains(0.1, 0.1) + round.contains(5, 3)
                + round.contains(2, 0.5) + " intersects " + round.intersects(-1, -1, 1.2, 1.2)
                + round.intersects(9, 5, 3, 3));
    }

    private static void paths(List<String> out) {
        Path2D path = new Path2D.Double();
        path.moveTo(0, 0);
        path.lineTo(10, 0);
        path.lineTo(10, 10);
        path.lineTo(0, 10);
        path.closePath();
        path.moveTo(2, 2);
        path.lineTo(8, 2);
        path.lineTo(8, 8);
        path.lineTo(2, 8);
        path.closePath();
        out.add("nonzero " + path.contains(5, 5) + path.contains(1, 1) + path.contains(11, 5));
        path.setWindingRule(Path2D.WIND_EVEN_ODD);
        out.add("evenodd " + path.contains(5, 5) + path.contains(1, 1) + " bounds "
                + path.getBounds2D() + " rect " + path.contains(0.5, 0.5, 1, 1)
                + path.intersects(4, 4, 1, 1) + path.intersects(-5, -5, 6, 6));
        Path2D curve = new Path2D.Float();
        curve.moveTo(0, 0);
        curve.quadTo(5, 10, 10, 0);
        curve.curveTo(10, -4, 0, -4, 0, 0);
        out.add("curve " + segments(curve.getPathIterator(null)) + " current "
                + curve.getCurrentPoint());
        Rectangle2D bounds = curve.getBounds2D();
        out.add("curve bounds " + Math.round(bounds.getMinX() * 1000) + " "
                + Math.round(bounds.getMinY() * 1000) + " " + Math.round(bounds.getMaxX() * 1000)
                + " " + Math.round(bounds.getMaxY() * 1000));
        Path2D joined = new Path2D.Double(new Rectangle(0, 0, 2, 2));
        joined.append(new Line2D.Double(2, 2, 5, 5), true);
        joined.append(new Line2D.Double(7, 7, 9, 9), true);
        out.add("append " + segments(joined.getPathIterator(null)));
        try {
            new Path2D.Double().lineTo(1, 1);
            out.add("a path drew from nowhere");
        } catch (IllegalPathStateException nowhere) {
            out.add("nowhere " + nowhere.getMessage());
        }
        Path2D moved = new Path2D.Double(new Rectangle(1, 1, 2, 2));
        moved.transform(AffineTransform.getScaleInstance(2, 3));
        out.add("scaled " + moved.getBounds2D() + " "
                + segments(new Rectangle(1, 1, 2, 2).getPathIterator(
                        AffineTransform.getTranslateInstance(1, 0))));
    }

    private static void polygons(List<String> out) {
        Polygon star = new Polygon(new int[] {0, 4, 8, 0, 8}, new int[] {0, 8, 0, 5, 5}, 5);
        star.addPoint(1, 1);
        out.add("bounds " + star.getBounds() + " contains " + star.contains(4, 4)
                + star.contains(4, 6) + star.contains(1, 4) + star.contains(20, 20));
        out.add(segments(new Polygon(new int[] {1, 2, 3}, new int[] {4, 5, 7}, 3)
                .getPathIterator(null)));
    }

    private static void colors(List<String> out) {
        Color color = new Color(200, 100, 50, 128);
        out.add(color + " " + Integer.toHexString(color.getRGB()) + " brighter "
                + color.brighter() + " darker " + color.darker() + " black brighter "
                + Color.BLACK.brighter() + " " + new Color(1, 2, 3).brighter());
        float[] hsb = Color.RGBtoHSB(200, 100, 50, null);
        out.add("hsb " + Math.round(hsb[0] * 1e6) + " " + Math.round(hsb[1] * 1e6) + " "
                + Math.round(hsb[2] * 1e6) + " back " + Integer.toHexString(
                        Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])) + " "
                + Integer.toHexString(Color.HSBtoRGB(0.75f, 0.5f, 0.25f)) + " "
                + Integer.toHexString(Color.HSBtoRGB(1.5f, 1f, 1f)) + " "
                + Color.getHSBColor(0.1f, 0.2f, 0.3f));
        out.add("decode " + Color.decode("#123456") + " " + Color.decode("0x10") + " float "
                + new Color(0.5f, 0.25f, 1f) + " " + new Color(0x80112233, true).getAlpha()
                + " " + new Color(0x80112233).getAlpha());
        out.add("transparency " + Color.RED.getTransparency() + color.getTransparency()
                + new Color(0, 0, 0, 0).getTransparency() + " equal "
                + new Color(255, 0, 0).equals(Color.RED) + " " + Color.ORANGE + Color.PINK);
        try {
            new Color(300, -1, 0, 256);
            out.add("a colour out of range");
        } catch (IllegalArgumentException wrong) {
            out.add(wrong.getMessage());
        }
    }

    private static void strokes(List<String> out) {
        BasicStroke dashed = new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_BEVEL,
                4, new float[] {3, 1}, 0.5f);
        out.add("hash " + new BasicStroke().hashCode() + " " + dashed.hashCode() + " equal "
                + dashed.equals(new BasicStroke(2.5f, 1, 2, 4, new float[] {3, 1}, 0.5f))
                + new BasicStroke().equals(new BasicStroke(1)));
        String[] refused = new String[4];
        try {
            new BasicStroke(-1);
        } catch (IllegalArgumentException wrong) {
            refused[0] = wrong.getMessage();
        }
        try {
            new BasicStroke(1, 0, 0, 0.5f);
        } catch (IllegalArgumentException wrong) {
            refused[1] = wrong.getMessage();
        }
        try {
            new BasicStroke(1, 0, 0, 1, new float[] {0, 0}, 0);
        } catch (IllegalArgumentException wrong) {
            refused[2] = wrong.getMessage();
        }
        try {
            new BasicStroke(1, 7, 0);
        } catch (IllegalArgumentException wrong) {
            refused[3] = wrong.getMessage();
        }
        out.add(String.join(", ", refused));
        AlphaComposite half = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
        out.add("composite " + half.getRule() + " " + half.getAlpha() + " "
                + half.derive(AlphaComposite.DST_IN).getRule() + " "
                + (half.derive(1.0f) == AlphaComposite.SrcOver) + " " + half.hashCode() + " "
                + half.equals(AlphaComposite.SrcOver.derive(0.5f)));
    }

    private static void fonts(List<String> out) {
        Font font = new Font(Font.DIALOG, Font.BOLD, 12);
        out.add(font + " " + font.deriveFont(20.6f).getSize() + " " + font.deriveFont(20.6f)
                .getSize2D() + " " + font.deriveFont(Font.ITALIC).getStyle() + " "
                + new Font(Font.SERIF, 9, 3).getStyle() + " " + font.isBold() + font.isItalic()
                + font.isPlain());
        out.add("equal " + font.equals(new Font("Dialog", 1, 12)) + " "
                + font.equals(font.deriveFont(12f)) + " " + font.equals(font.deriveFont(13f)));
    }

    private static void state(List<String> out) {
        Graphics2D graphics = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB)
                .createGraphics();
        out.add(graphics.getColor() + " " + graphics.getBackground() + " " + graphics.getFont()
                + " " + ((AlphaComposite) graphics.getComposite()).getRule() + " "
                + graphics.getTransform() + " " + graphics.getClip() + " "
                + (graphics.getRenderingHint(RenderingHints.KEY_ANTIALIASING)
                        == RenderingHints.VALUE_ANTIALIAS_OFF) + " "
                + graphics.getStroke().equals(new BasicStroke()));
        graphics.translate(3, 4);
        graphics.clipRect(0, 0, 5, 5);
        graphics.clipRect(2, 2, 10, 10);
        out.add("clip " + graphics.getClipBounds() + " " + graphics.getClip() + " "
                + graphics.hitClip(0, 0, 2, 2) + graphics.hitClip(1, 1, 2, 2));
        Graphics2D copy = (Graphics2D) graphics.create();
        copy.scale(2, 2);
        copy.setColor(Color.RED);
        out.add("copy " + copy.getTransform() + " " + copy.getClipBounds() + " "
                + graphics.getTransform() + " " + graphics.getColor());
        graphics.setClip(null);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        out.add("unclipped " + graphics.getClip() + " " + (graphics.getRenderingHint(
                RenderingHints.KEY_ANTIALIASING) == RenderingHints.VALUE_ANTIALIAS_ON));
        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_RENDER_QUALITY);
            out.add("a hint took the wrong value");
        } catch (IllegalArgumentException wrong) {
            out.add("a hint refuses a value for another key");
        }
    }

    private static void pixels(List<String> out) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(1, 1, 3, 2);
        graphics.setColor(Color.RED);
        graphics.drawLine(0, 5, 6, 5);
        graphics.drawLine(7, 7, 7, 3);
        out.add("filled " + rows(image));
        graphics.setColor(Color.GREEN);
        graphics.drawRect(0, 0, 4, 3);
        graphics.setBackground(new Color(0x20, 0x40, 0x60, 0x80));
        graphics.clearRect(5, 0, 2, 2);
        out.add("outlined " + rows(image));
        graphics.copyArea(0, 0, 3, 3, 4, 4);
        out.add("copied " + rows(image));

        BufferedImage target = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        target.setRGB(0, 0, 0xFF0000FF);
        target.setRGB(1, 0, 0x400000FF);
        BufferedImage half = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 3; x++) {
            half.setRGB(x, 0, 0x80FF0000);
        }
        target.createGraphics().drawImage(half, 0, 0, null);
        out.add("over " + rows(target));

        BufferedImage strip = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < 4; x++) {
            strip.setRGB(x, 0, 0xFF000000 | x * 0x40);
        }
        BufferedImage flipped = new BufferedImage(4, 1, BufferedImage.TYPE_INT_ARGB);
        flipped.createGraphics().drawImage(strip, 0, 0, 4, 1, 4, 0, 0, 1, null);
        BufferedImage doubled = new BufferedImage(8, 2, BufferedImage.TYPE_INT_ARGB);
        doubled.createGraphics().drawImage(strip, 0, 0, 8, 2, null);
        out.add("flipped " + rows(flipped) + " doubled " + rows(doubled));

        BufferedImage ruled = new BufferedImage(3, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D rules = ruled.createGraphics();
        rules.setColor(Color.WHITE);
        rules.fillRect(0, 0, 3, 1);
        rules.setComposite(AlphaComposite.Src);
        rules.setColor(new Color(10, 20, 30, 40));
        rules.fillRect(0, 0, 1, 1);
        rules.setComposite(AlphaComposite.Clear);
        rules.fillRect(1, 0, 1, 1);
        rules.setComposite(AlphaComposite.SrcOver);
        rules.setColor(new Color(0, 0, 0, 0));
        rules.fillRect(2, 0, 1, 1);
        out.add("rules " + rows(ruled));

        BufferedImage rgb = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        rgb.setRGB(0, 0, 0x12345678);
        rgb.createGraphics().drawImage(half, -1, 0, null);
        out.add("rgb " + rows(rgb) + " raw " + Integer.toHexString(
                ((DataBufferInt) rgb.getRaster().getDataBuffer()).getData()[0]) + " "
                + rgb.getTransparency() + image.getTransparency());

        int[] copy = ((DataBufferInt) image.getData().getDataBuffer()).getData();
        copy[0] = 7;
        out.add("data is a copy " + (image.getRGB(0, 0) != 7));
    }

    /** shapes on whole pixels, which a canvas covers exactly too */
    private static void canvas(List<String> out) {
        BufferedImage image = new BufferedImage(8, 8, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fill(new Polygon(new int[] {1, 4, 4, 1}, new int[] {1, 1, 3, 3}, 4));
        graphics.setColor(Color.GREEN);
        graphics.draw(new Rectangle(5, 4, 2, 3));
        Graphics2D scaled = (Graphics2D) graphics.create();
        scaled.scale(2, 2);
        scaled.setColor(Color.BLUE);
        scaled.fillRect(0, 2, 1, 1);
        out.add("shapes " + rows(image));

        BufferedImage turned = new BufferedImage(6, 6, BufferedImage.TYPE_INT_ARGB);
        Graphics2D turning = turned.createGraphics();
        turning.rotate(Math.PI / 2, 3, 3);
        turning.setColor(Color.WHITE);
        turning.fillRect(1, 1, 2, 1);
        turning.setColor(Color.YELLOW);
        turning.clip(new Rectangle(0, 0, 6, 4));
        turning.fill(new Path2D.Double(new Rectangle(4, 2, 2, 4)));
        out.add("turned " + rows(turned));

        BufferedImage placed = new BufferedImage(6, 2, BufferedImage.TYPE_INT_ARGB);
        BufferedImage dot = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        dot.setRGB(0, 0, 0xFF00FFFF);
        Graphics2D placing = placed.createGraphics();
        placing.drawImage(dot, AffineTransform.getTranslateInstance(1, 0), null);
        placing.drawImage(dot, 3, 0, 6, 2, 0, 0, 1, 1, null);
        out.add("placed " + rows(placed));
    }

    private static void imageio(List<String> out) throws IOException {
        for (String[] file : new String[][] {{"bmp", BMP}, {"gif", GIF}, {"png", PNG}}) {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(
                    Base64.getDecoder().decode(file[1])));
            out.add(file[0] + " " + image.getWidth() + "x" + image.getHeight() + " "
                    + rows(image));
        }
        BufferedImage jpeg = ImageIO.read(new ByteArrayInputStream(
                Base64.getDecoder().decode(JPEG)));
        int pixel = jpeg.getRGB(4, 4);
        boolean close = Math.abs((pixel >> 16 & 0xFF) - 0x33) < 6
                && Math.abs((pixel >> 8 & 0xFF) - 0x66) < 6 && Math.abs((pixel & 0xFF) - 0xCC) < 6;
        out.add("jpeg " + jpeg.getWidth() + "x" + jpeg.getHeight() + " type " + jpeg.getType()
                + " close to 3366cc " + close);
        out.add("unknown " + ImageIO.read(new ByteArrayInputStream(new byte[] {1, 2, 3, 4}))
                + " empty " + ImageIO.read(new ByteArrayInputStream(new byte[0])));
        byte[] png = Base64.getDecoder().decode(PNG);
        try {
            ImageIO.read(new ByteArrayInputStream(java.util.Arrays.copyOf(png, 40)));
            out.add("a cut png read");
        } catch (IOException cut) {
            out.add("a cut png fails");
        }
    }

    /** each pixel as hex, rows split by a bar; a clear pixel is 0 whatever its colour */
    private static String rows(BufferedImage image) {
        StringBuilder written = new StringBuilder();
        for (int y = 0; y < image.getHeight(); y++) {
            if (y > 0) {
                written.append(" |");
            }
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getRGB(x, y);
                written.append(' ').append(Integer.toHexString(pixel >>> 24 == 0 ? 0 : pixel));
            }
        }
        return written.toString().trim();
    }

    private static String segments(PathIterator path) {
        StringBuilder written = new StringBuilder(path.getWindingRule() + ":");
        double[] point = new double[6];
        while (!path.isDone()) {
            int type = path.currentSegment(point);
            written.append(' ').append(type);
            int count = type == PathIterator.SEG_CLOSE ? 0 : type == PathIterator.SEG_QUADTO ? 4
                    : type == PathIterator.SEG_CUBICTO ? 6 : 2;
            for (int i = 0; i < count; i++) {
                written.append(i == 0 ? "(" : ",").append(Math.round(point[i] * 1e6) / 1e6);
            }
            written.append(count > 0 ? ")" : "");
            path.next();
        }
        return written.toString();
    }
}
