import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/**
 * Убирает из module-info.class указанные `requires` и `provides` (по типу реализации или по имени сервиса).
 *
 * Usage: ModuleRequiresFix <in.jar> <out.jar> <entry> <moduleToDrop>[,<moduleToDrop>...]
 *
 * Зачем: в урезанном JRE из jdk4py нет модуля jdk.dynalink, а nashorn-core его требует. Форge-овский
 * coremod при этом требует сам nashorn, поэтому выкинуть nashorn нельзя — проще снять с него требование
 * несуществующего модуля (JS-coremods у нас всё равно выключены, классы dynalink не загружаются).
 */
public class ModuleRequiresFix {

    public static void main(String[] args) throws Exception {
        Path in = Path.of(args[0]), out = Path.of(args[1]);
        String entry = args[2];
        Set<String> drop = new HashSet<>(Arrays.asList(args[3].split(",")));

        try (ZipInputStream zin = new ZipInputStream(Files.newInputStream(in));
             ZipOutputStream zout = new ZipOutputStream(Files.newOutputStream(out))) {
            ZipEntry e;
            while ((e = zin.getNextEntry()) != null) {
                byte[] data = zin.readAllBytes();
                if (e.getName().equals(entry)) {
                    ClassReader cr = new ClassReader(data);
                    ClassWriter cw = new ClassWriter(0);
                    cr.accept(new ClassVisitor(Opcodes.ASM9, cw) {
                        @Override
                        public ModuleVisitor visitModule(String name, int access, String version) {
                            ModuleVisitor mv = super.visitModule(name, access, version);
                            return new ModuleVisitor(Opcodes.ASM9, mv) {
                                @Override
                                public void visitRequire(String module, int acc, String ver) {
                                    if (!drop.contains(module)) super.visitRequire(module, acc, ver);
                                }

                                @Override
                                public void visitProvide(String service, String... providers) {
                                    // сервис/реализация из выкинутого модуля ломает резолв («does not read a module»)
                                    if (drop.stream().anyMatch(d -> service.startsWith(d.replace('.', '/'))
                                            || service.startsWith(d))) return;
                                    for (String p : providers) {
                                        if (drop.stream().anyMatch(d -> p.startsWith(d.replace('.', '/'))
                                                || p.startsWith(d))) return;
                                    }
                                    super.visitProvide(service, providers);
                                }
                            };
                        }
                    }, 0);
                    data = cw.toByteArray();
                }
                zout.putNextEntry(new ZipEntry(e.getName()));
                zout.write(data);
                zout.closeEntry();
            }
        }
        System.out.println("готово: " + out);
    }
}
