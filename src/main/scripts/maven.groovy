import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration

Repository createHostedRepository(String name) {
    if (!repository.getRepositoryManager().exists(name))
        return repository.createMavenHosted(name)
    return repository.getRepositoryManager().get(name)
}

Repository createProxyRepository(String name, String url) {
    if (!repository.getRepositoryManager().exists(name))
        return repository.createMavenProxy(name, url)
    return repository.getRepositoryManager().get(name)
}

Repository createGroupRepository(String name, List<String> members) {
    if (!repository.getRepositoryManager().exists(name))
        return repository.createMavenGroup(name, members)
    else {
        Repository repo = repository.getRepositoryManager().get(name)
        repo.stop()
        Configuration config = repo.configuration
        config.attributes('group').set('memberNames', members)
        repo.update(config)
        repo.start()
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
createHostedRepository('maven-snapshots')
createProxyRepository('maven-central', 'https://repo1.maven.org/maven2/')

createProxyRepository('jcenter', 'https://jcenter.bintray.com/')
createProxyRepository('jboss-ga', 'https://maven.repository.redhat.com/ga/')
createProxyRepository('gradle-plugins', 'https://plugins.gradle.org/m2/')
createProxyRepository('spring-plugins-release', 'http://repo.spring.io/plugins-release/')

createGroupRepository('maven-public', ['maven-releases', 'maven-snapshots', 'maven-central', 'jcenter', 'jboss-ga', 'gradle-plugins', 'spring-plugins-release'])