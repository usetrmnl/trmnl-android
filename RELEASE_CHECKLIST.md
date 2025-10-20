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

<details><summary>Example Run Output</summary>

```
🚀 Suggested next version:
  - Version Code: 22
  - Version Name: 2.0.4

To update version, run the GitHub Actions workflow 'Version Management' with:
  - Version name: 2.0.4
  - Version code: 22
  - Git tag: v2.0.4 (default)
  - Release notes: your comma-separated release notes
```
</details>

### 2. Run the Version Management workflow

- Go to GitHub Actions → [Bump App Version](https://github.com/usetrmnl/trmnl-android/actions/workflows/version-management.yml) workflow
- Click "Run workflow"
- Enter the required information:
  - **Version name**: Semantic version (e.g., 1.9.5)
  - **Version code**: Integer value, must increase with each release
  - **Git tag** (optional): Defaults to v + version name
  - **Release notes**: Comma-separated release notes
  - **Create PR**: Whether to create a pull request or commit directly
 
<img width="357" height="719" alt="Screenshot 2025-08-15 at 1 01 32 PM" src="https://github.com/user-attachments/assets/8f0b308b-dc1f-406b-82bc-6d0d9b216426" />


### 3. Wait for workflow completion

- The workflow will update all necessary files
- It will create or update the changelog
- It will commit changes

This automated process ensures consistency across all version-related files without manual edits.

### 4. Review PR and trigger CI

After the version management workflow has completed:

1. **Review the PR** (if you ran the workflow with direct commits):

<img width="936" height="752" alt="Screenshot 2025-08-15 at 1 02 30 PM" src="https://github.com/user-attachments/assets/dd138659-3546-4a24-a102-59fb79d5902e" />


2. **Follow pre and post merge command guidelinse**: Merge the PR. See PR description with guides.
3. **Download signed release build**: Use the workflow that automatically signs and builds release APK.

Example PR with instructions:  
<img width="1315" height="321" alt="Screenshot 2025-08-15 at 1 29 48 PM" src="https://github.com/user-attachments/assets/f600cb63-09f1-4731-bde5-afb54210bb5f" />

Workflow containing the built APK:
<img width="1551" height="870" alt="Screenshot 2025-08-15 at 1 30 00 PM" src="https://github.com/user-attachments/assets/5252d934-e534-4287-b95b-0f66912f90aa" />

Unzipped artifact that is used for release:
<img width="710" height="51" alt="Screenshot 2025-08-15 at 1 30 25 PM" src="https://github.com/user-attachments/assets/71c1f97e-71f5-434b-aa74-a0ec24acf194" />



4. **Create a GitHub release**:
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
