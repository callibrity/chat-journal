# Release Command

Generate a GitHub release with release notes.

## Arguments
- `$ARGUMENTS` - Optional version number (e.g., 0.0.4). If not provided, auto-increments the patch version.

## Instructions

1. First, verify the build succeeds (including javadoc):
   ```
   mvn clean verify -q
   ```
   If the build fails, report the errors to the user and stop. Do not proceed with the release until all build issues are resolved.

2. Fetch the latest tags:
   ```
   git fetch --tags
   ```

3. Find the previous tag (the most recent tag):
   ```
   git tag -l --sort=-v:refname | head -1
   ```

4. Determine the new version:
   - If `$ARGUMENTS` is provided and non-empty, use that as the version
   - Otherwise, parse the previous tag and increment the patch version (last digit) by 1
   - Version must be semver format: `x.y.z` (three digits)

5. Get all commits between the previous tag and HEAD:
   ```
   git log <previous-tag>..HEAD --pretty=format:"%h %s%n%b" --reverse
   ```

6. Analyze the commits and generate release notes in this format:
   ```markdown
   ## Release <version>

   ### New Features
   - Feature descriptions...

   ### Improvements
   - Improvement descriptions...

   ### Bug Fixes
   - Bug fix descriptions...

   ### Testing
   - Testing improvements...

   ### Documentation
   - Documentation changes...
   ```

   Only include sections that have relevant changes. Group related commits together and write user-friendly descriptions (not just commit messages).

7. Based on the changes, propose 3-4 short, descriptive release title options. Format: `x.y.z - {title}`

   Examples:
   - "0.0.4 - Conversation Persistence & Infinite Scroll"
   - "0.0.4 - Enhanced Chat History"
   - "0.0.4 - Repository Refactoring & UI Improvements"

   The title should capture the most significant or user-facing changes.

8. Present the release notes and title options to the user. Ask them to:
   - Confirm or edit the release notes
   - Choose a title option (or provide their own)

9. Once confirmed, create the GitHub release:
   ```
   gh release create <version> --title "<chosen-title>" --notes "<release-notes>"
   ```

10. Report the release URL to the user.

## Notes
- The release will automatically trigger the GitHub Action to publish to Maven Central
- Always fetch tags first to ensure you have the latest
- If no previous tag exists, include all commits
- The tag will be the semver version (e.g., `0.0.4`), the title will be descriptive (e.g., `0.0.4 - Feature Name`)
