package cn.muyang.helpers;

import cn.muyang.env.SetupManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ProcessHelper {
    public static class ProcessResult {
        public static final Locale locale = Locale.getDefault();
        public int exitCode;
        public long execTime;
        public boolean timeout;
        public String stdout;
        public String stderr;
        public String commandLine;

        public ProcessResult() {
            stdout = stderr = "";
        }

        public void check(String processName) {
            if (!timeout && exitCode == 0) {
                return;
            }
            if (timeout) {
                System.err.println(processName + " compilation timed out. This may be because you have too many classes and methods to compile, or the machine performance is low.");
            } else {
                if (commandLine.contains("zig") && commandLine.contains("myj2c")) {
                    System.err.println(processName + " compilation error:" + stderr);
                    System.err.println("Zig temporary files have been automatically cleaned up for you: " + SetupManager.getZigGlobalCacheDirectory(true) + " Please run again");
                    System.out.println("If it fails again, please delete it manually and try again. If it still fails after manual deletion, please report the problem to the developer");
                }
            }
            //System.err.println("Command line: \n" + commandLine);
            System.err.println("exit: \n" + exitCode);
            System.err.println("stdout: \n" + stdout);
            System.err.println("stderr: \n" + stderr);
            throw new RuntimeException(processName + " " + (timeout ? "Command execution timed out" : "Command execution error"));
        }
    }

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    private static void readStream(InputStream is, Consumer<String> consumer) {
        executor.submit(() -> {
            try (InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8);
                 BufferedReader reader = new BufferedReader(isr)) {

                int count;
                char[] buf = new char[1 << 10];
                while ((count = reader.read(buf)) != -1) {
                    consumer.accept(String.copyValueOf(buf, 0, count));
                }
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    public static ProcessResult run(Path directory, long timeLimit, List<String> command) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Map<String, String> environment = processBuilder.environment();
        environment.put("ZIG_GLOBAL_CACHE_DIR", directory + File.separator + "cpp" + File.separator + ".cache");
        environment.put("TEMP", directory + File.separator + "cpp" + File.separator + ".temp");
        environment.put("TMP", directory + File.separator + "cpp" + File.separator + ".temp");
        Process process = processBuilder.directory(directory.toFile()).start();
        long startTime = System.currentTimeMillis();

        ProcessResult result = new ProcessResult();
        result.commandLine = String.join(" ", command);

        StringBuilder stdoutBuilder = new StringBuilder();
        StringBuilder stderrBuilder = new StringBuilder();

        readStream(process.getInputStream(), stdoutBuilder::append);
        readStream(process.getErrorStream(), stderrBuilder::append);
        try {
            if (!process.waitFor(timeLimit, TimeUnit.MILLISECONDS)) {
                result.timeout = true;
                process.destroyForcibly();
            }
            process.waitFor();
        } catch (InterruptedException ignored) {
        }

        result.stdout = stdoutBuilder.toString();
        result.stderr = stderrBuilder.toString();
        result.execTime = System.currentTimeMillis() - startTime;
        result.exitCode = process.exitValue();
        if (process.exitValue() != 0) {
            try {
                process.waitFor(1000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
            }
            process.destroyForcibly();
        }
        return result;
    }
}
