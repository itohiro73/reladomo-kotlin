# Reladomo-Kotlin Release Methodology

## Overview

This document describes the complete release process for the Reladomo-Kotlin project, designed to ensure secure, automated, and reliable releases to Maven Central.

## Architecture

### 1. Version Management
- **Semantic Versioning**: MAJOR.MINOR.PATCH format
- **Snapshot Versions**: Development versions end with `-SNAPSHOT`
- **Version File**: Centralized in `gradle.properties`

### 2. Build Pipeline
```
Developer Push → CI Build → PR Review → Merge → Tag → Release → Maven Central
```

### 3. Security Layers
1. **GPG Signing**: All artifacts signed with maintainer GPG key
2. **GitHub Secrets**: Encrypted storage of sensitive credentials
3. **Environment Protection**: Release environment requires approval
4. **2FA Requirements**: All accounts require two-factor authentication

## Automated Workflows

### CI Workflow (`.github/workflows/ci.yml`)
- **Triggers**: Push to main/develop, Pull requests
- **Jobs**:
  - Multi-version build (Java 17, 21)
  - Full test suite execution
  - Code quality checks
  - Dependency vulnerability scanning
- **Security**: Read-only permissions, no secrets access

### Release Workflow (`.github/workflows/release.yml`)
- **Triggers**: Git tags matching `v*` pattern
- **Jobs**:
  1. Validate tag format
  2. Build and test all modules
  3. Sign artifacts with GPG
  4. Publish to Maven Central staging
  5. Create GitHub release
  6. Update to next snapshot version
- **Security**: Requires environment approval, minimal permissions

## Publishing Configuration

### Modules Published
1. `reladomo-kotlin-core` - Core functionality
2. `reladomo-kotlin-generator` - Code generation tools
3. `reladomo-kotlin-spring-boot` - Spring Boot integration
4. `reladomo-kotlin-gradle-plugin` - Build plugin

### Maven Coordinates
```xml
<groupId>io.github.reladomo-kotlin</groupId>
<artifactId>reladomo-kotlin-[module]</artifactId>
<version>0.0.1</version>
```

### Gradle Configuration
- Publishing configured in `buildSrc/publishing-conventions.gradle.kts`
- Artifacts include: JAR, sources JAR, Javadoc JAR
- POM includes all required metadata for Maven Central

## Security Measures

### Secret Management
```
OSSRH_USERNAME          - Sonatype username
OSSRH_PASSWORD          - Sonatype password  
SIGNING_KEY_ID          - GPG key ID
SIGNING_PASSWORD        - GPG passphrase
SIGNING_SECRET_KEY      - Base64 encoded private key
```

### Access Control
- Release environment requires 2 maintainer approvals
- Branch protection on main branch
- Signed commits recommended
- Audit logs for all releases

## Release Process for v0.0.1

### Prerequisites
1. Set up GPG key for signing
2. Configure GitHub repository secrets
3. Verify OSSRH account access
4. Enable 2FA on all accounts

### Steps
1. **Prepare Release**
   ```bash
   # Update version
   sed -i 's/0.0.1-SNAPSHOT/0.0.1/' gradle.properties
   git add gradle.properties
   git commit -m "Prepare release 0.0.1"
   ```

2. **Create Release**
   ```bash
   # Tag release
   git tag -a v0.0.1 -m "Release version 0.0.1"
   git push origin v0.0.1
   ```

3. **Monitor Automation**
   - GitHub Actions builds and publishes
   - Staging repository created in OSSRH
   - Artifacts uploaded and signed

4. **Finalize Release**
   - Log into oss.sonatype.org
   - Close and release staging repository
   - Verify artifacts on Maven Central

### Post-Release
- Version automatically updated to 0.0.2-SNAPSHOT
- GitHub release created with artifacts
- Announcement ready for distribution

## Troubleshooting

### Common Issues
1. **GPG Signing Fails**: Check key expiration and secret encoding
2. **OSSRH Validation Fails**: Verify POM completeness
3. **GitHub Actions Timeout**: Check for test failures

### Rollback Procedure
- Cannot remove from Maven Central (immutable)
- Prepare hotfix release with incremented version
- Document known issues in CHANGELOG

## Maintenance

### Regular Tasks
- Monthly: Review and rotate secrets
- Quarterly: Update dependencies
- Yearly: Rotate GPG signing key
- Per-release: Update documentation

### Documentation
- `/docs/maintainer/` - Secure maintainer documentation
- `CHANGELOG.md` - User-facing changes
- `RELEASE_CHECKLIST.md` - Step-by-step checklist

## Benefits

1. **Automation**: Minimal manual intervention required
2. **Security**: Multiple layers of protection
3. **Reliability**: Consistent, repeatable process
4. **Transparency**: Full audit trail of releases
5. **Quality**: Automated testing before release

---

This methodology ensures that Reladomo-Kotlin releases are secure, reliable, and maintain the high quality expected by users of the library.