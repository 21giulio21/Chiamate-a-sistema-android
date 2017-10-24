
/*
* Software sviluppato da Giulio Tavella.
* Importante inserire dentro al gragle:
*     compile 'com.github.lanchon.dexpatcher:multidexlib2:2.2.1.r2'

* */




import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcode;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.builder.MutableMethodImplementation;
import org.jf.dexlib2.builder.instruction.BuilderInstruction35c;
import org.jf.dexlib2.iface.ClassDef;
import org.jf.dexlib2.iface.DexFile;
import org.jf.dexlib2.iface.Method;
import org.jf.dexlib2.iface.MethodImplementation;
import org.jf.dexlib2.iface.instruction.Instruction;
import org.jf.dexlib2.iface.instruction.formats.Instruction35c;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.immutable.ImmutableClassDef;
import org.jf.dexlib2.immutable.ImmutableMethod;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.lang.System.out;


public class Main {

    // NON HO CAPITO BENE QUESTA SINTAssi, MA COMUNQUE I NOMI DELLE CLASSI SONO QUESTI (SPERO)
    // com/android|android|com/google|javax?|dalvik|org/apache
    private static String  identificativiChiamateASistema [] = {"com/android","com/google","dalvik","org/apache"};
    public static int  nRedirected = 0;
    public static int nRemoved = 0;
    public int nNotRedirected = 0;


    public static void main(String [] args) throws IOException {
        File inputFile = new File("/home/giuloubuntu/Scrivania/Semplice App/app-debug.apk");

        DexFile dexFile = DexFileFactory.loadDexFile(inputFile, Opcodes.forApi(19));
        for (ClassDef classDef : dexFile.getClasses()) {
            //out.println(classDef + "\n");
            ClassDef d = customizeClass(classDef);

        }
    }

    private static ClassDef customizeClass(ClassDef classDef) {
        List<Method> methods = new ArrayList<>();
        boolean modifiedMethod = false;
        for (Method method : classDef.getMethods()) {
//            out.println("DIO->" + method.getName());
            MethodImplementation implementation = method.getImplementation();
            if (implementation == null) {
                //out.println("METODO1 : " + method.getName());
                methods.add(method);
                continue;
            }
            MethodImplementation customImpl = searchAndReplaceInvocations(implementation);
            if (customImpl==implementation) {
                //out.println("METODO2 : " + method.getName());

                methods.add(method);
                continue;
            }
            modifiedMethod = true;
            final ImmutableMethod newMethod = new ImmutableMethod(method.getDefiningClass(),
                    method.getName(),
                    method.getParameters(),
                    method.getReturnType(),
                    method.getAccessFlags(),
                    method.getAnnotations(),
                    customImpl);
            //out.println("METODO3 : " + newMethod.getName());

            methods.add(newMethod);
        }
        if (!modifiedMethod)
            return classDef;
        return new ImmutableClassDef(classDef.getType(),
                classDef.getAccessFlags(),
                classDef.getSuperclass(),
                classDef.getInterfaces(),
                classDef.getSourceFile(),
                classDef.getAnnotations(),
                classDef.getFields(),
                methods);
    }


    /*
    * Questa funzione prende tutti metodi utilizzati e controllo se sono
    * chiamate a sistema, se sono chiamate a sistema allora appartengono a questa classe
    * com/android|android|com/google|javax?|dalvik|org/apache
    *
    * */

    private static MethodImplementation searchAndReplaceInvocations(MethodImplementation origImplementation) {
        MutableMethodImplementation newImplementation = null;
        int i = -1;
        for (Instruction instruction : origImplementation.getInstructions()) {
            ++i;
            if (instruction.getOpcode() == Opcode.INVOKE_VIRTUAL) {
                Instruction35c i35c = (Instruction35c) instruction;

                String metodoConRispettivaClasse = ((Instruction35c) instruction).getReference().toString();
                for (String path: identificativiChiamateASistema) {
                    if (metodoConRispettivaClasse.contains(path))
                    {
                        out.println("CHIAMATA A SISTEMA ->" + metodoConRispettivaClasse);


                    }

                }






                /*
                * Qui secondo me basta controllare il package di appartenenza e capisco se è
                * uno fra quelli li allora è una è molto probabile sia una chiamata a sistema
                * private static final Pattern validApiClass = Pattern.compile("^L(com/android|android|com/google|javax?|dalvik|org/apache)/.*$");
                *
                *
                *
                *
                *
                *
                *
                *
                * */






                // /Instruction35c newInstruction = checkInstruction(i35c);
                Instruction35c newInstruction = i35c;
                if (newInstruction == i35c)
                    continue;
                if (newImplementation == null) // trick for memory saving
                    newImplementation = new MutableMethodImplementation(origImplementation);
                if (newInstruction == null) {
                    ++nRemoved;
                    newImplementation.removeInstruction(i--);
                    continue;
                }
                ++nRedirected;
                newImplementation.replaceInstruction(i, (BuilderInstruction35c)newInstruction);
            }
        }
        return newImplementation!=null ? newImplementation : origImplementation;
    }

/*
     * Check if the instruction must be rewritten
     * @param invokeInstr the instruction that contains the method invocation
     * @return the new instruction if exists a redefinition for this invocation,
     * null if the instruction must be removed (like an AD loading),
     * the original parameter if there's nothing to do

    private Instruction35c checkInstruction(final Instruction35c invokeInstr) {
        MethodReference mr = (MethodReference) invokeInstr.getReference();
        if (removeAds && isLoadingAd(mr))
            return null;
        if (onlyAdsRemoving)
            return invokeInstr;
        Set<String> permissions = apiToPermissions.get(mr);
        if (permissions == null)
            return invokeInstr;
        assert !permissions.isEmpty();
        out.printf(IOutput.Level.DEBUG, "Method %s.%s uses %s\n", mr.getDefiningClass(), mr.getName(), permissions);
        MethodReference redirection = redirections.get(mr);
        if (redirection != null) {
            out.printf(IOutput.Level.DEBUG, "Applying redirection to %s\n", redirection);
            return new BuilderInstruction35c(Opcode.INVOKE_STATIC,
                    invokeInstr.getRegisterCount(),
                    invokeInstr.getRegisterC(),
                    invokeInstr.getRegisterD(),
                    invokeInstr.getRegisterE(),
                    invokeInstr.getRegisterF(),
                    invokeInstr.getRegisterG(),
                    redirection);
        }
        if (mr.getReturnType().equals("V") && !noAutoRemoveVoid)
            return null;
        ++nNotRedirected;
        out.printf(IOutput.Level.ERROR,
                "Method %s.%s uses %s, but I don't have a redirection for it\n",
                mr.getDefiningClass(),
                mr.getName(),
                permissions);
        return invokeInstr;
    }

    private boolean isLoadingAd(MethodReference mr) {
        String defClass = mr.getDefiningClass();
        String methName = mr.getName();
        String retType = mr.getReturnType();
        if (methName.equals(LOADAD) && defClass.startsWith("Lcom/google/android/gms/ads/") && retType.equals("V")) {
            out.printf(IOutput.Level.DEBUG,
                    "ADSRM: removing '%s' method defined in class '%s'\n",
                    LOADAD, defClass);
            return true;
        }
        return false;
    }*/
}
