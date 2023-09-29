package io.randomthoughts;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    final static Playwright playwright = Playwright.create();

    final static Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setChannel("msedge").setHeadless(true));

    final static BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920, 1080));

    final static Page page = context.newPage();

    final static Path artifactsPath = Path.of(System.getProperty("user.dir"), "artifacts");

    static class ElmPackage {
        @JsonProperty
        private String name;

        @JsonProperty
        private String link;

        @JsonProperty
        private String desc;

        @JsonProperty
        private String version;

        public String getName() {
            return this.name;
        }

        public String getLink() {
            return this.link;
        }

        public String getDesc() {
            return this.desc;
        }

        public String getVersion() {
            return this.version;
        }
    }


    public static void main(String[] args) {
        System.out.println(artifactsPath);
        installPackages();
        dispose();
    }

    protected static void dispose() {
        page.close();
        browser.close();
        playwright.close();
    }

    private static void runQuiet(String command, File workingDirectory) {
        try {
            var process = Runtime.getRuntime().exec("pwsh -Command \"echo y | " + command + "\"", null, workingDirectory);
//            var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            var writer = new PrintWriter(new OutputStreamWriter(process.getOutputStream()));
//
//            reader.readLine();
//            writer.println('y');
//            writer.flush();

            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected static void installPackages() {
        var packages = getElmPackages();

        for (var pack : packages) {
            System.out.println("Installing package - " + pack.getName());
            installPackage(pack);
        }
    }

    private static File getPackageFolder(ElmPackage pack)
    {
        var normalizedName = pack.getName().replace('/', '~');

        return Path.of(artifactsPath.toString(), normalizedName).toFile();
    }

    protected static void installPackage(ElmPackage pack) {
        var newProjectPath = getPackageFolder(pack);

        // Create the directory
        if (!newProjectPath.exists()) {
            var created = newProjectPath.mkdirs();

            if (created) {
                System.out.println("Directory " + newProjectPath + " has been created.");
            } else {
                System.out.println("Unable to create a directory at " + newProjectPath + ".");
            }
        } else {
            System.out.println("Directory " + newProjectPath + " already exists.");
        }

        runQuiet("elm init", newProjectPath);
        runQuiet("elm install " + pack.getName(), newProjectPath);
    }

    private static List<ElmPackage> getElmPackages() {
        var jsonFilePath = Path.of(artifactsPath.toString(), "all-packages.json");

        try {
            var json = new String(Files.readAllBytes(jsonFilePath));
            var mapper = new ObjectMapper();

            var deserialized = mapper.readValue(json, ElmPackage[].class);

            return Arrays
                .stream(deserialized)
                .sorted(Comparator.comparing(ElmPackage::getName))
                .collect(Collectors.toList());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}