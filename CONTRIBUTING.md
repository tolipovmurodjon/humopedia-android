# Contributing

How we work on this repository, whether you are a person or Claude.

## The one-minute version

1. `main` is always shippable. Nobody pushes to it directly.
2. Every change lives on its own **branch**, goes up as a **pull request** (PR), passes CI, and is merged.
3. Releases are **tags** on `main` (`v2.0.0`), and each one has a higher `versionCode` than the last.

## Words you will see

| Word | Plain meaning |
|---|---|
| **commit** | A saved snapshot of your changes, with a message saying why |
| **branch** | Your own copy of the code to work on, without touching `main` |
| **push** | Upload your commits from your computer to GitHub |
| **pull** | Download what others pushed |
| **pull request (PR)** | "Please review my branch and merge it into `main`" |
| **merge** | The branch's changes join `main` |
| **CI** | GitHub builds and tests every PR automatically (the green tick or red cross) |
| **tag** | A name pinned to one commit, used for releases (`v2.0.0`) |

## Daily flow

```bash
git switch main && git pull                  # start from the latest main
git switch -c feat/onboarding-screen         # one branch per task
# ...work...
git add app/src/main/java/...                # add the files you changed, by name
git commit -m "feat(onboarding): add the grade picker"
git push -u origin feat/onboarding-screen
gh pr create --fill                          # open the pull request
```

When CI is green and the review is done: **Squash and merge** on GitHub, then delete the branch.

## Branch names

`type/short-description`, lowercase, words joined by hyphens:
`feat/lesson-player` · `fix/crash-on-rotate` · `chore/bump-compose` · `docs/release-guide`

## Commit messages

[Conventional Commits](https://www.conventionalcommits.org), English, imperative, under 72 characters:

```
feat(lesson): play the balance-scale lesson offline
fix(auth): keep the session after the app is killed
chore(deps): update Compose BOM to 2026.09.00
```

Types: `feat` new behaviour · `fix` bug · `chore` build or tooling · `docs` · `refactor` · `test` · `perf`.

## Never commit

`keystore.properties`, `*.jks`, `*.keystore`, `google-services.json`, `local.properties`, `build/`, `.idea/`.
`.gitignore` blocks them; if `git status` ever shows one, stop and ask.

## Releasing to Google Play

1. On a branch, raise `versionCode` by 1 and set `versionName` in `app/build.gradle.kts`. Merge it.
2. `./gradlew bundleRelease` on `main`, with `keystore.properties` in place.
3. Install the release build on a real phone and use it end to end.
4. Upload `app/build/outputs/bundle/release/app-release.aab` in Play Console, internal testing first.
5. Tag it: `git tag v2.0.0 && git push origin v2.0.0`, and add a GitHub release with the notes.

## Working with Claude

Claude works the same way: a branch per task, a PR per change, CI green before merge. It never reads or
prints the keystore or its passwords, and never pushes to `main`. Ask it to "open a PR"; you review and merge.

## Working with teammates

- Talk before starting something big: open an issue, say what you will do.
- Small PRs (under ~400 lines) get reviewed fast; huge ones get rubber-stamped.
- Before merging, someone other than the author looks at the PR.
- Pull `main` into your branch often (`git pull origin main`) so conflicts stay small.
