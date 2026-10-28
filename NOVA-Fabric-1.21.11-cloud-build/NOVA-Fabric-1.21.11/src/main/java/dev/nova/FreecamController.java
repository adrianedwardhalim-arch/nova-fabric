package dev.nova;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;

public final class FreecamController {
    private static boolean enabled = false;
    private static ArmorStandEntity cameraEntity;
    private static Vec3d playerAnchor;

    private FreecamController() {
    }

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(FreecamController::tick);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle(MinecraftClient client) {
        if (enabled) {
            disable(client);
        } else {
            enable(client);
        }
    }

    private static void enable(MinecraftClient client) {
        if (client.player == null || client.world == null) {
            return;
        }

        ArmorStandEntity camera = EntityType.ARMOR_STAND.create(client.world);
        if (camera == null) {
            return;
        }

        playerAnchor = client.player.getPos();
        camera.setInvisible(true);
        camera.setNoGravity(true);
        camera.setPosition(client.player.getX(), client.player.getY(),
                client.player.getZ());
        camera.setYaw(client.player.getYaw());
        camera.setPitch(client.player.getPitch());

        cameraEntity = camera;
        enabled = true;
        client.setCameraEntity(cameraEntity);
    }

    private static void disable(MinecraftClient client) {
        if (client.player != null) {
            if (playerAnchor != null) {
                client.player.setPosition(playerAnchor.x, playerAnchor.y,
                        playerAnchor.z);
            }
            client.player.setVelocity(Vec3d.ZERO);
            client.setCameraEntity(client.player);
        }

        cameraEntity = null;
        playerAnchor = null;
        enabled = false;
    }

    private static void tick(MinecraftClient client) {
        if (!enabled) {
            return;
        }

        if (client.player == null || client.world == null
                || cameraEntity == null || playerAnchor == null) {
            disable(client);
            return;
        }

        var player = client.player;
        player.setPosition(playerAnchor.x, playerAnchor.y, playerAnchor.z);
        player.setVelocity(Vec3d.ZERO);

        cameraEntity.setYaw(player.getYaw());
        cameraEntity.setPitch(player.getPitch());

        double yaw = Math.toRadians(player.getYaw());
        double forwardX = -Math.sin(yaw);
        double forwardZ = Math.cos(yaw);
        double rightX = Math.cos(yaw);
        double rightZ = Math.sin(yaw);

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        if (client.options.forwardKey.isPressed()) {
            x += forwardX;
            z += forwardZ;
        }
        if (client.options.backKey.isPressed()) {
            x -= forwardX;
            z -= forwardZ;
        }
        if (client.options.rightKey.isPressed()) {
            x += rightX;
            z += rightZ;
        }
        if (client.options.leftKey.isPressed()) {
            x -= rightX;
            z -= rightZ;
        }
        if (client.options.jumpKey.isPressed()) {
            y += 1.0;
        }
        if (client.options.sneakKey.isPressed()) {
            y -= 1.0;
        }

        double length = Math.sqrt(x * x + y * y + z * z);
        if (length > 0.0) {
            double speed = client.options.sprintKey.isPressed()
                    ? 0.9 : 0.35;
            x = x / length * speed;
            y = y / length * speed;
            z = z / length * speed;
            cameraEntity.setPosition(
                    cameraEntity.getX() + x,
                    cameraEntity.getY() + y,
                    cameraEntity.getZ() + z
            );
        }
    }
}
