import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

public class Utils {

    public static void printUsage() {
        System.out.println("Usage:\nMove to folder bin!\njava CodeGenerator ../testFiles/<filename>.jmm");
    }

    public static PrintWriter toFile(String className) {

        try {
            File dir = new File("../jasmin");
            if (!dir.exists())
                dir.mkdirs();

            File output = new File("../jasmin/" + className + ".j");
            if (!output.exists())
                output.createNewFile();

            PrintWriter writer = new PrintWriter(output);

            return writer;

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

        return null;
    }

}
