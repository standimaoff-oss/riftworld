import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Applies a Forge access-transformer config to a jar.
 *
 * Usage: AtApply <in.jar> <out.jar> <accesstransformer.cfg> [member-map.tsv]
 *
 * The cfg names classes in the official (mojmap) namespace but members by SRG name, while the jar we transform
 * already carries mojmap member names — member-map.tsv (`srg<TAB>mojmap`) bridges the two.
 */
public class AtApply {

    record Rule(String modifier, String owner, String member, String desc) {}

    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]), out = Path.of(args[1]), cfg = Path.of(args[2]);
        Map<String, String> memberMap = new HashMap<>();                       // "owner#srg" -> mojmap
        if (args.length > 3) {
            for (String line : Files.readAllLines(Path.of(args[3]), StandardCharsets.UTF_8)) {
                String[] p = line.split("\t");
                if (p.length == 3) memberMap.put(p[0] + "#" + p[1], p[2]);
            }
        }

        Map<String, List<Rule>> byOwner = new HashMap<>();
        int lines = 0, skipped = 0;
        for (String raw : Files.readAllLines(cfg, StandardCharsets.UTF_8)) {
            String line = raw;
            int hash = line.indexOf('#');
            if (hash >= 0) line = line.substring(0, hash);                     // в cfg хвостовые комментарии
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] p = line.split("\\s+");
            if (p.length < 2) { skipped++; continue; }
            String modifier = p[0];
            String owner = p[1].replace('.', '/');
            String member = p.length > 2 ? p[2] : null;
            String desc = p.length > 3 ? p[3] : null;
            if (member != null && member.indexOf('(') >= 0) {                 // AT склеивает имя и дескриптор
                int paren = member.indexOf('(');
                desc = member.substring(paren);
                member = member.substring(0, paren);
            }
            if (member != null) {
                if (member.contains("*")) { skipped++; continue; }             // wildcard ATs не нужны для компиляции
                member = memberMap.getOrDefault(owner + "#" + member, member); // cfg: srg-имена, jar: mojmap
                member = memberMap.getOrDefault("*#" + member, member);
            }
            byOwner.computeIfAbsent(owner, k -> new ArrayList<>()).add(new Rule(modifier, owner, member, desc));
            lines++;
        }

        int touchedClasses = 0, touchedMembers = 0;
        Map<String, Integer> missed = new LinkedHashMap<>();
        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                String name = e.getName();
                if (name.endsWith(".class")) {
                    String owner = name.substring(0, name.length() - 6);
                    List<Rule> rules = byOwner.get(owner);
                    if (rules != null) {
                        ClassNode cn = new ClassNode();
                        new ClassReader(data).accept(cn, 0);
                        for (Rule r : rules) {
                            if (r.member() == null) {
                                int before = cn.access;
                                cn.access = apply(cn.access, r.modifier());
                                if (before != cn.access) touchedClasses++;
                                continue;
                            }
                            int hits = 0;
                            for (MethodNode m : cn.methods) {
                                if (!m.name.equals(r.member())) continue;
                                if (r.desc() != null && !m.desc.equals(r.desc())) continue;
                                m.access = apply(m.access, r.modifier());
                                touchedMembers++; hits++;
                            }
                            for (FieldNode f : cn.fields) {
                                if (!f.name.equals(r.member())) continue;
                                f.access = apply(f.access, r.modifier());
                                touchedMembers++; hits++;
                            }
                            if (hits == 0) missed.merge(owner + " " + r.member() + (r.desc() == null ? "" : " " + r.desc()), 1, Integer::sum);
                        }
                        // ECJ/javac читают видимость вложенного класса из InnerClasses, а не из своего class- access
                        for (InnerClassNode icn : cn.innerClasses) {
                            List<Rule> inner = byOwner.get(icn.name);
                            if (inner == null) continue;
                            for (Rule r : inner) {
                                if (r.member() != null) continue;
                                int before = icn.access;
                                icn.access = apply(icn.access, r.modifier());
                                if (before != icn.access) touchedClasses++;
                            }
                        }
                        ClassWriter cw = new ClassWriter(0);
                        cn.accept(cw);
                        data = cw.toByteArray();
                    }
                }
                zout.putNextEntry(new ZipEntry(name));
                zout.write(data);
                zout.closeEntry();
            }
        }
        System.out.printf("AT: rules=%d skipped=%d classes=%d members=%d missed=%d%n",
                lines, skipped, touchedClasses, touchedMembers, missed.size());
        missed.keySet().stream().limit(6).forEach(k -> System.out.println("  MISS " + k));
    }

    static int apply(int access, String modifier) {
        int mods = access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
        boolean removeFinal = modifier.endsWith("-f");
        boolean addFinal = modifier.endsWith("+f");
        String base = modifier.replaceAll("[+-]f$", "");
        mods |= switch (base) {
            case "public" -> Opcodes.ACC_PUBLIC;
            case "protected" -> Opcodes.ACC_PROTECTED;
            case "private" -> Opcodes.ACC_PRIVATE;
            default -> 0;                                                    // "default" — package-private
        };
        if (removeFinal) mods &= ~Opcodes.ACC_FINAL;
        if (addFinal) mods |= Opcodes.ACC_FINAL;
        return mods;
    }
}
