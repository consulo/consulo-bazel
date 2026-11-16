package com.intellij.bazel.python.backend;

// TODO consulo: not ported. consulo-python has the old packaging API (PyPackageManager per SDK, created by the
//  PyPackageManagers service) and no PythonPackageManagerProvider extension point, PythonPackageManager, PythonInterpreter,
//  PythonRepositoryManager, PyResult or PythonPackage(name, version, isEditable); the Bazel package manager (registered upstream
//  as Pythonid.pythonPackageManagerProvider) cannot be plugged in. Upstream code:
//
// internal class BazelPythonPackageManagerProvider : PythonPackageManagerProvider {
//   override fun createPackageManager(project: Project, interpreter: PythonInterpreter): PythonPackageManager? {
//     if (!project.isBazelProject) return null
//     return BazelPythonPackageManager(project, interpreter)
//   }
// }
//
// // TODO: fetch the list of installed Python packages properly from Bazel.
// //  For now this just hardcodes pytest to make run gutters work
// private class BazelPythonPackageManager(project: Project, interpreter: PythonInterpreter) : PythonPackageManager(project, interpreter) {
//   private companion object {
//     val PYTEST = PythonPackage("pytest", "1.0.0", false)
//   }
//
//   init {
//     installedPackages = listOf(PYTEST)
//   }
//
//   override val dependenciesFilesRelativePaths: List<Path>
//     get() = emptyList()
//
//   override val repositoryManager: PythonRepositoryManager
//     get() = BazelPythonRepositoryManager(project)
//
//   override suspend fun syncLockedCommand(): PyResult<Unit> = Result.Success(Unit)
//
//   override suspend fun installPackageCommand(
//     installRequest: PythonPackageInstallRequest,
//     options: List<String>,
//     module: Module?,
//     dependencyGroup: PyDependencyGroup?,
//   ): PyResult<Unit> = Result.Success(Unit)
//
//   override suspend fun updatePackageCommand(vararg specifications: PythonRepositoryPackageSpecification): PyResult<Unit> =
//     Result.Success(Unit)
//
//   override suspend fun uninstallPackageCommand(
//     vararg pythonPackages: String,
//     workspaceMember: PyWorkspaceMember?,
//     dependencyGroup: PyDependencyGroup?,
//   ): PyResult<Unit> = Result.Success(Unit)
//
//   override suspend fun loadPackagesCommand(): PyResult<List<PythonPackage>> {
//     return Result.Success(listOf(PYTEST))
//   }
//
//   override suspend fun loadOutdatedPackagesCommand(): PyResult<List<PythonOutdatedPackage>> =
//     Result.Success(emptyList())
// }
//
// private class BazelPythonRepositoryManager(
//   override val project: Project,
// ) : PythonRepositoryManager {
//   override val allRepositories: List<PyPackageRepository>
//     get() = emptyList()
//
//   override suspend fun getPackageDetails(
//     packageName: String,
//     repository: PyPackageRepository?,
//   ): PyResult<PythonPackageDetails> =
//     @Suppress("HardCodedStringLiteral")
//     Result.Failure(MessageError("Not implemented"))
//
//   override suspend fun getLatestVersion(
//     packageName: String,
//     repository: PyPackageRepository?,
//   ): PyPackageVersion? = null
//
//   override suspend fun getVersions(
//     packageName: String,
//     repository: PyPackageRepository?,
//   ): List<String>? = null
//
//   override suspend fun refreshCaches(): Result<Unit, PythonRepositoryManager.PythonRepositoryIOError> = Result.Success(Unit)
//
//   override suspend fun initCaches(): Result<Unit, PythonRepositoryManager.PythonRepositoryIOError> = Result.Success(Unit)
//
//   override suspend fun findPackageSpecification(
//     requirement: PyRequirement,
//     repository: PyPackageRepository?,
//   ): PythonRepositoryPackageSpecification? = null
// }
final class BazelPythonPackageManager {
    private BazelPythonPackageManager() {
    }
}
