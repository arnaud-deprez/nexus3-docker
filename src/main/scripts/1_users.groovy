import groovy.json.JsonOutput
import org.sonatype.nexus.security.role.NoSuchRoleException
import org.sonatype.nexus.security.role.Role
import org.sonatype.nexus.security.user.User
import org.sonatype.nexus.security.user.UserStatus

import static org.sonatype.nexus.security.user.UserManager.DEFAULT_SOURCE

Role createOrUpdateRole(String id, String name, String description, List<String> privileges, List<String> roles) {
    Role role = null
    try {
        role = security.getSecuritySystem().getAuthorizationManager(DEFAULT_SOURCE).getRole(id)
    }
    catch (NoSuchRoleException ex) {
        log.info("No role found with id: $id. Will create this role", ex)
    }
    if (role) {
        role.setName(name)
        role.setDescription(description)
        role.setPrivileges(privileges.toSet())
        role.setRoles(roles.toSet())
        role = security.getSecuritySystem().getAuthorizationManager(DEFAULT_SOURCE).updateRole(role)
    } else {
        role = security.addRole(id, name, description, privileges, roles)
    }
    return role
}

User createOrUpdateUser(String id, String firstName, String lastName, String email, boolean active, String password, List<String> roles) {
    User user = security.getSecuritySystem().getUser(id)
    if (user) {
        user.setFirstName(firstName)
        user.setLastName(lastName)
        user.setEmailAddress(email)
        user.setStatus(active ? UserStatus.active : UserStatus.disabled)
        security.getSecuritySystem().updateUser(user)
        user = security.setUserRoles(id, roles)
    } else {
        user = security.addUser(id, firstName, lastName, email, active, password, roles)
    }
    return user
}

//
// Create a new role that allows a user same access as anonymous and adds healtchcheck access
//
def devPrivileges = ['nx-healthcheck-read', 'nx-healthcheck-summary-read']
def anoRole = ['nx-anonymous']
// add roles that uses the built in nx-anonymous role as a basis and adds more privileges
createOrUpdateRole('developer', 'Developer', 'User with privileges to allow read access to repo content and healtcheck', devPrivileges, anoRole)
log.info('Role developer created')
// use the new role to create a user
def devRoles = ['developer']

//
// Create new role that allows deployment and create a user to be used on a CI server
//
// privileges with pattern * to allow any format, browse and read are already part of nx-anonymous
def depPrivileges = ['nx-repository-view-*-*-add', 'nx-repository-view-*-*-edit']
// add roles that uses the developer role as a basis and adds more privileges
createOrUpdateRole('deployer', 'Deployer', 'User with privileges to allow deployment all repositories', depPrivileges, devRoles)
log.info('Role deployer created')
def depRoles = ['deployer']
def lJenkins = createOrUpdateUser('jenkins', 'Leeroy', 'Jenkins', 'leeroy.jenkins@example.com', true, 'jenkins', depRoles)
log.info('User jenkins created')


log.info('Script 1_users completed successfully')

//Return a JSON response containing our new Users for confirmation
return JsonOutput.toJson([lJenkins])