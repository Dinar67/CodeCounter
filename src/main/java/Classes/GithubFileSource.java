package Classes;

import Interfaces.ICodeSource;
import Services.GithubService;

public class GithubFileSource implements ICodeSource {
    private final String owner;
    private final String repo;
    private final String branch;
    private final String path;
    private final GithubService githubService;

    public GithubFileSource(String owner, String repo, String branch, String path, GithubService githubService) {
        this.owner = owner;
        this.repo = repo;
        this.branch = branch;
        this.path = path;
        this.githubService = githubService;
    }

    // Полный путь внутри репозитория, а не просто имя файла - иначе два одноимённых
    // File.java из разных папок будут неотличимы в списке
    @Override public String getDisplayName() { return path; }

    @Override public String readText() throws Exception {
        return githubService.downloadRawFile(owner, repo, branch, path);
    }
}