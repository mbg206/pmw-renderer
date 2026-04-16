package me.mbg206.pmwrenderer.renderer;

import dev.protomanly.pmweather.PMWeather;
import dev.protomanly.pmweather.config.ClientConfig;
import dev.protomanly.pmweather.config.ServerConfig;
import dev.protomanly.pmweather.event.GameBusClientEvents;
import dev.protomanly.pmweather.particle.EntityRotFX;
import dev.protomanly.pmweather.weather.Storm;
import dev.protomanly.pmweather.weather.ThermodynamicEngine;
import dev.protomanly.pmweather.weather.WeatherHandlerClient;
import dev.protomanly.pmweather.weather.storms.StormTypes;
import me.mbg206.pmwrenderer.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class ParticleManager {
    private static final int QUALITY = 50;

    private final List<Storm> supercells = new ArrayList<>();
    private final Map<Storm, List<EntityRotFX>> supercellParticles = new HashMap<>();
    private final int mapSize;
    private final ClientLevel level;

    private float playerPrecip = 0F;
    private float prevPlayerPrecip = 0F;
    private ThermodynamicEngine.Precipitation precipType;

    private int ticksSinceUpdate = 0;

    public ParticleManager(ClientLevel level) {
        this.level = level;

        this.mapSize = (2*ClientConfig.maxParticleSpawnDistanceFromPlayer / QUALITY * 2);
    }

    public void tick() {
        if (ticksSinceUpdate >= Config.updateEveryTicks) {
            Player player = Minecraft.getInstance().player;
            if (GameBusClientEvents.weatherHandler == null || player == null) {
                return;
            }

            WeatherHandlerClient handler = (WeatherHandlerClient) GameBusClientEvents.weatherHandler;
            Vec3 playerPos = player.position();

            if (Config.renderMode == Config.RenderMode.CLASSIC) {
                spawnNewClouds(handler, playerPos);
            }

            prevPlayerPrecip = playerPrecip;
            playerPrecip = fixPrecip(handler.getPrecipitation());
            precipType = ThermodynamicEngine.getPrecipitationType(handler, playerPos, level, 0);

            ticksSinceUpdate = 0;
        }
        else for (Storm storm : supercells) {
            for (EntityRotFX particle : supercellParticles.get(storm)) {
                updateTornadoParticle(storm, particle);
            }
        }


        ticksSinceUpdate++;
    }

    public float getPlayerPrecip(float partialTick) {
        return Mth.lerp((ticksSinceUpdate + partialTick) / Config.updateEveryTicks, prevPlayerPrecip, playerPrecip);
    }

    public float getFog(float partialTick) {
        float precip = getPlayerPrecip(partialTick);
        boolean isSnow = precipType == ThermodynamicEngine.Precipitation.WINTRY_MIX || precipType == ThermodynamicEngine.Precipitation.SNOW;
        float fog;
        if (isSnow) {
            fog = Mth.sqrt(precip);
        }
        else {
            fog = (precip * precip) / 2;
        }
        return Math.min(fog, 1F);
    }

    public float getSkyDarken(float partialTick) {
        return getPlayerPrecip(partialTick) * 0.7F;
    }


    private Vec3 getWorldPos(Vec3 playerPosition, int x, int z) {
        return new Vec3(
                (playerPosition.x - 2*ClientConfig.maxParticleSpawnDistanceFromPlayer) + (QUALITY * x),
                80,
                (playerPosition.z - 2*ClientConfig.maxParticleSpawnDistanceFromPlayer) + (QUALITY * z));
    }


    private static float fixPrecip(float precip) {
        return Math.min(1, precip * 3);
    }
    private static float getBrightness(float fixedPrecip) {
        return 1.0F - (fixedPrecip * 0.8F);
    }

    private void spawnNewClouds(WeatherHandlerClient handler, Vec3 playerPosition) {
        Vec3 baseCloudColor = ClassicRenderer.getCloudColor();

        for (int x = 0; x < mapSize; x++) for (int z = 0; z < mapSize; z++) {
            Vec3 pos = getWorldPos(playerPosition, x, z);

            float cloudDensity = ClassicRenderer.getBasicCloudDensity(handler, (float) pos.x, (float) pos.z);
            float precip = 0F;
            float maxPrecip = 0F;
            double vx = -0.25F;
            double vz = -0.25F;

            if (cloudDensity > 0.15F) {
                precip += (cloudDensity - 0.15F) * 2.0F;
            }

            for(Storm storm : handler.getStorms()) {
                if (!storm.visualOnly && storm.hasPrecipitation()) {
                    float stormPrecip = storm.getPrecipitation(pos);

                    if (storm.is(StormTypes.SUPERCELL) && storm.stage == 3) {
                        double dx = pos.x() - storm.position.x();
                        double dz = pos.z() - storm.position.z();
                        float distance = (float) Math.sqrt(dx * dx + dz * dz);
                        if (distance < storm.width * 32) {
                            cloudDensity = 1F;
                        }
                    }

                    if (stormPrecip > maxPrecip) {
                        maxPrecip = stormPrecip;
                        vx = storm.velocity.x() * 0.05; // n / 20
                        vz = storm.velocity.z() * 0.05;
                    }
                    precip = storm.addToPrecip(precip, stormPrecip);
                }
            }


            precip = Math.clamp(precip * (float)ServerConfig.rainStrength, 0.0F, 1.0F);
            precip = fixPrecip(precip);

            cloudDensity = Math.max(precip, cloudDensity);

            float brightness = getBrightness(precip);
            Vec3 cloudColor = baseCloudColor.multiply(brightness, brightness, brightness);

            if (cloudDensity > (PMWeather.RANDOM.nextFloat() * 0.15F)) {
                spawnCloud(pos.x, ServerConfig.layer0Height, pos.z, vx, vz, cloudColor);
                if (maxPrecip > 0F && maxPrecip < 0.3F) {
                    spawnCloud(pos.x, ServerConfig.layer0Height, pos.z, -0.25, -0.25, baseCloudColor);
                }
            }

        }

        Set<Storm> newSupercells = new HashSet<>();
        for (Storm storm : handler.getStorms()) {
            if (!storm.is(StormTypes.SUPERCELL)) {
                continue;
            }
            if (storm.stage != 3) {
                continue;
            }

            newSupercells.add(storm);
            if (!supercells.contains(storm)) {
                supercells.add(storm);
                supercellParticles.put(storm, new ArrayList<>());
            }


            //float brightness = getBrightness(Math.clamp((storm.smoothWindspeed - 100) / 60, 0F, 1F));
            float brightness = getBrightness(0.9F);
            Vec3 cloudColor = baseCloudColor.multiply(brightness, brightness, brightness);
            spawnTornadoParticles(storm, cloudColor);
        }

        Iterator<Storm> it = supercells.iterator();
        while (it.hasNext()) {
            Storm supercell = it.next();
            if (!newSupercells.contains(supercell)) {
                supercellParticles.remove(supercell);
                it.remove();
            }

            else {
                supercellParticles.get(supercell).removeIf(particle -> !particle.isAlive());
            }
        }
    }

    private EntityRotFX createParticle(double x, double y, double z) {
        EntityRotFX cloud = new EntityRotFX(level, x, y, z, 0, 0, 0);
        cloud.setMotionX(0);
        cloud.setMotionY(0);
        cloud.setMotionZ(0);
        //cloud.setAlpha(0.9F);
        cloud.setSprite(ParticleRegistry.cloudTexture);
        cloud.setLifetime(Config.updateEveryTicks + Config.particleCrossover);
        cloud.ignoreWind = true;
        cloud.useCustomBBForRenderCulling = true;
        cloud.renderRange = (float) (ClientConfig.maxParticleSpawnDistanceFromPlayer * 2);
        cloud.renderType = EntityRotFX.SORTED_TRANSLUCENT;
        cloud.setScale(50);
        cloud.setCanCollide(false);
        cloud.facePlayer = false;
        cloud.vanillaMotionDampen = false;
        cloud.setGravity(0.F);
        cloud.ticksFadeInMax = 40;
        cloud.ticksFadeOutMax = 40;
        cloud.tick();

        cloud.bbRender = cloud.bbRender.inflate(32);

        return cloud;
    }

    public void spawnCloud(double x, double y, double z, double vx, double vz, Vec3 color) {
        x += PMWeather.RANDOM.nextDouble(20.0);
        z += PMWeather.RANDOM.nextDouble(20.0);
        EntityRotFX cloud = createParticle(x, y, z);
        cloud.setMotionX(vx);
        cloud.setMotionZ(vz);

        cloud.rotationPitch = cloud.prevRotationPitch = 70F + PMWeather.RANDOM.nextFloat(40F);
        cloud.rotationYaw = cloud.prevRotationYaw = -20F + PMWeather.RANDOM.nextFloat(40F);
        cloud.rotationRoll = -20F + PMWeather.RANDOM.nextFloat(40F);

        cloud.setColor((float) color.x(), (float) color.y(), (float) color.z());

        cloud.spawnAsWeatherEffect();
    }

    public void spawnTornadoParticles(Storm storm, Vec3 cloudColor) {
        List<EntityRotFX> clouds = supercellParticles.get(storm);
        Vec3 pos = storm.position;

        float downPercent = Math.min(1F, storm.smoothWindspeed / storm.touchdownSpeed);

        // supercell rotation

        //int n = (int) (storm.width / 2) + 10;
        int n = (int) (125 * downPercent) + 2;
        for (int i = 0; i < n; i++) {
            EntityRotFX cloud = createParticle(pos.x, ServerConfig.layer0Height - 10, pos.z);
            cloud.rotationPitch = cloud.prevRotationPitch = 70F + PMWeather.RANDOM.nextFloat(40F);
            cloud.rotationYaw = cloud.prevRotationYaw = (float) i / n * 360;
            cloud.spinFastRate = PMWeather.RANDOM.nextFloat(storm.width * 3F * downPercent) + 10;
            //cloud.spinFastRate = PMWeather.RANDOM.nextFloat(300 * downPercent) + 10;
            cloud.setColor((float) cloudColor.x, (float) cloudColor.y, (float) cloudColor.z);
            updateTornadoParticle(storm, cloud);

            cloud.spawnAsWeatherEffect();
            clouds.add(cloud);
        }

        // funnel

        double maxY = ServerConfig.layer0Height;
        double y = Mth.lerp(
                downPercent,
                maxY,
                //Heightmap.Types.MOTION_BLOCKING_NO_LEAVES
                level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, (int) pos.x, (int) pos.z)
        );

        float size = storm.width / 8;
        float randomness = 0F;
        Vec3 funnelColor = cloudColor.multiply(0.85, 0.85, 0.85);
        do {
            int particlesInRow = Math.max(3, (int) size / 16); // n / 10
            for (int i = 0; i < particlesInRow; i++) {
                EntityRotFX cloud = createParticle(pos.x, y, pos.z);
                cloud.rotationPitch = cloud.prevRotationPitch = 30F;
                cloud.rotationYaw = cloud.prevRotationYaw = (float) i / particlesInRow * 360;
                cloud.spinFastRate = Mth.lerp(randomness, size, PMWeather.RANDOM.nextFloat(size));
                //cloud.setColor(0.4f, 0.4f, 0.4f);
                cloud.setColor((float) funnelColor.x(), (float) funnelColor.y(), (float) funnelColor.z());
                updateTornadoParticle(storm, cloud);

                cloud.spawnAsWeatherEffect();
                clouds.add(cloud);
                if (randomness < 1.0F) {
                    randomness += 0.05F;
                }
            }
            //size *= 1.3F;
            size += (storm.width / 10) + 5;
            y += 20;

        } while (y < maxY);
    }

    // ticked
    public void updateTornadoParticle(Storm storm, EntityRotFX particle) {
        Vec3 pos = storm.position;
        particle.rotationYaw += 20/Math.max(1, particle.spinFastRate) + 1;
        double rollRad = particle.rotationYaw * (Math.PI / 180);
        particle.setPos(
                pos.x + (Math.sin(rollRad) * particle.spinFastRate),
                particle.getY(),
                pos.z + (Math.cos(rollRad) * particle.spinFastRate));
    }


    public void stop() {
        supercells.clear();
        supercellParticles.clear();
    }

    public void start() {
        ticksSinceUpdate = Config.updateEveryTicks - 10;
    }
}
