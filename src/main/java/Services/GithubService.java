package Services;

import Interfaces.IService;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GithubService implements IService {

    private static final Pattern REPO_URL_PATTERN =
            Pattern.compile("github\\.com/([^/\\s]+)/([^/\\s#?]+)");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public record RepoRef(String owner, String repo) {}
    public record RemoteFile(String path) {}

    public RepoRef parseUrl(String url) throws Exception {
        Matcher m = REPO_URL_PATTERN.matcher(url);
        if (!m.find()) throw new Exception("Не удалось распознать ссылку на GitHub-репозиторий");
        String owner = m.group(1);
        String repo = m.group(2).replaceAll("\\.git$", "");
        return new RepoRef(owner, repo);
    }

    public String fetchDefaultBranch(RepoRef ref) throws Exception {
        String url = "https://api.github.com/repos/" + ref.owner() + "/" + ref.repo();
        var request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .GET().build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "Не удалось получить информацию о репозитории");

        var obj = JsonParser.parseString(response.body()).getAsJsonObject();
        return obj.get("default_branch").getAsString();
    }

    // Один запрос - получает пути ВСЕХ .java файлов репозитория без скачивания содержимого
    public List<RemoteFile> listJavaFiles(RepoRef ref, String branch) throws Exception {
        String url = "https://api.github.com/repos/" + ref.owner() + "/" + ref.repo()
                + "/git/trees/" + branch + "?recursive=1";
        var request = HttpRequest.newBuilder(URI.create(url))
                .header("Accept", "application/vnd.github+json")
                .GET().build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "Не удалось получить список файлов репозитория");

        var obj = JsonParser.parseString(response.body()).getAsJsonObject();
        boolean truncated = obj.has("truncated") && obj.get("truncated").getAsBoolean();

        JsonArray tree = obj.getAsJsonArray("tree");
        List<RemoteFile> result = new ArrayList<>();
        for (var el : tree) {
            JsonObject entry = el.getAsJsonObject();
            if (!"blob".equals(entry.get("type").getAsString())) continue;
            String path = entry.get("path").getAsString();
            if (path.endsWith(".java")) result.add(new RemoteFile(path));
        }

        if (truncated) {
            throw new Exception("Репозиторий слишком большой - GitHub API вернул неполный список файлов ("
                    + result.size() + " найдено). Попробуйте проанализировать конкретную подпапку отдельно.");
        }
        return result;
    }

    // Скачивает содержимое ОДНОГО файла как текст - вызывается отдельно для каждого файла
    // в момент анализа, а не заранее списком. Именно это даёт потоковую обработку:
    // в памяти в любой момент - только те файлы, что реально сейчас анализируются потоками пула
    public String downloadRawFile(String owner, String repo, String branch, String path) throws Exception {
        String url = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path;
        var request = HttpRequest.newBuilder(URI.create(url)).GET().build();

        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        checkStatus(response, "Не удалось скачать файл " + path);
        return response.body();
    }

    private void checkStatus(HttpResponse<String> response, String message) throws Exception {
        if (response.statusCode() == 403) {
            throw new Exception(message + ": превышен лимит запросов к GitHub API, попробуйте позже");
        }
        if (response.statusCode() == 404) {
            throw new Exception(message + ": не найдено (возможно, репозиторий приватный)");
        }
        if (response.statusCode() / 100 != 2) {
            throw new Exception(message + ": HTTP " + response.statusCode());
        }
    }
}