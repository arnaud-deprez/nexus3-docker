import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.Repository
import org.sonatype.nexus.repository.config.Configuration
import org.sonatype.nexus.repository.storage.WritePolicy

Repository createHostedRepository(final String name,
                                  final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                  final boolean strictContentTypeValidation = true,
                                  final WritePolicy writePolicy = WritePolicy.ALLOW_ONCE) {
    if (!repository.getRepositoryManager().exists(name)) {
        log.info("Create hosted repository $name")
        Repository repo = repository.createNpmHosted(name, blobStoreName, strictContentTypeValidation, writePolicy)
        log.info("Repository created: ${repo.getConfiguration()}")
        return repo
    }
    return repository.getRepositoryManager().get(name)
}

def createHostedRepository(Map m) { createHostedRepository m*.value }

Repository createProxyRepository(final String name,
                                 final String url,
                                 final String blobStoreName = BlobStoreManager.DEFAULT_BLOBSTORE_NAME,
                                 final boolean strictContentTypeValidation = true) {
    if (!repository.getRepositoryManager().exists(name)) {
        log.info("Create proxy repository $name")
        Repository repo = repository.createNpmProxy(name, url, blobStoreName, strictContentTypeValidation)
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
        Repository repo = repository.createNpmGroup(name, members, blobStoreName)
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

createHostedRepository('npm-releases')
createProxyRepository('npm-registry', 'http://registry.npmjs.org/')
createGroupRepository('npm-public', ['npm-releases', 'npm-registry'])