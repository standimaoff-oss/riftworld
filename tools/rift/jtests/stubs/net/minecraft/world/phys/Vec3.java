package net.minecraft.world.phys;

/**
 * Test-only stub of Minecraft's Vec3 (same maths as the real immutable vector). Not shipped with the mod.
 */
public class Vec3 {
    public static final Vec3 ZERO = new Vec3(0, 0, 0);

    public final double x, y, z;

    public Vec3(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vec3 add(Vec3 o) { return new Vec3(x + o.x, y + o.y, z + o.z); }
    public Vec3 add(double dx, double dy, double dz) { return new Vec3(x + dx, y + dy, z + dz); }
    public Vec3 subtract(Vec3 o) { return new Vec3(x - o.x, y - o.y, z - o.z); }
    public Vec3 subtract(double dx, double dy, double dz) { return new Vec3(x - dx, y - dy, z - dz); }
    public Vec3 scale(double s) { return new Vec3(x * s, y * s, z * s); }
    public Vec3 multiply(double sx, double sy, double sz) { return new Vec3(x * sx, y * sy, z * sz); }
    public double dot(Vec3 o) { return x * o.x + y * o.y + z * o.z; }
    public Vec3 cross(Vec3 o) { return new Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x); }
    public double lengthSqr() { return x * x + y * y + z * z; }
    public double length() { return Math.sqrt(lengthSqr()); }
    public double distanceTo(Vec3 o) { return Math.sqrt(distanceToSqr(o)); }
    public double distanceToSqr(Vec3 o) {
        double dx = x - o.x, dy = y - o.y, dz = z - o.z;
        return dx * dx + dy * dy + dz * dz;
    }
    public Vec3 normalize() {
        double l = length();
        return l < 1.0E-4 ? ZERO : new Vec3(x / l, y / l, z / l);
    }
    public Vec3 lerp(Vec3 to, double t) {
        return new Vec3(x + (to.x - x) * t, y + (to.y - y) * t, z + (to.z - z) * t);
    }
    public Vec3 horizontal() { return new Vec3(x, 0, z); }

    @Override
    public String toString() { return "(" + x + ", " + y + ", " + z + ")"; }
}
