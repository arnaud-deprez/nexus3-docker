import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.maven.LayoutPolicy
import org.sonatype.nexus.repository.maven.VersionPolicy
import org.sonatype.nexus.repository.storage.WritePolicy

Repository createHostedRepository(final String name,
                                  final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                  final boolean strictContentTypeValidation = true,
                                  final VersionPolicy versionPolicy = VersionPolicy.RELEASE,
                                  final WritePolicy writePolicy = WritePolicy.ALLOW_ONCE,
                                  final LayoutPolicy layoutPolicy = LayoutPolicy.STRICT) {
    if (!repository.getRepositoryManager().exists(name)) {
        log.info("Create hosted repository $name")
        Repository repo = repository.createMavenHosted(name, blobStoreName, strictContentTypeValidation, versionPolicy, writePolicy, layoutPolicy)
        log.info("Repository created: ${repo.getConfiguration()}")
        return repo
    }
    return repository.getRepositoryManager().get(name)
}

def createHostedRepository(Map m) { createHostedRepository m*.value }

Repository createProxyRepository(final String name,
                                 final String url,
                                 final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                 final boolean strictContentTypeValidation = true,
                                 final VersionPolicy versionPolicy = VersionPolicy.RELEASE,
                                 final LayoutPolicy layoutPolicy = LayoutPolicy.STRICT) {
    if (!repository.getRepositoryManager().exists(name)) {
        log.info("Create proxy repository $name")
        Repository repo = repository.createMavenProxy(name, url, blobStoreName, strictContentTypeValidation, versionPolicy, layoutPolicy)
        log.info("Repository created: ${repo.getConfiguration()}")
        return repo
    }
    return repository.getRepositoryManager().get(name)
}

Repository createGroupRepository(final String name,
                                 final List<String> members,
                                 final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME) {
    if (!repository.getRepositoryManager().exists(name)) {
        log.info("Create group repository $name")
        Repository repo = repository.createMavenGroup(name, members, blobStoreName)
        log.info("Repository created: ${repo.getConfiguration()}")
        return repo
    }
    else {
        log.info("Update group repository $name")
        Repository repo = repository.getRepositoryManager().get(name)
        Configuration config = repo.getConfiguration()
        Map<String, Map<String,Object>> attributes = config.getAttributes()
        attributes.group.memberNames = members
        config.setAttributes(attributes)
        repository.getRepositoryManager().update(config)
        log.info("Repository updated: ${repo.getConfiguration()}")
        return repo
    }
}

/**
 * by default in the base image contains:
 *   - groups:
 *     - maven-public
 *   - hosted:
 *     - maven-releases
 *     - maven-snapshots
 *   - proxy:
 *     - maven-central
 */
createHostedRepository('maven-releases')
createHostedRepository('maven-snapshots', BlobStoreManager.DEFAULT_BLOBSTORE_NAME, true, VersionPolicy.SNAPSHOT, WritePolicy.ALLOW)
createProxyRepository('maven-central', 'https://repo1.maven.org/maven2/')

createProxyRepository('jcenter', 'https://jcenter.bintray.com/')
createProxyRepository('jboss-ga', 'https://maven.repository.redhat.com/ga/')
createProxyRepository('gradle-plugins', 'https://plugins.gradle.org/m2/')
createProxyRepository('spring-plugins-release', 'http://repo.spring.io/plugins-release/')

createGroupRepository('maven-public', ['maven-releases', 'maven-snapshots', 'maven-central', 'jcenter', 'jboss-ga', 'gradle-plugins', 'spring-plugins-release'])
