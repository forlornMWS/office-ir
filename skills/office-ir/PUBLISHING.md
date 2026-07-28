# Publishing the office-ir skill to skills.sh

[skills.sh](https://skills.sh) is an open agent-skills leaderboard. Skills are
**distributed via git repos**, not uploaded — you push a public GitHub repo
containing a skill folder with `SKILL.md`, and users install with one command.
No registry PR or manual submission; the leaderboard auto-indexes public repos
and ranks by install telemetry.

## The short version

Build the jar into the skill folder, commit, push — then anyone installs with:

```bash
npx skills add <owner>/office-ir --full-depth
```

That's it. `skills add` installs the **whole skill folder** — `SKILL.md` *and*
the co-located `office-ir.jar` — into the agent's skill dir. The skill runs
immediately (no clone/build at install time); it just needs a JDK 17 `java` on
the consumer's machine.

> Verified: `skills add` ships sibling files alongside `SKILL.md` (a marker file
> survived install into `~/.claude/skills/<name>/`). So the jar rides along.

## CLI (the `skills` npm package)

```bash
npx skills@latest init <name>      # scaffold <name>/SKILL.md locally
npx skills add <owner>/<repo>      # install skill(s) from GitHub
npx skills add <owner>/<repo> --full-depth   # discover SKILL.md in any subdir
npx skills add <url-to-SKILL.md>   # …or from a hosted file
npx skills list                    # show installed skills
npx skills remove [skills]         # uninstall
```

Install scope defaults to project `.claude/skills/`; `-g` for user-global
`~/.claude/skills/`. Claude Code installs via symlink by default (or `--copy`
for real files).

## Prepare the publishable skill folder

The folder `skills/office-ir/` must contain `SKILL.md` + the built jar. The
repo's `scripts/build-skill.sh` builds the jar and bundles it (plus SKILL.md)
into `dist/office-ir/`, but for publishing you want the jar **inside the skill
folder in the repo**:

```bash
# one-time layout: build jar straight into the skill folder
JAVA_HOME=/path/java17 mvn -o clean package -DskipTests
cp target/office-ir.jar skills/office-ir/office-ir.jar
git add skills/office-ir/office-ir.jar skills/office-ir/SKILL.md
git commit -m "feat: bundle office-ir.jar with skill"
```

Rebuild + recommit on each release to ship updates.

## Publish steps

1. Push this repo to a public GitHub repo, e.g. `https://github.com/<owner>/office-ir`.
   - Set the real owner in `SKILL.md` step 1 (replace `<owner>`).
2. Tag a release so consumers can pin a version.
3. Verify from a clean clone:
   ```bash
   npx skills add <owner>/office-ir --full-depth -g -y
   # in Claude Code, invoke the office-ir skill on a sample file
   ```
4. The leaderboard auto-indexes public repos once users run `skills add`. To
   boost discoverability, add GitHub topics (`claude-code`, `skill`, `excel`,
   `word`, `office`, `json-ir`) and a README linking to `https://skills.sh`.

## Tradeoff: commit the jar, or build on demand?

- **Commit jar (simple, recommended above):** one `skills add`, runs instantly.
  Cost: the ~21 MB jar sits in git history (binary, poor delta compression).
  Fine for a personal/small repo; use a release-tagged commit so history doesn't
  pile up many jar revisions.
- **Don't commit jar (lean):** skill ships only `SKILL.md`; step 1 clones the
  repo + `mvn package` at first use. Tiny skill, but every consumer needs JDK 17
  **and** Maven at runtime — friction. Use only if repo size matters more than UX.

Tip: to avoid jar-revision bloat in history, keep the jar in one commit (don't
keep re-adding rebuilt jars across history) and rely on GitHub Releases for
versioned binaries if you need distribution channels.
