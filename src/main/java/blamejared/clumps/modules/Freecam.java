package blamejared.clumps.modules;

import blamejared.clumps.ClumpsModule;
import blamejared.clumps.Option;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class Freecam extends ClumpsModule {

    private final Option.DoubleOption speed = new Option.DoubleOption("Speed", 1.0, 0.1, 10.0, 0.1);
    private final Option.DoubleOption scrollSens = new Option.DoubleOption("Scroll Sensitivity", 0.0, 0.0, 2.0, 0.1);
    private final Option.BoolOption toggleOnDamage = new Option.BoolOption("Toggle On Damage", false);
    private final Option.BoolOption toggleOnDeath = new Option.BoolOption("Toggle On Death", false);
    private final Option.BoolOption showHands = new Option.BoolOption("Show Hands", true);
    private final Option.BoolOption staySneaking = new Option.BoolOption("Stay Sneaking", true);
    private final Option.BoolOption reloadChunks = new Option.BoolOption("Reload Chunks", true);
    private final Option.BoolOption staticView = new Option.BoolOption("Static View", true);

    public Vec3 pos = Vec3.ZERO;
    public Vec3 prevPos = Vec3.ZERO;

    public float yaw, pitch;
    public float lastYaw, lastPitch;

    private boolean initialized = false;
    private CameraType prevPerspective = null;
    private double speedValue;
    private double savedFovScale;
    private boolean savedBobView;
    private boolean wasSneaking;
    private boolean clearedServerInput;

    private boolean forward, backward, right, left, up, down;

    @Override public String getName() { return "Freecam"; }
    @Override public String getDescription() { return "Detach camera from player"; }
    @Override public String getCategory() { return "Movement"; }

    @Override
    public List<Option<?>> getOptions() {
        return Option.list(speed, scrollSens, toggleOnDamage, toggleOnDeath,
                showHands, staySneaking, reloadChunks, staticView);
    }

    public boolean isInitialized() { return initialized; }
    public boolean isToggleOnDamage() { return toggleOnDamage.getValue(); }
    public boolean isToggleOnDeath() { return toggleOnDeath.getValue(); }
    public boolean renderHands() { return !enabled || showHands.getValue(); }
    public boolean staySneaking() { return enabled && staySneaking.getValue() && wasSneaking; }

    public void changeLookDirection(double dx, double dy) {
        lastYaw = yaw;
        lastPitch = pitch;
        yaw += (float) dx;
        pitch += (float) dy;
        pitch = Mth.clamp(pitch, -90f, 90f);
    }

    public void onScroll(double value) {
        if (!enabled || !initialized) return;
        if (scrollSens.getValue() > 0) {
            speedValue += value * 0.25 * (scrollSens.getValue() * speedValue);
            if (speedValue < 0.1) speedValue = 0.1;
        }
    }

    public double getX(float t) { return Mth.lerp(t, prevPos.x, pos.x); }
    public double getY(float t) { return Mth.lerp(t, prevPos.y, pos.y); }
    public double getZ(float t) { return Mth.lerp(t, prevPos.z, pos.z); }
    public float getYaw(float t) { return Mth.lerp(t, lastYaw, yaw); }
    public float getPitch(float t) { return Mth.lerp(t, lastPitch, pitch); }

    public boolean onKeyInput(Minecraft client, int key, boolean pressed) {
        // Not used — movement is captured via GLFW polling in onTick
        return false;
    }

    @Override
    public void onTick(Minecraft client) {
        if (client.player == null || client.level == null) return;

        if (!initialized) {
            savedFovScale = client.options.fovEffectScale().get();
            savedBobView = client.options.bobView().get();
            if (staticView.getValue()) {
                client.options.fovEffectScale().set(0.0);
                client.options.bobView().set(false);
            }

            yaw = client.player.getYRot();
            pitch = client.player.getXRot();
            prevPerspective = client.options.getCameraType();
            speedValue = speed.getValue();

            pos = client.gameRenderer.getMainCamera().position();
            prevPos = pos;

            if (client.options.getCameraType() == CameraType.THIRD_PERSON_FRONT) {
                yaw += 180;
                pitch *= -1;
            }

            lastYaw = yaw;
            lastPitch = pitch;
            wasSneaking = client.options.keyShift.isDown();

            forward = client.options.keyUp.isDown();
            backward = client.options.keyDown.isDown();
            right = client.options.keyRight.isDown();
            left = client.options.keyLeft.isDown();
            up = client.options.keyJump.isDown();
            down = client.options.keyShift.isDown();

            unpress(client);
            client.player.setDeltaMovement(Vec3.ZERO);
            if (reloadChunks.getValue()) client.levelRenderer.allChanged();
            initialized = true;
            clearedServerInput = false;
        }

        if (!clearedServerInput) {
            client.player.connection.send(new ServerboundPlayerInputPacket(client.player.input.keyPresses));
            client.player.setDeltaMovement(Vec3.ZERO);
            clearedServerInput = true;
        }

        if (client.screen != null) {
            unpress(client);
            prevPos = pos;
            lastYaw = yaw;
            lastPitch = pitch;
            return;
        }

        if (!prevPerspective.isFirstPerson())
            client.options.setCameraType(CameraType.FIRST_PERSON);

        // Poll movement keys via GLFW
        long window = client.getWindow().handle();
        forward  = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        backward = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        left     = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        right    = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        up       = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        down     = GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;

        // Forward/right vectors from yaw
        float yawRad = yaw * 0.017453292f;
        Vec3 fwd = new Vec3(-Mth.sin(yawRad), 0, Mth.cos(yawRad));
        float yawRad90 = (yaw + 90) * 0.017453292f;
        Vec3 rt = new Vec3(-Mth.sin(yawRad90), 0, Mth.cos(yawRad90));
        double vx = 0, vy = 0, vz = 0;

        double s = GLFW.glfwGetKey(client.getWindow().handle(), GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ? 1.0 : 0.5;

        boolean a = false, b = false;
        if (forward)  { vx += fwd.x * s * speedValue; vz += fwd.z * s * speedValue; a = true; }
        if (backward) { vx -= fwd.x * s * speedValue; vz -= fwd.z * s * speedValue; a = true; }
        if (right)    { vx += rt.x * s * speedValue;  vz += rt.z * s * speedValue;  b = true; }
        if (left)     { vx -= rt.x * s * speedValue;  vz -= rt.z * s * speedValue;  b = true; }

        if (a && b) { double d = 1.0 / Math.sqrt(2); vx *= d; vz *= d; }

        if (up)   vy += s * speedValue;
        if (down) vy -= s * speedValue;

        prevPos = pos;
        pos = pos.add(vx, vy, vz);
    }

    private void unpress(Minecraft client) {
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
    }

    public void reset() {
        if (!initialized) return;
        Minecraft client = Minecraft.getInstance();

        if (reloadChunks.getValue())
            client.execute(() -> client.levelRenderer.allChanged());

        if (prevPerspective != null) {
            client.options.setCameraType(prevPerspective);
            prevPerspective = null;
        }

        if (staticView.getValue()) {
            client.options.fovEffectScale().set(savedFovScale);
            client.options.bobView().set(savedBobView);
        }

        wasSneaking = false;
        clearedServerInput = false;
        forward = backward = right = left = up = down = false;
        initialized = false;
        pos = Vec3.ZERO;
        prevPos = Vec3.ZERO;
    }
}
