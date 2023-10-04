package io.randomthoughts;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class Main {
    final static String artifactsPath = Paths.get(System.getProperty("user.dir"), "artifacts").toString();

    private static String getArtifactPath(String... nodes) {
        return Paths.get(artifactsPath, nodes).toString();
    }

    private static String getArtifactUrl(String... nodes) {
        assert nodes.length > 1;

        return "https://package.elm-lang.org/" + String.join("/", nodes);
    }

    private static void downloadIfMissing(String... nodes) {
        var target = getArtifactPath(nodes);
        var title = "📦 " + String.join("/", nodes);

        var targetFile = Paths.get(target).toFile();

        if (targetFile.exists()) {
            System.out.println(title + " ➡ Skipped");
            return;
        }

        attempt(title, () -> download(nodes));
    }

    private static void downloadForcibly(String... nodes) {
        attempt("💪 " + String.join("/", nodes), () -> download(nodes));
    }

    private static void attempt(String title, CheckedRunnable<Exception> runnable) {
        System.out.print(title + " ➡ ");

        try {
            runnable.run();
            System.out.println("OK");
        } catch (RuntimeException e) {
            System.out.println("Error: " + e.getCause().getClass().getCanonicalName());
        }
    }

    private static void download(String... nodes) throws IOException {
        var source = getArtifactUrl(nodes);
        var target = getArtifactPath(nodes);
        var targetPath = Paths.get(target);

        // Create directory
        targetPath.getParent().toFile().mkdirs();

        try (var in = new URL(source).openStream()) {
            Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void downloadPackageMetaFiles(String pack) {
        for (var version : getReleases(pack)) {
            downloadPackageMetaFiles(pack, version);
        }
    }

    private static void downloadPackageMetaFiles(String pack, String version) {
        downloadIfMissing("packages", pack, version, "elm.json");
        downloadIfMissing("packages", pack, version, "docs.json");
        downloadIfMissing("packages", pack, version, "README.md");
        // downloadIfMissing("packages", pack, version, "endpoint.json");
    }

    public static List<String> getReleases(String pack) {
        downloadForcibly("packages", pack, "releases.json");

        var releases = readJsonFile(
            getArtifactPath("packages", pack, "releases.json"),
            new TypeReference<HashMap<String, Integer>>() {}
        );

        assert releases != null;

        return new ArrayList<>(releases.keySet());
    }

    public static void main(String[] args) {
        System.out.println("Artifacts Path: " + artifactsPath);
        installPackages();
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
            var packageName = pack.get("name");

            downloadPackageMetaFiles(packageName);
            installPackage(packageName);
            cleanupPackage(packageName);
        }
    }

    private static File getPackageInstallationFolder(String pack) {
        return Paths.get(getArtifactPath("packages", pack, "@install")).toFile();
    }

    protected static void installPackage(String pack) {
        attempt("✨ " + pack, () -> {
            var projectFolder = getPackageInstallationFolder(pack);

            // Create the directory
            var $ = projectFolder.mkdirs();

            runQuiet("elm init", projectFolder);
            runQuiet("elm install " + pack, projectFolder);
        });
    }

    private static void deleteFile(File file) {
        var removeCommand = "Remove-Item -Force -Recurse -Path \"" + file.getAbsolutePath() + "\"";
        var cmd = "pwsh -Command \"" + removeCommand.replace("\"", "\"\"") + "\"";

        attempt("📃 " + removeCommand, () -> Runtime.getRuntime().exec(cmd, null).waitFor());
    }

    private static void cleanupPackage(String pack) {
        var projectFolder = getPackageInstallationFolder(pack);

        deleteFile(projectFolder);
    }

    private static List<HashMap<String, String>> getElmPackages() {
        downloadForcibly("search.json");

        var packages = readJsonFile(
            getArtifactPath("search.json"),
            new TypeReference<ArrayList<HashMap<String, String>>>() {}
        );

        assert packages != null;

        packages.sort(Comparator.comparing(p -> p.get("name")));

        return packages;
    }

    private static <T> T readJsonFile(String jsonFilePath, TypeReference<T> type) {
        try {
            var json = new String(Files.readAllBytes(Paths.get(jsonFilePath)));
            var mapper = new ObjectMapper();

            return mapper.readValue(json, type);
        } catch (Exception e) {
            return null;
        }
    }
}

@FunctionalInterface
interface CheckedRunnable<E extends Exception> extends Runnable {

    @Override
    default void run() throws RuntimeException {
        try {
            runExceptionally();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    void runExceptionally() throws E;
}