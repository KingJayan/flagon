package org.example.blocks;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * a whole small game: a loop, input, textures, sound, a saved high score and a pause
 */
public final class BlocksGame {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int RATE = 60;

    private static final int COLUMNS = 10;
    private static final int ROWS = 5;
    private static final float BRICK_WIDTH = 60.0f;
    private static final float BRICK_HEIGHT = 16.0f;
    private static final float BRICK_TOP = 44.0f;
    private static final float MARGIN = 20.0f;

    private static final float PADDLE_WIDTH = 84.0f;
    private static final float PADDLE_HEIGHT = 10.0f;
    private static final float PADDLE_Y = 330.0f;
    private static final float PADDLE_SPEED = 7.0f;

    private static final float BALL_SIZE = 7.0f;
    private static final float BALL_SPEED = 4.2f;

    private static final int LIVES = 3;
    private static final int SAMPLE_RATE = 22_050;

    private static final String SAVE = "/flagon/blocks-high.txt";

    /** 0-9 as five columns of seven rows, one bit per pixel, drawn as quads */
    private static final int[] DIGITS = {
            0x7B6F, 0x2492, 0x73E7, 0x73CF, 0x5BE4, 0x79CF, 0x79EF, 0x7249, 0x7BEF, 0x7BCF,
    };

    private final boolean[] bricks = new boolean[COLUMNS * ROWS];
    private final float[] ball = new float[4];

    private int texture;
    private int brickSound;
    private int wallSound;
    private int lostSound;

    private float paddleX = (WIDTH - PADDLE_WIDTH) / 2.0f;
    private int score;
    private int lives = LIVES;
    private int highScore;
    private int cleared;
    private boolean over;
    private boolean paused;

    public static void main(String[] arguments) throws Exception {
        new BlocksGame().run(frameLimit(arguments));
    }

    private static int frameLimit(String[] arguments) {
        for (int at = 0; at + 1 < arguments.length; at++) {
            if (arguments[at].equals("--frames")) {
                return Integer.parseInt(arguments[at + 1]);
            }
        }
        return Integer.MAX_VALUE;
    }

    private void run(int frameLimit) throws Exception {
        Display.setDisplayMode(new DisplayMode(WIDTH, HEIGHT));
        Display.setTitle("Blocks");
        Display.create();
        Keyboard.create();
        AL.create();

        highScore = readHighScore();
        System.out.println("blocks: ready " + Display.getWidth() + "x" + Display.getHeight());
        System.out.println("blocks: high score " + highScore);

        buildLevel();
        texture = buildTexture();
        brickSound = buildSound(880.0f, 60);
        wallSound = buildSound(440.0f, 45);
        lostSound = buildSound(160.0f, 220);

        GL11.glViewport(0, 0, WIDTH, HEIGHT);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        int frame = 0;
        while (!Display.isCloseRequested() && frame < frameLimit && Display.isCreated()) {
            step();
            draw();
            Display.update();
            Display.sync(RATE);
            frame++;
        }

        saveHighScore();
        System.out.println("blocks: " + frame + " frames, score " + score);
        release();
    }

    // the game

    private void buildLevel() {
        java.util.Arrays.fill(bricks, true);
        cleared = 0;
        resetBall();
    }

    private void resetBall() {
        ball[0] = WIDTH / 2.0f;
        ball[1] = PADDLE_Y - 40.0f;
        ball[2] = BALL_SPEED * 0.6f;
        ball[3] = -BALL_SPEED;
    }

    private void step() {

        boolean watching = Display.isActive() && Display.isVisible();
        if (paused != !watching) {
            paused = !watching;
            System.out.println(paused ? "blocks: paused" : "blocks: resumed");
        }
        if (paused || over) {
            drainInput();
            return;
        }

        movePaddle();
        moveBall();
    }

    private void drainInput() {
        while (Keyboard.next()) {
            continue;
        }
    }

    private void movePaddle() {
        while (Keyboard.next()) {
            if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_ESCAPE) {
                Display.destroy();
                return;
            }
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_LEFT) || Keyboard.isKeyDown(Keyboard.KEY_A)) {
            paddleX -= PADDLE_SPEED;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_RIGHT) || Keyboard.isKeyDown(Keyboard.KEY_D)) {
            paddleX += PADDLE_SPEED;
        }
        if (Mouse.isButtonDown(0)) {
            paddleX = Mouse.getX() - PADDLE_WIDTH / 2.0f;
        }
        paddleX = Math.max(0.0f, Math.min(WIDTH - PADDLE_WIDTH, paddleX));
    }

    private void moveBall() {
        ball[0] += ball[2];
        ball[1] += ball[3];

        if (ball[0] < 0.0f || ball[0] + BALL_SIZE > WIDTH) {
            ball[2] = -ball[2];
            ball[0] = Math.max(0.0f, Math.min(WIDTH - BALL_SIZE, ball[0]));
            play(wallSound);
        }
        if (ball[1] < 0.0f) {
            ball[1] = 0.0f;
            ball[3] = -ball[3];
            play(wallSound);
        }

        if (ball[3] > 0.0f && overlaps(paddleX, PADDLE_Y, PADDLE_WIDTH, PADDLE_HEIGHT)) {
            ball[3] = -ball[3];
            ball[1] = PADDLE_Y - BALL_SIZE;

            float offset = (ball[0] + BALL_SIZE / 2.0f - paddleX) / PADDLE_WIDTH - 0.5f;
            ball[2] = BALL_SPEED * 2.0f * offset;
            play(wallSound);
        }

        hitBricks();

        if (ball[1] > HEIGHT) {
            lives--;
            play(lostSound);
            System.out.println("blocks: life lost, " + lives + " left");
            if (lives <= 0) {
                finish("out of lives");
            } else {
                resetBall();
            }
        }
    }

    private void hitBricks() {
        for (int index = 0; index < bricks.length; index++) {
            if (!bricks[index]) {
                continue;
            }
            float x = brickX(index);
            float y = brickY(index);
            if (!overlaps(x, y, BRICK_WIDTH - 4.0f, BRICK_HEIGHT - 4.0f)) {
                continue;
            }
            bricks[index] = false;
            cleared++;
            score += 10;
            ball[3] = -ball[3];
            play(brickSound);
            if (cleared == bricks.length) {
                finish("cleared");
            }
            return;
        }
    }

    private void finish(String why) {
        over = true;
        System.out.println("blocks: over, " + why + ", score " + score);
    }

    private boolean overlaps(float x, float y, float width, float height) {
        return ball[0] + BALL_SIZE > x && ball[0] < x + width
                && ball[1] + BALL_SIZE > y && ball[1] < y + height;
    }

    private static float brickX(int index) {
        return MARGIN + (index % COLUMNS) * BRICK_WIDTH;
    }

    private static float brickY(int index) {
        return BRICK_TOP + (index / COLUMNS) * BRICK_HEIGHT;
    }

    // drawing

    private void draw() {
        GL11.glClearColor(0.05f, 0.06f, 0.11f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0, WIDTH, HEIGHT, 0.0, -1.0, 1.0);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        drawBricks();

        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glColor4f(0.85f, 0.87f, 0.95f, 1.0f);
        quad(paddleX, PADDLE_Y, PADDLE_WIDTH, PADDLE_HEIGHT);
        quad(ball[0], ball[1], BALL_SIZE, BALL_SIZE);
        GL11.glColor4f(0.95f, 0.85f, 0.35f, 1.0f);
        digits(score, 12.0f, 12.0f);
        GL11.glColor4f(0.9f, 0.35f, 0.35f, 1.0f);
        digits(lives, WIDTH - 30.0f, 12.0f);
        if (paused) {
            GL11.glColor4f(0.0f, 0.0f, 0.0f, 0.55f);
            quad(0.0f, 0.0f, WIDTH, HEIGHT);
        }
        GL11.glEnd();
    }

    private void drawBricks() {
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL11.glBegin(GL11.GL_QUADS);
        for (int index = 0; index < bricks.length; index++) {
            if (!bricks[index]) {
                continue;
            }
            int row = index / COLUMNS;
            GL11.glColor4f(0.35f + 0.13f * row, 0.85f - 0.11f * row, 0.45f + 0.09f * row, 1.0f);
            texturedQuad(brickX(index) + 2.0f, brickY(index) + 2.0f,
                    BRICK_WIDTH - 4.0f, BRICK_HEIGHT - 4.0f);
        }
        GL11.glEnd();
    }

    private static void quad(float x, float y, float width, float height) {
        GL11.glVertex2f(x, y);
        GL11.glVertex2f(x, y + height);
        GL11.glVertex2f(x + width, y + height);
        GL11.glVertex2f(x + width, y);
    }

    private static void texturedQuad(float x, float y, float width, float height) {
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2f(x, y);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2f(x, y + height);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2f(x + width, y + height);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2f(x + width, y);
    }

    /** the hud, one quad per lit pixel of the little font above */
    private static void digits(int value, float x, float y) {
        String written = Integer.toString(value);
        for (int at = 0; at < written.length(); at++) {
            int glyph = DIGITS[written.charAt(at) - '0'];
            for (int bit = 0; bit < 15; bit++) {
                if ((glyph & (1 << (14 - bit))) == 0) {
                    continue;
                }
                quad(x + at * 14.0f + (bit % 3) * 4.0f, y + (bit / 3) * 4.0f, 3.0f, 3.0f);
            }
        }
    }

    // the pieces a host has to supply

    private static int buildTexture() {
        int size = 8;
        ByteBuffer pixels = BufferUtils.createByteBuffer(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean edge = x == 0 || y == 0 || x == size - 1 || y == size - 1;
                byte shade = (byte) (edge ? 0x90 : 0xFF);
                pixels.put(shade).put(shade).put((byte) 0xFF).put((byte) 0xFF);
            }
        }
        pixels.flip();
        int name = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, name);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, size, size, 0,
                GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, pixels);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        return name;
    }

    private static int buildSound(float hertz, int millis) {
        int frames = SAMPLE_RATE * millis / 1000;
        ByteBuffer pcm = BufferUtils.createByteBuffer(frames * 2);
        for (int frame = 0; frame < frames; frame++) {
            double fade = 1.0 - (double) frame / frames;
            double wave = Math.sin(2.0 * Math.PI * hertz * frame / SAMPLE_RATE);
            short sample = (short) (wave * fade * Short.MAX_VALUE * 0.4);
            pcm.put((byte) (sample & 0xFF)).put((byte) ((sample >> 8) & 0xFF));
        }
        pcm.flip();

        int buffer = AL10.alGenBuffers();
        AL10.alBufferData(buffer, AL10.AL_FORMAT_MONO16, pcm, SAMPLE_RATE);
        int source = AL10.alGenSources();
        AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
        AL10.alSourcef(source, AL10.AL_GAIN, 0.25f);
        return source;
    }

    private static void play(int source) {
        AL10.alSourceStop(source);
        AL10.alSourcePlay(source);
    }

    private int readHighScore() {
        File saved = new File(SAVE);
        if (!saved.isFile()) {
            return 0;
        }
        try (InputStream in = new FileInputStream(saved)) {
            byte[] read = new byte[16];
            int count = in.read(read);
            return count <= 0 ? 0 : Integer.parseInt(new String(read, 0, count,
                    StandardCharsets.UTF_8).trim());
        } catch (IOException | NumberFormatException unreadable) {
            return 0;
        }
    }

    private void saveHighScore() {
        if (score <= highScore) {
            System.out.println("blocks: high score stands at " + highScore);
            return;
        }
        File home = new File("/flagon");
        if (!home.isDirectory() && !home.mkdir()) {
            System.out.println("blocks: nowhere to save a high score");
            return;
        }
        try (FileOutputStream out = new FileOutputStream(SAVE)) {
            out.write(Integer.toString(score).getBytes(StandardCharsets.UTF_8));
            System.out.println("blocks: saved high score " + score);
        } catch (IOException failed) {
            System.out.println("blocks: could not save a high score: " + failed.getMessage());
        }
    }

    private void release() {
        AL10.alDeleteSources(brickSound);
        AL10.alDeleteSources(wallSound);
        AL10.alDeleteSources(lostSound);
        AL.destroy();
        GL11.glDeleteTextures(texture);
        if (Display.isCreated()) {
            Display.destroy();
        }
    }
}
