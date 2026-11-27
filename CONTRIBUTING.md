# Contributing

## Setup

Requires JDK 25+, [Bun](https://bun.sh) and `ffmpeg`.

```bash
make setup    # web deps and Playwright browsers
make bundles  # compile all bundles
make dev      # site at http://127.0.0.1:5273
```

Read [Architecture](web/docs/architecture.md) before a large change. The other pages under [web/docs/](web/docs/) are the user docs.

## Change

1. Open an issue first for any sort of feature request, refactor, or fix.
2. Branch from an up-to-date `main`.
3. Make sure to add tests paired with your change, as follows:
   - JVM logic: a JUnit test in the module.
   - Browser behavior: a program in `modules/bundles/sandbox` and a spec in `web/tests/`. To run a spec in Firefox, add it to the Firefox `testMatch` in `web/playwright.config.ts`.
4. Update `web/docs/` when behavior or a public API changes. `ApiReferenceTest` fails when `web/docs/api.md` excludes any public type.

## Check

| Command | Checks |
| --- | --- |
| `make test` | JVM unit tests |
| `make browser` | Playwright tests in Chromium and Firefox (WASM and JS) |
| `make lint`, `make typecheck` | Java style, web lint, types |
| `make bundles-release` | Release bundles over `modules/toolchain/size-budget.txt` |
| `make check` | CI |

Notes:
- PR CI only builds dev bundles
- Run `make bundles-release` before you change a class library, a shim or the sandbox
- Failing size budgets block the site deploy
- Time Firefox in a normal install: Playwright's Firefox runs WebAssembly without its optimizing compiler
- Never commit `vendor/`

## Commits and PRs

- Title: describes the change, short and lowercase. For example, `fix ci` or `add lwjgl 3 openal`; [Conventional Commit](https://www.conventionalcommits.org/en/v1.0.0/) style is *optional*, but in general the title should be fairly descriptive for a brief clause.
- Body: `Closes #123.` for each issue it closes
- For more than three changes: list them all as bullets in the body or split the commit.
- Keep one change per pull request
- Pull requests are squash-merged into main

## Writing

Write docs, comments, issues and commit messages in short, simple technical English, using concrete verbs. Remove filler and repeated information. Comments only where code is not obvious.

## License

The repo and all contributions are licensed under the [MIT License](LICENSE).
