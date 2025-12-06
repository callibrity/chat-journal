# Workflow

## Pre-commit Checklist

Before committing any changes, ALWAYS run the following command to ensure all files have proper license headers:

```bash
mvn -Plicense license:format
```

This will automatically add the Apache 2.0 license header to any files missing it. The build will fail if license headers are missing, so running this command first prevents build failures.
