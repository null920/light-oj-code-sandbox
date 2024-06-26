import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * @author null&&
 * @Date 2024/6/24 19:46
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        String userDir = System.getProperty("user.dir");
        // 通过相对路径，直接读取到项目的配置文件
        String filePath = userDir + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator + "木马程序.bat";
        // Linux 提权
        // String authCmd = "chmod 777 " + filePath;
        // Runtime.getRuntime().exec(authCmd).waitFor();
        Process process = Runtime.getRuntime().exec(filePath);
        process.waitFor();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String compileOutputLine;
        while ((compileOutputLine = bufferedReader.readLine()) != null) {
            System.out.println(compileOutputLine);
        }
        System.out.println("木马执行完成");
    }
}