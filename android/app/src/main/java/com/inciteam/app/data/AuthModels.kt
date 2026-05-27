package com.inciteam.app.data

data class SignInResponse(
    val token: String,
    val userId: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val workEmail: String?,
    val role: String,
    val workspace: WorkspaceSummary?
)

data class WorkspaceSummary(
    val organizationId: Long?,
    val organizationName: String?,
    val teamId: Long?,
    val teamName: String?,
    val teamRole: String?,
    val teamTimezone: String?
)

data class AuthenticatedUser(
    val id: Long,
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val workEmail: String?,
    val role: String,
    val workspace: WorkspaceSummary?
) {
    val displayName: String
        get() = firstName?.trim()?.takeIf { it.isNotEmpty() } ?: username

    val workspaceLine: String
        get() {
            val organization = workspace?.organizationName?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Your organization"
            val team = workspace?.teamName?.trim()?.takeIf { it.isNotEmpty() }
                ?: "Current team"
            return "$organization / $team"
        }

    val isGlobalAdmin: Boolean
        get() = role.equals("Admin", ignoreCase = true)

    val canManageCurrentTeam: Boolean
        get() {
            if (isGlobalAdmin) {
                return true
            }
            val teamRole = workspace?.teamRole?.uppercase()
            return teamRole == "TEAM_ADMIN" || teamRole == "MANAGER"
        }

    companion object {
        fun from(response: SignInResponse): AuthenticatedUser {
            return AuthenticatedUser(
                id = response.userId,
                username = response.username,
                firstName = response.firstName,
                lastName = response.lastName,
                workEmail = response.workEmail,
                role = response.role,
                workspace = response.workspace
            )
        }
    }
}

data class StoredSession(
    val token: String,
    val user: AuthenticatedUser
)
