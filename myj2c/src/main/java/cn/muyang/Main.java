package cn.muyang;

import cn.muyang.env.SetupManager;
import cn.muyang.xml.Config;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class Main {

    public static final String VERSION = "2023.0209.08";
    public static final String LOGO = "\n      ███╗   ███╗ ██╗   ██╗    ██╗ ██████╗   ██████╗\n      ████╗ ████║ ╚██╗ ██╔╝    ██║ ╚════██╗ ██╔════╝\n      ██╔████╔██║  ╚████╔╝     ██║  █████╔╝ ██║     \n      ██║╚██╔╝██║   ╚██╔╝ ██   ██║ ██╔═══╝  ██║     \n      ██║ ╚═╝ ██║    ██║  ╚█████╔╝ ███████╗ ╚██████╗\n      ╚═╝     ╚═╝    ╚═╝   ╚════╝  ╚══════╝  ╚═════╝\n\n";

    public static final Locale locale = Locale.getDefault();

    @CommandLine.Command(name = "myj2c-Bytecode-Translator", mixinStandardHelpOptions = true, version = "MYJ2C Bytecode Translator " + VERSION,
            description = "Translator .jar file into .c files and generates output .jar file")
    private static class NativeObfuscatorRunner implements Callable<Integer> {

        @CommandLine.Parameters(index = "0", description = "Jar file to transpile")
        private File jarFile;

        @CommandLine.Parameters(index = "1", description = "Output directory")
        private String outputDirectory;

        @CommandLine.Option(names = {"-c", "--config"}, defaultValue = "config.xml",
                description = "Config file")
        private File config;

        @CommandLine.Option(names = {"-l", "--libraries"}, description = "Directory for dependent libraries")
        private File librariesDirectory;

        @CommandLine.Option(names = {"--plain-lib-name"}, description = "Plain library name for LoaderPlain")
        private String libraryName;

        @CommandLine.Option(names = {"-u","--lib-url"}, description = "Library url for Loader")
        private String libraryUrl;

        @CommandLine.Option(names = {"-a", "--annotations"}, description = "Use annotations to ignore/include native obfuscation")
        private boolean useAnnotations;

        @CommandLine.Option(names = {"-d"}, description = "Delete Jar file after translator")
        private boolean delete;

        @Override
        public Integer call() throws Exception {
            MYObfuscator obfuscator = new MYObfuscator();
            ExecutorService threadPool = Executors.newCachedThreadPool();
            StringBuilder stringBuilder = new StringBuilder();
            if (Files.exists(config.toPath())) {
                try (BufferedReader br = Files.newBufferedReader(config.toPath())) {
                    String str;
                    while ((str = br.readLine()) != null) {
                        stringBuilder.append(str);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                Path configPath = Files.createFile(config.toPath());
                if (locale.getLanguage().contains("zh")) {
                    stringBuilder.append("<myj2c>\n" +
                            "\t<targets>\n" +
                            "\t\t<target>WINDOWS_X86_64</target>\n" +
                            "\t\t<!--<target>WINDOWS_AARCH64</target>\n" +
                            "\t\t<target>MACOS_X86_64</target>\n" +
                            "\t\t<target>MACOS_AARCH64</target>-->\n" +
                            "\t\t<target>LINUX_X86_64</target>\n" +
                            "\t\t<!--<target>LINUX_AARCH64</target>-->\n" +
                            "\t</targets>\n" +
                            "\t<options>\n" +
                            "\t<!--<expireDate>2023-12-31</expireDate> 应用过期日期，格式yyyy-MM-dd -->\n" +
                            "\t\t<!--字符串混淆-->\n" +
                            "\t\t<stringObf>false</stringObf>\n" +
                            "\t\t<!--控制流混淆-->\n" +
                            "\t\t<flowObf>false</flowObf>\n" +
                            "\t</options>" +
                            "\t\t<!-- match支持 Ant 风格的路径匹配 ? 匹配一个字符, * 匹配多个字符, ** 匹配多层路径 -->\n" +
                            "\t\t<match className=\"**\" />\n" +
                            "\t\t<!--<match className=\"cn/myj2c/web/**\" />-->\n" +
                            "\t\t<!--<match className=\"cn.myj2c.service.**\" />-->\n" +
                            "\t</include>\n" +
                            "\t<exclude>\n" +
                            "\t\t<!--<match className=\"cn/myj2c/Main\" methodName=\"main\" methodDesc=\"(\\[Ljava/lang/String;)V\"/>-->\n" +
                            "\t\t<!--<match className=\"cn.myj2c.test.**\" />-->\n" +
                            "\t</exclude>\n" +
                            "</myj2c>\n");
                } else {
                    stringBuilder.append("<myj2c>\n" +
                            "\t<targets>\n" +
                            "\t\t<target>WINDOWS_X86_64</target>\n" +
                            "\t\t<!--<target>WINDOWS_AARCH64</target>\n" +
                            "\t\t<target>MACOS_X86_64</target>\n" +
                            "\t\t<target>MACOS_AARCH64</target>-->\n" +
                            "\t\t<target>LINUX_X86_64</target>\n" +
                            "\t\t<!--<target>LINUX_AARCH64</target>-->\n" +
                            "\t</targets>\n" +
                            "\t<options>\n" +
                            "\t<!--<expireDate>2023-12-31</expireDate>  Expiration date, format:yyyy-MM-dd -->\n" +
                            "\t\t<!--String obfuscation-->\n" +
                            "\t\t<stringObf>false</stringObf>\n" +
                            "\t\t<!--Control flow obfuscation-->\n" +
                            "\t\t<flowObf>false</flowObf>\n" +
                            "\t</options>" +
                            "\t<include>\n" +
                            "\t\t<!-- Match supports Ant style path matching? Match one character, * match multiple characters, * * match multiple paths -->\n" +
                            "\t\t<match className=\"**\" />\n" +
                            "\t\t<!--<match className=\"cn/myj2c/web/**\" />-->\n" +
                            "\t\t<!--<match className=\"cn.myj2c.service.**\" />-->\n" +
                            "\t</include>\n" +
                            "\t<exclude>\n" +
                            "\t\t<!--<match className=\"cn/myj2c/Main\" methodName=\"main\" methodDesc=\"(\\[Ljava/lang/String;)V\"/>-->\n" +
                            "\t\t<!--<match className=\"cn.myj2c.test.**\" />-->\n" +
                            "\t</exclude>\n" +
                            "</myj2c>\n");
                }
                Files.write(configPath, stringBuilder.toString().getBytes(StandardCharsets.UTF_8));
                if (locale.getLanguage().contains("zh")) {
                    System.out.println("读取配置文件失败,已为您生成默认配置文件");
                    System.out.println("默认配置编译所有类和方法,会严重影响程序运行性能,请慎重使用该功能");
                    System.out.println("请打开配置文件,配置编译的类和方法后继续运行编译命令");
                } else {
                    System.out.println("Unable to read configuration file. Default config has been generated for you");
                    System.out.println("The default configuration compiles all classes and methods, which will seriously affect the running performance of the program. Please use this function with caution");
                    System.out.println("Please open the configuration file, configure the compiled classes and methods, and then continue to run the command");
                }
                return 0;
            }
            Serializer serializer = new Persister();
            Config configInfo = serializer.read(Config.class, stringBuilder.toString());
            Future future = threadPool.submit(new Callable() {
                @Override
                public Path call() throws IOException {
                    return obfuscator.preProcess(jarFile.toPath(), configInfo, useAnnotations);
                }
            });
            if (locale.getLanguage().contains("zh")) {
                System.out.println("\n正在初始化系统...");
            } else {
                System.out.println("\nInitializing system...");
            }
            SetupManager.init();
            if (locale.getLanguage().contains("zh")) {
                System.out.println("初始化完成\n");
            } else {
                System.out.println("Initialization complete\n");
            }
            if (locale.getLanguage().contains("zh")) {
                System.out.println("正在读取配置文件:" + config.toPath());
            } else {
                System.out.println("Reading configuration file\n:" + config.toPath());
            }

            List<Path> libs = new ArrayList<>();
            if (librariesDirectory != null) {
                Files.walk(librariesDirectory.toPath(), FileVisitOption.FOLLOW_LINKS)
                        .filter(f -> f.toString().endsWith(".jar") || f.toString().endsWith(".zip"))
                        .forEach(libs::add);
            }
            if (new File(outputDirectory).isDirectory()) {
                File outFile = new File(outputDirectory, jarFile.getName());
                if (outFile.exists()) {
                    outFile.renameTo(new File(outputDirectory, jarFile.getName() + ".BACKUP"));
                }
            } else {
                File outFile = new File(outputDirectory);
                if (outFile.exists()) {
                    outFile.renameTo(new File(outputDirectory + ".BACKUP"));
                }
            }

            obfuscator.process(jarFile.toPath(), (Path) future.get(), Paths.get(outputDirectory), configInfo, libs, libraryName, libraryUrl, useAnnotations, delete);
            return 0;
        }
    }

    public static void main(String[] args) throws IOException {
        System.out.println(LOGO + "    MuYang Java to C Bytecode Translator V" + VERSION + "\n\n            Copyright (c) MYJ2C 2022-2024\n\n==========================================================\n");

        System.exit(new CommandLine(new NativeObfuscatorRunner()).setCaseInsensitiveEnumValuesAllowed(true).execute(args));
    }


    private static int getLength(final Object array) {
        if (array == null) {
            return 0;
        }
        return Array.getLength(array);
    }


}
