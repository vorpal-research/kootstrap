package org.jetbrains.kootstrap

import org.jetbrains.kootstrap.util.opt
import org.jetbrains.kootstrap.util.targetRoots
import org.jetbrains.kotlin.codegen.kotlinType
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getType

/**
 * Created by akhin on 7/5/16.
 */

fun variableType(variable: KtVariableDeclaration, ctx: BindingContext) =
        (ctx[BindingContext.DECLARATION_TO_DESCRIPTOR, variable] as VariableDescriptor).returnType

fun doit(args: Array<String>) {
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
            .forEach { println(it.name); println(it.text); println(it.collectDescendantsOfType<KtVariableDeclaration>().joinToString("\n") {
                "${it.name}: ${variableType(it, ctx).toString()}"
            }) }
}

fun main(args: Array<String>) {
    doit(args)
}
