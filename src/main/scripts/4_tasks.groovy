import org.sonatype.nexus.blobstore.api.BlobStoreManager
import org.sonatype.nexus.repository.purge.PurgeUnusedTask
import org.sonatype.nexus.scheduling.TaskConfiguration
import org.sonatype.nexus.scheduling.TaskScheduler
import org.sonatype.nexus.scheduling.schedule.Cron

/**
 * This script is inspired from https://github.com/rgl/nexus-vagrant/blob/master/provision/provision-nexus/src/main/groovy/provision.groovy
 *
 **/

def createRemoveMavenSnapshotsTask(TaskScheduler taskScheduler) {
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance("repository.maven.remove-snapshots")
    taskConfiguration.name = "Maven - Delete old snapshots from maven-snapshots repository"

    taskConfiguration.setString("repositoryName", "maven-snapshots")
    taskConfiguration.setString("minimumRetained", "1")
    taskConfiguration.setString("snapshotRetentionDays", "30")
    // TODO: taskConfiguration.setAlertEmail("TODO")
    taskScheduler.scheduleTask(
        taskConfiguration,
        new Cron(new Date().next(), "0 0 0 ? * SAT")
    )
    log.info("Task ${taskConfiguration.name} created")
}

def createRemoveComponentsFromProxiesTask(TaskScheduler taskScheduler) {
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance("repository.purge-unused")
    taskConfiguration.name = "Proxy - Delete unused components"

    taskConfiguration.setString(PurgeUnusedTask.REPOSITORY_NAME_FIELD_ID, "*") // * = all repositories
    taskConfiguration.setString(PurgeUnusedTask.LAST_USED_FIELD_ID, "30")
    // TODO: taskConfiguration.setAlertEmail("TODO")
    taskScheduler.scheduleTask(
        taskConfiguration,
        new Cron(new Date().next(), "0 0 0 ? * SAT")
    )
    log.info("Task ${taskConfiguration.name} created")
}

def createCompactBlobstoreTask(TaskScheduler taskScheduler) {
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance("blobstore.compact")
    taskConfiguration.name = "Blobstore - Compact Blobstore " + BlobStoreManager.DEFAULT_BLOBSTORE_NAME

    taskConfiguration.setString("blobstoreName", BlobStoreManager.DEFAULT_BLOBSTORE_NAME)
    // TODO: taskConfiguration.setAlertEmail("TODO")
    taskScheduler.scheduleTask(
        taskConfiguration,
        new Cron(new Date().next(), "0 0 18 ? * SAT")
    )
    log.info("Task ${taskConfiguration.name} created")
}

def createScriptTask(String name, TaskScheduler taskScheduler, String cron = "0 0 0 * * ?") {
    TaskConfiguration taskConfiguration = taskScheduler.createTaskConfigurationInstance("script")
    taskConfiguration.name = "Script - $name"

    taskConfiguration.setString("language", "groovy")
    //TODO: request sonatype for a more convenient way
    taskConfiguration.setString("source", '''
import org.sonatype.nexus.repository.storage.Query
import org.sonatype.nexus.repository.storage.StorageFacet

import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

def deleteComponentsOlderThan(String repositoryName, Duration releaseRetention) {
    def fmt = DateTimeFormatter.ofPattern('yyyy-MM-dd HH:mm:ss')
    // Get a repository
    def repo = repository.repositoryManager.get(repositoryName)
    // Get a database transaction
    def tx = repo.facet(StorageFacet).txSupplier().get()
    // Search assets that haven't been downloaded for more than three months
    try {
        // Begin the transaction
        tx.begin()
        tx.findAssets(
            Query.builder()
                .where('last_downloaded <')
                .param(Instant.now().minus(releaseRetention).format(fmt))
                .build(),
            [repo]
        ).each { asset ->
            if (asset.componentId() != null) {
                def component = tx.findComponent(asset.componentId())
                if (component != null) {
                    def count = tx.countComponents(Query.builder().where('name').eq(component.name()).and('version >').param(component.version()).build(), [repo])
                    // Check if there is newer components of the same name
                    if (count > 0) {
                        log.info("Delete asset ${asset.name()} as it has not been downloaded since $releaseRetention and has a newer version")
                        tx.deleteAsset(asset)
                        tx.deleteComponent(component)
                    }
                }
            }
        }
        // End the transaction
        tx.commit()
    }
    catch (all) {
        log.info("Exception: ${all}")
        all.printStackTrace()
        tx.rollback()
    }
    finally {
        tx.close()
    }
}

[
    [name: 'npm-registry', retention: Duration.of(1, ChronoUnit.MONTHS)],
    [name: 'npm-snapshots', retention: Duration.of(1, ChronoUnit.MONTHS)],
    [name: 'maven-releases', retention: Duration.of(6, ChronoUnit.MONTHS)],
    [name: 'npm-releases', retention: Duration.of(6, ChronoUnit.MONTHS)]
].each { repo ->
    log.info("Start cleaning repository ${repo.name}")
    deleteComponentsOlderThan(repo.name, repo.retention)
    log.info("End cleaning repository ${repo.name}")
}
''')

    // TODO: taskConfiguration.setAlertEmail("TODO")
    taskScheduler.scheduleTask(
        taskConfiguration,
        new Cron(new Date().next(), cron)
    )
    log.info("Task ${taskConfiguration.name} created")
}

TaskScheduler taskScheduler = container.lookup(TaskScheduler.class)

createRemoveMavenSnapshotsTask(taskScheduler)
createRemoveComponentsFromProxiesTask(taskScheduler)
createCompactBlobstoreTask(taskScheduler)

// Script tasks for those that does not exist yet
createScriptTask("Delete unused components", taskScheduler, "0 0 0 ? * SAT")
