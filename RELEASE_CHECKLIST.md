# Release Checklist for TRMNL Android

This document outlines the process for creating new releases of the TRMNL Android app.

## Version Management

The project uses GitHub Actions to manage version synchronization across different files:

- `app/build.gradle.kts`: Contains the app's version code and name
- `metadata/ink.trmnl.android.yml`: Contains F-Droid metadata and versioning
- `fastlane/metadata/android/en-US/changelogs/`: Contains version-specific changelog files

## Creating a new release

### 1. Check current version information

```bash
# Show current version information and suggested next version
./scripts/show_version_info.sh
```

This will display:
- Current version code and name
- F-Droid metadata version information
- Existing changelog files
- Suggested next version

### 2. Run the Version Management workflow

- Go to GitHub Actions → Version Management workflow
- Click "Run workflow"
- Enter the required information:
  - **Version name**: Semantic version (e.g., 1.9.5)
  - **Version code**: Integer value, must increase with each release
  - **Git tag** (optional): Defaults to v + version name
  - **Release notes**: Comma-separated release notes
  - **Create PR**: Whether to create a pull request or commit directly

### 3. Wait for workflow completion

- The workflow will update all necessary files
- It will create or update the changelog
- It will commit changes and create a git tag

This automated process ensures consistency across all version-related files without manual edits.

### 4. Post-release tasks

After the version management workflow has completed:

1. **Pull the latest changes** (if you ran the workflow with direct commits):
   ```bash
   git pull origin main
   ```

2. **Verify the build works with the new version**:
   ```bash
   # Build release variant to ensure it works
   ./gradlew assembleRelease
   ```

3. **Create a GitHub release**:
   - Go to GitHub → Releases → Draft new release
   - Select the tag created by the workflow
   - Title: "TRMNL Android v{VERSION_NAME}"
   - Description: Copy content from the changelog file
   - Attach the built APK

## Troubleshooting

If you encounter issues with the version management workflow:

1. **Manual versioning**:
   - Update `app/build.gradle.kts` with the new version code and name
   - Update `metadata/ink.trmnl.android.yml` with the same version information
   - Create a new changelog file in `fastlane/metadata/android/en-US/changelogs/{VERSION_CODE}.txt`
   - Commit and tag manually

2. **Workflow permissions**:
   - Ensure the GitHub Actions workflow has sufficient permissions to create commits and tags
   - Check the repository settings → Actions → General → Workflow permissions
