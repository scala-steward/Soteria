package com.leobenkel.safetyplugin.Transformations

import com.leobenkel.safetyplugin.Config.SafetyConfiguration
import com.leobenkel.safetyplugin.Messages.ErrorMessage
import com.leobenkel.safetyplugin.Utils.LoggerExtended
import com.leobenkel.safetyplugin.Utils.ImplicitModuleToString._
import com.leobenkel.safetyplugin.Messages.CommonMessage._
import com.leobenkel.safetyplugin.Modules.Dependency
import sbt.ModuleID

private[Transformations] trait CheckVersion {

  /**
    * This will check that the libraries have the legal versions.
    *
    * @param libraries the libraries fetch by the build, coming from [[sbt.Keys.allDependencies]]
    * @param log       The current log
    */
  def checkVersion(
    log:        LoggerExtended,
    config:     SafetyConfiguration,
    libraries:  Seq[ModuleID],
    moreErrors: ErrorMessage = ErrorMessage.Empty
  ): ResultMessages = {
    log.separatorDebug("LibraryDependencyWriter.checkVersion")

    val allCorrectLibraries = config.CorrectVersions

    val librariesToCheck: Seq[ModuleID] = getLibraryToCheck(allCorrectLibraries, libraries)

    log.debug(s"> Verifying version of libraries (${librariesToCheck.size}) :")
    librariesToCheck.prettyString(log, "checkVersion")

    consolidateErrors(allCorrectLibraries, librariesToCheck, moreErrors)
  }

  private def getLibraryToCheck(
    allCorrectLibraries: Seq[Dependency],
    libraries:           Seq[ModuleID]
  ): Seq[ModuleID] = {
    for {
      correctLibrary <- allCorrectLibraries
      library        <- libraries if correctLibrary === library
    } yield {
      library
    }
  }

  private def consolidateErrors(
    allCorrectLibraries: Seq[Dependency],
    librariesToCheck:    Seq[ModuleID],
    moreErrors:          ErrorMessage
  ): ResultMessages = {
    (allCorrectLibraries
      .filter(_.version.isRight)
      .map(m => (m, m.version.right.get))
      .flatMap {
        case (correctModule, correctVersion) =>
          buildErrors(librariesToCheck, correctModule, correctVersion)
      }
      .toError("Wrong versions") ++ moreErrors)
      .resolve(
        s"> All ${librariesToCheck.size} libraries that we " +
          s"know have the right versions"
      )
  }

  private def buildErrors(
    librariesToCheck: Seq[ModuleID],
    correctModule:    Dependency,
    correctVersion:   String
  ): Seq[String] = {
    librariesToCheck
      .filter(m => (correctModule === m) && m.revision != correctVersion)
      .map { m =>
        val correctModuleToString = m
          .withRevision(correctVersion)
          .withName(correctModule.name)
          .prettyString

        s"${m.prettyString} should be $correctModuleToString"
      }
  }
}
