package org.jetbrains.kootstrap

import com.intellij.openapi.extensions.ExtensionPoint
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.Disposer
import com.intellij.pom.PomModel
import com.intellij.pom.PomTransaction
import com.intellij.pom.core.impl.PomModelImpl
import com.intellij.pom.tree.TreeAspect
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.impl.source.codeStyle.IndentHelper
import com.intellij.psi.impl.source.tree.TreeCopyHandler
import com.intellij.psi.search.GlobalSearchScope
import org.apache.commons.cli.CommandLine
import org.jetbrains.kootstrap.idea.MockCodeStyleManager
import org.jetbrains.kootstrap.idea.MockIndentHelper
import org.jetbrains.kootstrap.util.*
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.CliLightClassGenerationSupport
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.JvmPackagePartProvider
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.addKotlinSourceRoots
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

/**
 * Created by akhin on 7/5/16.
 */

object FooBarCompiler {

    init {
        Extensions.getRootArea().registerExtensionPoint(
                TreeCopyHandler.EP_NAME.name,
                TreeCopyHandler::class.java.canonicalName,
                ExtensionPoint.Kind.INTERFACE
        )
    }

    fun analyzeBunchOfSources(
            env: KotlinCoreEnvironment,
            files: Collection<KtFile>,
            cfg: CompilerConfiguration
    ): BindingContext? {
        return TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                env.project,
                files,
                CliLightClassGenerationSupport.CliBindingTrace(),
                cfg,
                { scope -> JvmPackagePartProvider(env, scope) }
        ).bindingContext
    }

    fun setupMyCfg(
            cmd: CommandLine): CompilerConfiguration {

        val cfg = CompilerConfiguration()

        val jdkRoots = PathUtil.getJdkClassesRootsFromCurrentJre()
        val kotlinRoots = PathUtilEx.getKotlinPathsForCompiler()

        // TODO: Do not add the same jar file twice

        cfg.addJvmClasspathRoots(jdkRoots)
        cfg.addJvmClasspathRoots(kotlinRoots)
        cfg.addJvmClasspathRoots(cmd.jarFiles.map(::File))

        if (cmd.pomFile.isNotEmpty()) {
            setupFromPom(File(cmd.pomFile), cfg)
        }

        cfg.addKotlinSourceRoots(cmd.kotlinRoots)
        cfg.addKotlinSourceRoots(cmd.targetRoots)

        cfg.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        cfg.put(CommonConfigurationKeys.MODULE_NAME, JvmAbi.DEFAULT_MODULE_NAME)

        return cfg
    }

    fun setupMyEnv(cfg: CompilerConfiguration): KotlinCoreEnvironment {

        val env = KotlinCoreEnvironment.createForProduction(
                Disposer.newDisposable(),
                cfg,
                EnvironmentConfigFiles.JVM_CONFIG_FILES
        )

        class MyPomModelImpl(env: KotlinCoreEnvironment) : PomModelImpl(env.project) {
            override fun runTransaction(pt: PomTransaction) = pt.run()
        }

        val pomModel = MyPomModelImpl(env)
        TreeAspect(pomModel)

        env.application.registerService(
                PomModel::class.java,
                pomModel
        )

        env.application.registerService(
                CodeStyleManager::class.java,
                MockCodeStyleManager(env.project)
        )

        env.application.registerService(
                IndentHelper::class.java,
                MockIndentHelper()
        )

        return env
    }

    fun tearDownMyEnv(env: KotlinCoreEnvironment) =
            KotlinCoreEnvironment.disposeApplicationEnvironment()

}
