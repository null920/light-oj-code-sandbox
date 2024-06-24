/**
 * 无限睡眠（阻塞程序执行）
 *
 * @author null&&
 * @Date 2024/6/24 17:28
 */
public class Main {
    public static void main(String[] args) throws InterruptedException {
        // 一小时
        long ONE_HOUR = 60 * 60 * 1000L;
        Thread.sleep(ONE_HOUR);
        System.out.println("睡完了");
    }
}
