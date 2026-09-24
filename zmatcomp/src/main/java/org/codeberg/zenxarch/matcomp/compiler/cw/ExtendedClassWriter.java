package org.codeberg.zenxarch.matcomp.compiler.cw;

import static org.objectweb.asm.Opcodes.*;

import com.google.common.io.Files;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMaps;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.codeberg.zenxarch.matcomp.MaterialRuleCompilerConstants;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.commons.InstructionAdapter;

public abstract class ExtendedClassWriter {
  public final String classname;
  public final ClassWriter cw;

  public ExtendedClassWriter(String classname) {
    this.classname = classname;
    this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
  }

  public static final boolean dumpClasses =
      Boolean.getBoolean("org.codeberg.zenxarch.zmatcomp.dumpClasses");
  private static final Path dumpDir =
      FabricLoader.getInstance().getGameDir().resolve("./cache/zmatcomp/classes");

  static {
    if (dumpClasses) {
      try {
        var f = dumpDir.toFile();
        if (f.exists()) {
          for (var file : f.listFiles()) {
            if (file.getName().endsWith(".class")) file.delete();
          }
        }
      } catch (Exception e) {
        MaterialRuleCompilerConstants.LOGGER.error("Failed to clean class dump", e);
      }
    }
  }

  private static void dumpClass(String classname, byte[] bytes) {
    var file = dumpDir.resolve(classname + ".class").toFile();
    file.getParentFile().mkdirs();
    try {
      Files.write(bytes, file);
    } catch (Exception e) {
      MaterialRuleCompilerConstants.LOGGER.error("Failed to dump " + classname, e);
    }
  }

  public void start() {
    cw.visit(V25, ACC_PUBLIC | ACC_STATIC | ACC_SUPER, classname, null, "java/lang/Object", null);

    var mv = new InstructionAdapter(cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null));
    mv.visitCode();
    var l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.invokespecial("java/lang/Object", "<init>", "()V", false);
    mv.visitInsn(RETURN);

    var l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "L" + classname + ";", null, l0, l1, 0);

    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  public Class<?> finish() {
    cw.visitEnd();
    var bytes = cw.toByteArray();

    if (dumpClasses) dumpClass(classname, bytes);

    return defineClass(classname, bytes);
  }

  public void addStaticFinalField(String name, String desc, String signature) {
    cw.visitField(ACC_PUBLIC | ACC_FINAL | ACC_STATIC, name, desc, signature, null).visitEnd();
  }

  public InstructionAdapter beginStaticInit() {
    var mv = new InstructionAdapter(cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null));
    mv.visitCode();
    return mv;
  }

  // load the value before it
  public void initStaticField(InstructionAdapter mv, String name, String desc) {
    mv.putstatic(classname, name, desc);
  }

  public void finishStaticInit(InstructionAdapter mv) {
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public InstructionAdapter newMethod(String name, String desc, String sign) {
    return new InstructionAdapter(cw.visitMethod(ACC_PUBLIC | ACC_STATIC, name, desc, sign, null));
  }

  public Label startMethodVisit(InstructionAdapter mv) {
    mv.visitCode();
    var l0 = new Label();
    mv.visitLabel(l0);
    return l0;
  }

  public void endMethodVisit(InstructionAdapter mv) {
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public void loadIntVar(InstructionAdapter mv, int index) {
    mv.visitVarInsn(ILOAD, index);
  }

  public void loadObjVar(InstructionAdapter mv, int index) {
    mv.visitVarInsn(ALOAD, index);
  }

  private static final Object2ObjectMap<String, byte[]> clazzes =
      Object2ObjectMaps.synchronize(new Object2ObjectOpenHashMap<>());

  private static final ClassLoader clazzLoader =
      new ClassLoader(ExtendedClassWriter.class.getClassLoader()) {
        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
          synchronized (this.getClassLoadingLock(name)) {
            var c = this.findLoadedClass(name);
            if (c != null) return c;
            if (clazzes.containsKey(name)) {
              var bytes = clazzes.get(name);
              return super.defineClass(name, bytes, 0, bytes.length);
            }

            return super.loadClass(name);
          }
        }
      };

  public static Class<?> defineClass(String className, byte[] bytes) {
    clazzes.put(className, bytes);

    try {
      return clazzLoader.loadClass(className);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
