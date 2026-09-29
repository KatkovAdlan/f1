import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class TestLaunchWrapper {
    public static void main(String[] args) throws Exception {
        if (args.length < 3 || !"--world".equals(args[0])) {
            System.err.println("Usage: TestLaunchWrapper --world <worldName> <java> [java args...]");
            System.exit(2);
        }

        final String worldName = args[1];
        final String javaExecutable = args[2];

        List<String> javaArgs = new ArrayList<>();
        for (int i = 3; i < args.length; i++) {
            javaArgs.add(args[i]);
        }

        List<String> command = new ArrayList<>(javaArgs.size() + 1);
        command.add(javaExecutable);
        command.addAll(javaArgs);

        Process child = new ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .start();

        Thread stdinProxy = new Thread(() -> proxyInput(child, worldName),
                "XtoXray-TestLaunchWrapper-stdin");
        stdinProxy.setDaemon(true);
        stdinProxy.start();

        int exitCode = child.waitFor();
        stdinProxy.join(2000L);
        System.exit(exitCode);
    }

    private static void proxyInput(Process child, String worldName) {
        try {
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));
            BufferedWriter out = new BufferedWriter(
                    new OutputStreamWriter(child.getOutputStream(), StandardCharsets.UTF_8));

            List<String> launchScript = new ArrayList<>();
            String line;

            while ((line = in.readLine()) != null) {
                launchScript.add(line);

                if ("launch".equals(line) || "abort".equals(line)) {
                    if ("launch".equals(line)) {
                        injectWorldName(launchScript, worldName);
                    }

                    writeLines(out, launchScript);
                    launchScript.clear();
                    out.flush();

                    if ("abort".equals(line)) {
                        return;
                    }
                    break;
                }
            }

            while ((line = in.readLine()) != null) {
                out.write(line);
                out.newLine();
                out.flush();
            }
        } catch (IOException e) {
            try {
                child.getOutputStream().close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void injectWorldName(List<String> script, String worldName) {
        script.removeIf(line -> line.startsWith("worldName "));

        int insertAt = script.size();
        for (int i = 0; i < script.size(); i++) {
            String line = script.get(i);
            if (line.startsWith("param ") || line.startsWith("windowTitle ")) {
                insertAt = i;
                break;
            }
        }

        script.add(insertAt, "worldName " + worldName);
    }

    private static void writeLines(BufferedWriter out, List<String> lines) throws IOException {
        for (String value : lines) {
            out.write(value);
            out.newLine();
        }
    }
}
