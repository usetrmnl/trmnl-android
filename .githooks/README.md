# Git Hooks

This project uses Git hooks to maintain code quality.

## Setup for new developers

After cloning the repository, run:

```bash
git config core.hooksPath .githooks
```

This will configure Git to use the hooks in the `.githooks` directory.

## Available hooks

### Pre-commit

Automatically runs `./gradlew formatKotlin` before each commit and prevents commits if:
- The code fails to pass formatting checks
- The formatter modifies files that aren't included in the commit

If your commit is rejected due to formatting issues:
1. Review the error messages
2. Add the formatted files to your commit with `git add`
3. Try committing again
