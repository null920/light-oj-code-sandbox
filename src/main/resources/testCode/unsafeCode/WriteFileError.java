import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

/**
 * 向服务器写入文件（植入木马）
 *
 * @author null&&
 * @Date 2024/6/24 19:31
 */
public class Main {
    public static void main(String[] args) throws IOException {
        String userDir = System.getProperty("user.dir");
        // 通过相对路径，直接读取到项目的配置文件
        String filePath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "木马程序.bat";
        String errorProgram = "java -version 2>&1";
        Files.write(Paths.get(filePath), Arrays.asList(errorProgram));
        System.out.println("写入木马成功，你完了哈哈");
    }
}
