package org.jetbrains.kootstrap

import org.jetbrains.kootstrap.util.opt
import org.jetbrains.kootstrap.util.targetRoots
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext

/**
 * Created by akhin on 7/5/16.
 */

fun main(args: Array<String>) {

    val cmd = opt.parse(args)

    val cfg = FooBarCompiler.setupMyCfg(cmd)
    val env = FooBarCompiler.setupMyEnv(cfg)

    val ktFiles = env.getSourceFiles().map {
        val f = KtPsiFactory(it).createFile(it.virtualFile.path, it.text)
        f.originalFile = it
        f
    }
    val targetFiles = ktFiles.filter { f ->
        cmd.targetRoots.any { root ->
            f.originalFile.virtualFile.path.startsWith(root)
        }
    }

    var ctx = FooBarCompiler.analyzeBunchOfSources(env, ktFiles, cfg)
            ?: BindingContext.EMPTY

    targetFiles.asSequence()
            .forEach { println(it.name); println(it.text) }
}
