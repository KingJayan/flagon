package java.awt.image;

import dev.flagon.awt.Text;
import dev.flagon.platform.graphics.Canvas2D;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * draws into a {@link BufferedImage}. what a pixel loop can do exactly, as the jdk does, it does
 * in java: opaque fills, straight thin lines and unscaled copies. everything else goes through
 * the image's canvas, which always antialiases.
 */
final class ImageGraphics extends Graphics2D {

    private static final BasicStroke THIN = new BasicStroke();

    private final BufferedImage image;
    private Color color = Color.WHITE;
    private Color background = Color.BLACK;
    private Font font = new Font(Font.DIALOG, Font.PLAIN, 12);
    private BasicStroke stroke = THIN;
    private AlphaComposite composite = AlphaComposite.SrcOver;
    private RenderingHints hints = defaults();
    private AffineTransform transform = new AffineTransform();
    /** device space; every one of them clips */
    private List<Shape> clips = new ArrayList<>();

    ImageGraphics(BufferedImage image) {
        this.image = image;
    }

    private ImageGraphics(ImageGraphics from) {
        this.image = from.image;
        this.color = from.color;
        this.background = from.background;
        this.font = from.font;
        this.stroke = from.stroke;
        this.composite = from.composite;
        this.hints = (RenderingHints) from.hints.clone();
        this.transform = new AffineTransform(from.transform);
        this.clips = new ArrayList<>(from.clips);
    }

    private static RenderingHints defaults() {
        RenderingHints hints = new RenderingHints(null);
        hints.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        hints.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_DEFAULT);
        hints.put(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_DEFAULT);
        hints.put(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        hints.put(RenderingHints.KEY_TEXT_LCD_CONTRAST, 140);
        hints.put(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_DEFAULT);
        return hints;
    }

    @Override
    public Graphics create() {
        return new ImageGraphics(this);
    }

    @Override
    public void dispose() {
    }

    @Override
    public Color getColor() {
        return color;
    }

    @Override
    public void setColor(Color color) {
        if (color != null) {
            this.color = color;
        }
    }

    @Override
    public Paint getPaint() {
        return color;
    }

    @Override
    public void setPaint(Paint paint) {
        if (paint instanceof Color given) {
            color = given;
        } else if (paint != null) {
            throw new UnsupportedOperationException("a bundle paints with a solid Color, not "
                    + paint.getClass().getName());
        }
    }

    @Override
    public void setPaintMode() {
    }

    @Override
    public void setXORMode(Color color) {
        throw new UnsupportedOperationException("a bundle cannot draw in XOR mode");
    }

    @Override
    public Color getBackground() {
        return background;
    }

    @Override
    public void setBackground(Color color) {
        background = color;
    }

    @Override
    public Font getFont() {
        return font;
    }

    @Override
    public void setFont(Font font) {
        if (font != null) {
            this.font = font;
        }
    }

    @Override
    public FontMetrics getFontMetrics(Font font) {
        return Text.metrics(font);
    }

    @Override
    public Stroke getStroke() {
        return stroke;
    }

    @Override
    public void setStroke(Stroke stroke) {
        if (stroke == null) {
            throw new IllegalArgumentException("null Stroke");
        }
        if (!(stroke instanceof BasicStroke basic)) {
            throw new UnsupportedOperationException("a bundle draws with a BasicStroke, not "
                    + stroke.getClass().getName());
        }
        this.stroke = basic;
    }

    @Override
    public Composite getComposite() {
        return composite;
    }

    @Override
    public void setComposite(Composite composite) {
        if (composite == null) {
            throw new IllegalArgumentException("null Composite");
        }
        if (!(composite instanceof AlphaComposite alpha)) {
            throw new UnsupportedOperationException("a bundle blends by an AlphaComposite, not "
                    + composite.getClass().getName());
        }
        this.composite = alpha;
    }

    @Override
    public void setRenderingHint(RenderingHints.Key key, Object value) {
        hints.put(key, value);
    }

    @Override
    public Object getRenderingHint(RenderingHints.Key key) {
        return hints.get(key);
    }

    @Override
    public void setRenderingHints(Map<?, ?> given) {
        hints = defaults();
        hints.putAll(given);
    }

    @Override
    public void addRenderingHints(Map<?, ?> given) {
        hints.putAll(given);
    }

    @Override
    public RenderingHints getRenderingHints() {
        return (RenderingHints) hints.clone();
    }

    @Override
    public void translate(int x, int y) {
        transform.translate(x, y);
    }

    @Override
    public void translate(double tx, double ty) {
        transform.translate(tx, ty);
    }

    @Override
    public void rotate(double theta) {
        transform.rotate(theta);
    }

    @Override
    public void rotate(double theta, double x, double y) {
        transform.rotate(theta, x, y);
    }

    @Override
    public void scale(double sx, double sy) {
        transform.scale(sx, sy);
    }

    @Override
    public void shear(double shx, double shy) {
        transform.shear(shx, shy);
    }

    @Override
    public void transform(AffineTransform more) {
        transform.concatenate(more);
    }

    @Override
    public void setTransform(AffineTransform given) {
        transform = new AffineTransform(given);
    }

    @Override
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    @Override
    public Shape getClip() {
        if (clips.isEmpty()) {
            return null;
        }
        Shape device = clips.size() == 1 ? clips.get(0) : clipBounds();
        if (device instanceof Rectangle2D box && translation() != null) {
            double x = box.getX() - transform.getTranslateX();
            double y = box.getY() - transform.getTranslateY();
            if (whole(x) && whole(y) && whole(box.getWidth()) && whole(box.getHeight())) {
                return new Rectangle((int) x, (int) y, (int) box.getWidth(),
                        (int) box.getHeight());
            }
            return new Rectangle2D.Double(x, y, box.getWidth(), box.getHeight());
        }
        try {
            return transform.createInverse().createTransformedShape(device);
        } catch (NoninvertibleTransformException flat) {
            return null;
        }
    }

    @Override
    public Rectangle getClipBounds() {
        Shape clip = getClip();
        return clip == null ? null : clip.getBounds();
    }

    @Override
    public void setClip(Shape clip) {
        clips = new ArrayList<>();
        if (clip != null) {
            clips.add(device(clip));
        }
    }

    @Override
    public void setClip(int x, int y, int width, int height) {
        setClip(new Rectangle(x, y, width, height));
    }

    @Override
    public void clip(Shape shape) {
        if (shape == null) {
            clips = new ArrayList<>();
            return;
        }
        Shape device = device(shape);
        int last = clips.size() - 1;
        if (last >= 0 && clips.get(last) instanceof Rectangle2D before
                && device instanceof Rectangle2D now) {
            Rectangle2D both = before.createIntersection(now);
            clips.set(last, both.isEmpty() ? new Rectangle2D.Double() : both);
        } else {
            clips.add(device);
        }
    }

    @Override
    public void clipRect(int x, int y, int width, int height) {
        clip(new Rectangle(x, y, width, height));
    }

    private Shape device(Shape shape) {
        int[] offset = translation();
        if (shape instanceof Rectangle2D box && offset != null) {
            return new Rectangle2D.Double(box.getX() + offset[0], box.getY() + offset[1],
                    box.getWidth(), box.getHeight());
        }
        return transform.createTransformedShape(shape);
    }

    private Rectangle2D clipBounds() {
        Rectangle2D bounds = new Rectangle2D.Double(0, 0, image.getWidth(), image.getHeight());
        for (Shape clip : clips) {
            bounds = bounds.createIntersection(clip.getBounds2D());
        }
        return bounds;
    }

    @Override
    public void fillRect(int x, int y, int width, int height) {
        if (width > 0 && height > 0) {
            fill(new Rectangle(x, y, width, height));
        }
    }

    @Override
    public void drawRect(int x, int y, int width, int height) {
        if (width < 0 || height < 0) {
            return;
        }
        if (thinAndExact()) {
            super.drawRect(x, y, width, height);
        } else {
            draw(new Rectangle(x, y, width, height));
        }
    }

    @Override
    public void clearRect(int x, int y, int width, int height) {
        AlphaComposite was = composite;
        Color paint = color;
        composite = AlphaComposite.Src;
        color = background;
        try {
            fillRect(x, y, width, height);
        } finally {
            composite = was;
            color = paint;
        }
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2) {
        if ((x1 == x2 || y1 == y2) && thinAndExact()) {
            int[] offset = translation();
            fillPixels(Math.min(x1, x2) + offset[0], Math.min(y1, y2) + offset[1],
                    Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1, color.getRGB());
            return;
        }
        draw(new Line2D.Float(x1, y1, x2, y2));
    }

    @Override
    public void drawRoundRect(int x, int y, int width, int height, int arcWidth,
                              int arcHeight) {
        draw(new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight));
    }

    @Override
    public void fillRoundRect(int x, int y, int width, int height, int arcWidth,
                              int arcHeight) {
        fill(new RoundRectangle2D.Float(x, y, width, height, arcWidth, arcHeight));
    }

    @Override
    public void drawOval(int x, int y, int width, int height) {
        draw(new Ellipse2D.Float(x, y, width, height));
    }

    @Override
    public void fillOval(int x, int y, int width, int height) {
        fill(new Ellipse2D.Float(x, y, width, height));
    }

    @Override
    public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        arc(x, y, width, height, startAngle, arcAngle, false);
    }

    @Override
    public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        arc(x, y, width, height, startAngle, arcAngle, true);
    }

    /** awt angles run counterclockwise in degrees; a canvas's run clockwise in radians */
    private void arc(int x, int y, int width, int height, int startAngle, int arcAngle,
                     boolean pie) {
        if (width < 0 || height < 0 || arcAngle == 0) {
            return;
        }
        double sweep = Math.max(-360, Math.min(360, arcAngle));
        double start = -Math.toRadians(startAngle);
        double end = -Math.toRadians(startAngle + sweep);
        double cx = x + width / 2.0;
        double cy = y + height / 2.0;
        Rectangle2D bounds = new Rectangle2D.Double(x, y, width, height);
        paint(area(bounds, pie ? 0 : pad()), null, canvas -> {
            userSpace(canvas, !pie);
            canvas.beginPath();
            if (pie) {
                canvas.moveTo(cx, cy);
            }
            canvas.ellipse(cx, cy, width / 2.0, height / 2.0, start, end, sweep > 0);
            if (pie) {
                canvas.closePath();
                canvas.fill(false);
            } else {
                line(canvas);
                canvas.stroke();
            }
        });
    }

    @Override
    public void drawPolyline(int[] xPoints, int[] yPoints, int count) {
        if (count <= 0) {
            return;
        }
        Path2D path = new Path2D.Float();
        path.moveTo(xPoints[0], yPoints[0]);
        for (int i = 1; i < count; i++) {
            path.lineTo(xPoints[i], yPoints[i]);
        }
        draw(path);
    }

    @Override
    public void drawPolygon(int[] xPoints, int[] yPoints, int count) {
        draw(new Polygon(xPoints, yPoints, count));
    }

    @Override
    public void fillPolygon(int[] xPoints, int[] yPoints, int count) {
        fill(new Polygon(xPoints, yPoints, count));
    }

    @Override
    public void fill(Shape shape) {
        if (shape instanceof Rectangle2D box && fillsExactly(box)) {
            int[] offset = translation();
            fillPixels((int) box.getX() + offset[0], (int) box.getY() + offset[1],
                    (int) box.getWidth(), (int) box.getHeight(), color.getRGB());
            return;
        }
        paint(area(shape.getBounds2D(), 0), shape, canvas -> {
            userSpace(canvas, false);
            PathIterator path = shape.getPathIterator(null);
            trace(canvas, path);
            canvas.fill(path.getWindingRule() == PathIterator.WIND_EVEN_ODD);
        });
    }

    @Override
    public void draw(Shape shape) {
        if (shape instanceof Rectangle2D box && whole(box.getX()) && whole(box.getY())
                && whole(box.getWidth()) && whole(box.getHeight()) && thinAndExact()) {
            drawRect((int) box.getX(), (int) box.getY(), (int) box.getWidth(),
                    (int) box.getHeight());
            return;
        }
        if (shape instanceof Line2D line && (line.getX1() == line.getX2()
                || line.getY1() == line.getY2()) && whole(line.getX1()) && whole(line.getY1())
                && whole(line.getX2()) && whole(line.getY2()) && thinAndExact()) {
            drawLine((int) line.getX1(), (int) line.getY1(), (int) line.getX2(),
                    (int) line.getY2());
            return;
        }
        paint(area(shape.getBounds2D(), pad()), null, canvas -> {
            userSpace(canvas, true);
            trace(canvas, shape.getPathIterator(null));
            line(canvas);
            canvas.stroke();
        });
    }

    @Override
    public void drawString(String text, int x, int y) {
        drawString(text, (float) x, (float) y);
    }

    @Override
    public void drawString(String text, float x, float y) {
        if (text == null) {
            throw new NullPointerException("String is null");
        }
        if (text.isEmpty()) {
            return;
        }
        double[] measured = Text.measure(font, text);
        Rectangle2D bounds = new Rectangle2D.Double(x, y - measured[1], measured[0],
                measured[1] + measured[2]);
        String css = Text.css(font);
        paint(area(bounds, font.getSize2D() / 4.0 + 1), null, canvas -> {
            userSpace(canvas, false);
            canvas.font(css);
            canvas.fillText(text, x, y);
        });
    }

    @Override
    public boolean drawImage(Image image, int x, int y, ImageObserver observer) {
        return drawImage(image, x, y, null, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, Color background,
                             ImageObserver observer) {
        BufferedImage source = source(image);
        int width = source.getWidth();
        int height = source.getHeight();
        return drawImage(image, x, y, x + width, y + height, 0, 0, width, height, background,
                observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
                             ImageObserver observer) {
        return drawImage(image, x, y, width, height, null, observer);
    }

    @Override
    public boolean drawImage(Image image, int x, int y, int width, int height,
                             Color background, ImageObserver observer) {
        BufferedImage source = source(image);
        if (width == 0 || height == 0) {
            return true;
        }
        return drawImage(image, x, y, x + width, y + height, 0, 0, source.getWidth(),
                source.getHeight(), background, observer);
    }

    @Override
    public boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                             int fromX1, int fromY1, int fromX2, int fromY2,
                             ImageObserver observer) {
        return drawImage(image, toX1, toY1, toX2, toY2, fromX1, fromY1, fromX2, fromY2, null,
                observer);
    }

    @Override
    public boolean drawImage(Image image, int toX1, int toY1, int toX2, int toY2,
                             int fromX1, int fromY1, int fromX2, int fromY2, Color background,
                             ImageObserver observer) {
        BufferedImage source = source(image);
        if (toX1 == toX2 || toY1 == toY2 || fromX1 == fromX2 || fromY1 == fromY2) {
            return true;
        }
        if (copiesExactly(toX2 - toX1, toY2 - toY1, fromX2 - fromX1, fromY2 - fromY1)) {
            int[] offset = translation();
            copyPixels(source, toX1 + offset[0], toY1 + offset[1], toX2 + offset[0],
                    toY2 + offset[1], fromX1, fromY1, fromX2, fromY2, background);
            return true;
        }
        AffineTransform place = new AffineTransform();
        place.translate(toX1, toY1);
        place.scale((double) (toX2 - toX1) / (fromX2 - fromX1),
                (double) (toY2 - toY1) / (fromY2 - fromY1));
        place.translate(-fromX1, -fromY1);
        draw(source, place, Math.min(fromX1, fromX2), Math.min(fromY1, fromY2),
                Math.abs(fromX2 - fromX1), Math.abs(fromY2 - fromY1), background);
        return true;
    }

    @Override
    public boolean drawImage(Image image, AffineTransform place, ImageObserver observer) {
        BufferedImage source = source(image);
        draw(source, place == null ? new AffineTransform() : place, 0, 0, source.getWidth(),
                source.getHeight(), null);
        return true;
    }

    private void draw(BufferedImage source, AffineTransform place, int x, int y, int width,
                      int height, Color behind) {
        Rectangle2D part = new Rectangle2D.Double(x, y, width, height);
        Shape placed = place.createTransformedShape(part);
        if (behind != null) {
            Color was = color;
            color = behind;
            try {
                fill(placed);
            } finally {
                color = was;
            }
        }
        boolean smooth = hints.get(RenderingHints.KEY_INTERPOLATION) != null
                && hints.get(RenderingHints.KEY_INTERPOLATION)
                        != RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR;
        Canvas2D from = source.surface();
        paint(area(placed.getBounds2D(), 0), placed, canvas -> {
            AffineTransform full = new AffineTransform(transform);
            full.concatenate(place);
            set(canvas, full);
            canvas.smoothing(smooth);
            canvas.drawImage(from, x, y, width, height, x, y, width, height);
        });
    }

    @Override
    public void copyArea(int x, int y, int width, int height, int dx, int dy) {
        if (width <= 0 || height <= 0) {
            return;
        }
        BufferedImage before = image.getSubimage(0, 0, image.getWidth(), image.getHeight());
        AlphaComposite was = composite;
        composite = AlphaComposite.Src;
        try {
            int[] offset = translation();
            if (offset == null || clipRectangle() == null) {
                drawImage(before, x + dx, y + dy, x + dx + width, y + dy + height, x, y,
                        x + width, y + height, null);
            } else {
                int left = x + offset[0];
                int top = y + offset[1];
                copyPixels(before, left + dx, top + dy, left + dx + width, top + dy + height,
                        left, top, left + width, top + height, null);
            }
        } finally {
            composite = was;
        }
    }

    private static BufferedImage source(Image image) {
        if (image instanceof BufferedImage buffered) {
            return buffered;
        }
        throw new UnsupportedOperationException(
                "Graphics draws a BufferedImage here; there is no other kind of image in a "
                        + "bundle");
    }

    // what java draws itself

    /** @return the whole-pixel offset the transform moves by, or null when it does more */
    private int[] translation() {
        if (transform.getScaleX() != 1.0 || transform.getScaleY() != 1.0
                || transform.getShearX() != 0.0 || transform.getShearY() != 0.0
                || !whole(transform.getTranslateX()) || !whole(transform.getTranslateY())) {
            return null;
        }
        return new int[] {(int) transform.getTranslateX(), (int) transform.getTranslateY()};
    }

    private static boolean whole(double value) {
        return value == Math.rint(value) && Math.abs(value) < Integer.MAX_VALUE;
    }

    /** @return the device rectangle every clip leaves, or null when a clip is not one */
    private Rectangle clipRectangle() {
        Rectangle area = new Rectangle(0, 0, image.getWidth(), image.getHeight());
        for (Shape clip : clips) {
            if (!(clip instanceof Rectangle2D box) || !whole(box.getX()) || !whole(box.getY())
                    || !whole(box.getWidth()) || !whole(box.getHeight())) {
                return null;
            }
            area = area.intersection(new Rectangle((int) box.getX(), (int) box.getY(),
                    (int) box.getWidth(), (int) box.getHeight()));
        }
        return area;
    }

    private boolean pixelRules() {
        int rule = composite.getRule();
        return composite.getAlpha() == 1.0f && (rule == AlphaComposite.SRC_OVER
                || rule == AlphaComposite.SRC || rule == AlphaComposite.CLEAR);
    }

    private boolean fillsExactly(Rectangle2D box) {
        return translation() != null && clipRectangle() != null && pixelRules()
                && whole(box.getX()) && whole(box.getY()) && whole(box.getWidth())
                && whole(box.getHeight()) && (composite.getRule() != AlphaComposite.SRC_OVER
                        || color.getAlpha() == 0xFF || color.getAlpha() == 0);
    }

    private boolean thinAndExact() {
        return THIN.equals(stroke) && !antialiased() && translation() != null
                && clipRectangle() != null && pixelRules()
                && (composite.getRule() != AlphaComposite.SRC_OVER
                        || color.getAlpha() == 0xFF || color.getAlpha() == 0);
    }

    private boolean copiesExactly(int toWidth, int toHeight, int fromWidth, int fromHeight) {
        Object interpolation = hints.get(RenderingHints.KEY_INTERPOLATION);
        boolean scaled = Math.abs(toWidth) != Math.abs(fromWidth)
                || Math.abs(toHeight) != Math.abs(fromHeight);
        return (!scaled || interpolation == null
                || interpolation == RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR)
                && translation() != null && clipRectangle() != null && pixelRules()
                && Math.abs(toWidth) % Math.abs(fromWidth) == 0
                && Math.abs(toHeight) % Math.abs(fromHeight) == 0;
    }

    private void fillPixels(int x, int y, int width, int height, int argb) {
        int rule = composite.getRule();
        if (rule == AlphaComposite.SRC_OVER && argb >>> 24 == 0) {
            return;
        }
        int value = rule == AlphaComposite.CLEAR ? 0 : image.stored(argb);
        Rectangle area = clipRectangle().intersection(new Rectangle(x, y, width, height));
        if (area.isEmpty()) {
            return;
        }
        int[] pixels = image.pixels();
        int stride = image.getWidth();
        for (int row = area.y; row < area.y + area.height; row++) {
            java.util.Arrays.fill(pixels, row * stride + area.x,
                    row * stride + area.x + area.width, value);
        }
        image.changed();
    }

    /** a nearest neighbour copy, mirrored when the corners say so; scales up by whole steps */
    private void copyPixels(BufferedImage source, int toX1, int toY1, int toX2, int toY2,
                            int fromX1, int fromY1, int fromX2, int fromY2, Color behind) {
        Rectangle area = clipRectangle().intersection(new Rectangle(Math.min(toX1, toX2),
                Math.min(toY1, toY2), Math.abs(toX2 - toX1), Math.abs(toY2 - toY1)));
        if (area.isEmpty()) {
            return;
        }
        int rule = composite.getRule();
        int[] into = image.pixels();
        int[] from = source == image ? image.pixels().clone() : source.pixels();
        int stride = image.getWidth();
        int fromStride = source.getWidth();
        int toWidth = toX2 - toX1;
        int toHeight = toY2 - toY1;
        int fromWidth = fromX2 - fromX1;
        int fromHeight = fromY2 - fromY1;
        int under = behind == null ? 0 : behind.getRGB();
        for (int y = area.y; y < area.y + area.height; y++) {
            int sourceY = flipped(y - toY1, toHeight, fromY1, fromHeight);
            if (sourceY < 0 || sourceY >= source.getHeight()) {
                continue;
            }
            for (int x = area.x; x < area.x + area.width; x++) {
                int sourceX = flipped(x - toX1, toWidth, fromX1, fromWidth);
                if (sourceX < 0 || sourceX >= source.getWidth()) {
                    continue;
                }
                int pixel = source.read(from[sourceY * fromStride + sourceX]);
                if (behind != null) {
                    pixel = over(pixel, under);
                }
                int at = y * stride + x;
                if (rule == AlphaComposite.CLEAR) {
                    into[at] = 0;
                } else if (rule == AlphaComposite.SRC) {
                    into[at] = image.stored(pixel);
                } else {
                    into[at] = image.stored(over(pixel, image.read(into[at])));
                }
            }
        }
        image.changed();
    }

    /** the source pixel whose centre a destination pixel's centre maps onto */
    private static int flipped(int along, int toLength, int fromStart, int fromLength) {
        int steps = toLength / fromLength;
        if (toLength < 0) {
            along = -along - 1;
        }
        int index = along / Math.abs(steps);
        return fromLength > 0 ? fromStart + index : fromStart - 1 - index;
    }

    /** non-premultiplied source over, rounded the way 8-bit tables round */
    static int over(int source, int target) {
        int sa = source >>> 24;
        if (sa == 0xFF) {
            return source;
        }
        if (sa == 0) {
            return target;
        }
        int ta = multiply(255 - sa, target >>> 24);
        int alpha = sa + ta;
        int out = alpha << 24;
        for (int shift = 16; shift >= 0; shift -= 8) {
            int channel = multiply(sa, (source >> shift) & 0xFF)
                    + multiply(ta, (target >> shift) & 0xFF);
            out |= Math.min(255, (int) Math.round(channel * 255.0 / alpha)) << shift;
        }
        return out;
    }

    private static int multiply(int a, int b) {
        return (a * b + 127) / 255;
    }

    // what the canvas draws

    private boolean antialiased() {
        return hints.get(RenderingHints.KEY_ANTIALIASING) == RenderingHints.VALUE_ANTIALIAS_ON;
    }

    /** how far past its shape an outline can reach, in user space */
    private double pad() {
        double half = Math.max(stroke.getLineWidth(), 1) / 2.0;
        double reach = stroke.getLineJoin() == BasicStroke.JOIN_MITER
                ? Math.max(stroke.getMiterLimit(), Math.sqrt(2)) : Math.sqrt(2);
        return half * reach + 1;
    }

    /** @return the device pixels a shape with these user bounds can touch, clipped */
    private Rectangle area(Rectangle2D bounds, double pad) {
        double[] corners = {
            bounds.getMinX() - pad, bounds.getMinY() - pad,
            bounds.getMaxX() + pad, bounds.getMinY() - pad,
            bounds.getMaxX() + pad, bounds.getMaxY() + pad,
            bounds.getMinX() - pad, bounds.getMaxY() + pad,
        };
        transform.transform(corners, 0, corners, 0, 4);
        double left = Double.POSITIVE_INFINITY;
        double top = Double.POSITIVE_INFINITY;
        double right = Double.NEGATIVE_INFINITY;
        double bottom = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < corners.length; i += 2) {
            left = Math.min(left, corners[i]);
            right = Math.max(right, corners[i]);
            top = Math.min(top, corners[i + 1]);
            bottom = Math.max(bottom, corners[i + 1]);
        }
        Rectangle2D reach = new Rectangle2D.Double(Math.floor(left) - 1, Math.floor(top) - 1,
                Math.ceil(right) - Math.floor(left) + 2, Math.ceil(bottom) - Math.floor(top) + 2);
        Rectangle2D clipped = reach.createIntersection(clipBounds());
        if (clipped.isEmpty() || Double.isNaN(clipped.getWidth())) {
            return new Rectangle();
        }
        int x = (int) Math.floor(clipped.getX());
        int y = (int) Math.floor(clipped.getY());
        return new Rectangle(x, y, (int) Math.ceil(clipped.getMaxX()) - x,
                (int) Math.ceil(clipped.getMaxY()) - y);
    }

    private interface Drawn {
        void on(Canvas2D canvas);
    }

    /**
     * draws on the canvas inside {@code area}, then copies the area back. a canvas operation
     * that clears what it does not cover, as copy does, is held to {@code exact} too.
     */
    private void paint(Rectangle area, Shape exact, Drawn drawn) {
        int rule = composite.getRule();
        if (area.isEmpty() || rule == AlphaComposite.DST) {
            return;
        }
        Canvas2D canvas = image.surface();
        canvas.save();
        try {
            canvas.setTransform(1, 0, 0, 1, 0, 0);
            canvas.beginPath();
            canvas.moveTo(area.x, area.y);
            canvas.lineTo(area.x + area.width, area.y);
            canvas.lineTo(area.x + area.width, area.y + area.height);
            canvas.lineTo(area.x, area.y + area.height);
            canvas.closePath();
            canvas.clip(false);
            for (Shape clip : clips) {
                PathIterator path = clip.getPathIterator(null);
                trace(canvas, path);
                canvas.clip(path.getWindingRule() == PathIterator.WIND_EVEN_ODD);
            }
            if (exact != null && unbounded(rule)) {
                set(canvas, transform);
                PathIterator path = exact.getPathIterator(null);
                trace(canvas, path);
                canvas.clip(path.getWindingRule() == PathIterator.WIND_EVEN_ODD);
            }
            canvas.composite(operation(rule));
            canvas.alpha(rule == AlphaComposite.CLEAR ? 1.0 : composite.getAlpha());
            canvas.color(rule == AlphaComposite.CLEAR ? 0xFF000000 : color.getRGB());
            drawn.on(canvas);
        } finally {
            canvas.restore();
        }
        image.drawn(area.x, area.y, area.width, area.height);
    }

    private static boolean unbounded(int rule) {
        return rule == AlphaComposite.SRC || rule == AlphaComposite.SRC_IN
                || rule == AlphaComposite.DST_IN || rule == AlphaComposite.SRC_OUT
                || rule == AlphaComposite.DST_ATOP;
    }

    private static String operation(int rule) {
        return switch (rule) {
            case AlphaComposite.CLEAR, AlphaComposite.DST_OUT -> "destination-out";
            case AlphaComposite.SRC -> "copy";
            case AlphaComposite.DST_OVER -> "destination-over";
            case AlphaComposite.SRC_IN -> "source-in";
            case AlphaComposite.DST_IN -> "destination-in";
            case AlphaComposite.SRC_OUT -> "source-out";
            case AlphaComposite.SRC_ATOP -> "source-atop";
            case AlphaComposite.DST_ATOP -> "destination-atop";
            case AlphaComposite.XOR -> "xor";
            default -> "source-over";
        };
    }

    /**
     * user space on the canvas. outlines without antialiasing move half a pixel, so a one
     * pixel line covers whole pixels as the jdk's do.
     */
    private void userSpace(Canvas2D canvas, boolean outline) {
        if (outline && !antialiased() && hints.get(RenderingHints.KEY_STROKE_CONTROL)
                != RenderingHints.VALUE_STROKE_PURE) {
            AffineTransform moved = AffineTransform.getTranslateInstance(0.5, 0.5);
            moved.concatenate(transform);
            set(canvas, moved);
        } else {
            set(canvas, transform);
        }
    }

    private static void set(Canvas2D canvas, AffineTransform at) {
        canvas.setTransform(at.getScaleX(), at.getShearY(), at.getShearX(), at.getScaleY(),
                at.getTranslateX(), at.getTranslateY());
    }

    private void line(Canvas2D canvas) {
        double width = stroke.getLineWidth();
        if (width == 0) {
            double scale = Math.sqrt(Math.abs(transform.getDeterminant()));
            width = scale == 0 ? 1 : 1 / scale;
        }
        float[] dashes = stroke.getDashArray();
        double[] dash = new double[dashes == null ? 0 : dashes.length];
        for (int i = 0; i < dash.length; i++) {
            dash[i] = dashes[i];
        }
        String cap = switch (stroke.getEndCap()) {
            case BasicStroke.CAP_ROUND -> "round";
            case BasicStroke.CAP_SQUARE -> "square";
            default -> "butt";
        };
        String join = switch (stroke.getLineJoin()) {
            case BasicStroke.JOIN_ROUND -> "round";
            case BasicStroke.JOIN_BEVEL -> "bevel";
            default -> "miter";
        };
        canvas.line(width, cap, join, stroke.getMiterLimit(), dash, stroke.getDashPhase());
    }

    private static void trace(Canvas2D canvas, PathIterator path) {
        double[] point = new double[6];
        canvas.beginPath();
        while (!path.isDone()) {
            switch (path.currentSegment(point)) {
                case PathIterator.SEG_MOVETO -> canvas.moveTo(point[0], point[1]);
                case PathIterator.SEG_LINETO -> canvas.lineTo(point[0], point[1]);
                case PathIterator.SEG_QUADTO ->
                        canvas.quadTo(point[0], point[1], point[2], point[3]);
                case PathIterator.SEG_CUBICTO -> canvas.curveTo(point[0], point[1], point[2],
                        point[3], point[4], point[5]);
                default -> canvas.closePath();
            }
            path.next();
        }
    }
}
