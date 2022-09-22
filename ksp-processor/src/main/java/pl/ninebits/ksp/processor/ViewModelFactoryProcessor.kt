package pl.ninebits.ksp.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import com.squareup.kotlinpoet.ksp.writeTo
import pl.ninebits.ksp.annotation.AssistedAutogenFactory
import pl.ninebits.ksp.annotation.AutogenFactory

const val ASSISTED_FACTORY_NAME = "assistedFactory"
class ViewModelFactoryProcessor(
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger,
    private val options: Map<String, String>,
) : SymbolProcessor {
    private val vmQ = options[VIEWMODEL_ARG] ?: VIEWMODEL_DEFAULT
    private val vmK = ClassName.bestGuess(vmQ)
    private val vmFactoryQ = options[VIEWMODEL_PROVIDER_FACTORY_ARG] ?: VIEWMODEL_PROVIDER_FACTORY_DEFAULT
    private val vmFactoryK = ClassName.bestGuess(vmFactoryQ)

    override fun process(resolver: Resolver): List<KSAnnotated> {
        val interfaces = resolver
            .getSymbolsWithAnnotation(AssistedAutogenFactory::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.INTERFACE }
            .toList()
        interfaces.forEach { generateAssisted(it) }
        val classes = resolver
            .getSymbolsWithAnnotation(AutogenFactory::class.qualifiedName.orEmpty())
            .filterIsInstance<KSClassDeclaration>()
            .filter { it.classKind == ClassKind.CLASS }
            .toList()
        classes.forEach { generate(it) }

        return emptyList()
    }

    @OptIn(KspExperimental::class)
    private fun generateAssisted(ksClassDeclaration: KSClassDeclaration) {
        val name = ksClassDeclaration.toClassName()

        val annotation = ksClassDeclaration.getAnnotationsByType(AssistedAutogenFactory::class).first()
        val method = ksClassDeclaration.getDeclaredFunctions().firstOrNull {
            it.simpleName.asString() == annotation.methodName
        }
        if (method == null) {
            logger.error("${name.simpleName}: does not have a method named '${annotation.methodName}'", ksClassDeclaration)
            return
        }

        val returnType = method.returnType?.resolve()
        val returnDeclaration = returnType?.declaration as? KSClassDeclaration
        if (!returnDeclaration.isExtendViewModel(vmQ)) {
            logger.error("${name.simpleName}: return type '${returnType?.toString()}' for method '${annotation.methodName}' does not extend '$vmQ'", ksClassDeclaration)
            return
        }

        logger.warn("Found assisted: $method, ${returnDeclaration?.toClassName()}")
        val file = generateAssistedFile(ASSISTED_FACTORY_NAME, annotation.methodName, ksClassDeclaration, method.parameters)
        file.writeTo(codeGenerator, Dependencies(true))
    }

    private fun generateAssistedFile(
        assistedFactoryName: String,
        callMethodName: String,
        factoryInterface: KSClassDeclaration,
        parameters: List<KSValueParameter>,
    ): FileSpec {
        val newClassName =
            ClassName(factoryInterface.packageName.asString(), "${factoryInterface.simpleName.asString()}Autogen")
        val addParams = parameters.map { param ->
            param.name!!.asString() to param.type.resolve().toTypeName()
        }
        val allParams = listOf(assistedFactoryName to factoryInterface.toClassName()) + addParams

        val pc = FunSpec.constructorBuilder().apply {
            allParams.forEach { (name, type) -> addParameter(name, type) }
        }.build()

        val codeBlock = typedCallCode(addParams)
        val func = vmCreateFunction(vmK) { t ->
            addStatement("return %N.%N($codeBlock) as %T", assistedFactoryName, callMethodName, t)
        }

        val ext = FunSpec.builder("invoke").apply {
            addModifiers(KModifier.OPERATOR)
            addParams.forEach { (name, type) -> addParameter(name, type) }
            receiver(factoryInterface.toClassName())
            returns(newClassName)
            addStatement("return %T(this, $codeBlock)", newClassName)
        }.build()

        val typeClass = TypeSpec.classBuilder(newClassName).apply {
            addSuperinterface(vmFactoryK)
            primaryConstructor(pc)

            // constructor properties
            allParams.forEach { (name, type) ->
                addProperty(
                    PropertySpec.builder(name, type)
                        .initializer(name)
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
            }

            // functions
            addFunction(func)
        }.build()

        return FileSpec.builder(newClassName.packageName, newClassName.simpleName)
            .addType(typeClass)
            .addFunction(ext)
            .addImport(factoryInterface.packageName.asString(), factoryInterface.simpleName.asString())
            .build()
    }

    @OptIn(KspExperimental::class)
    private fun generate(ksClassDeclaration: KSClassDeclaration) {
        val name = ksClassDeclaration.toClassName()
        val annotation = ksClassDeclaration.getAnnotationsByType(AutogenFactory::class).first()

        val pc = ksClassDeclaration.primaryConstructor
        if (pc == null) {
            logger.exception(IllegalStateException("${name.simpleName}: primary constructor not found"))
            return
        }
        if (!ksClassDeclaration.isExtendViewModel(vmQ)) {
            logger.exception(IllegalStateException("${name.simpleName}: needs to extend '$vmQ'"))
            return
        }

        logger.warn("Found: ${ksClassDeclaration.toClassName()}")
        val file = generateFile(ksClassDeclaration, annotation.suffix, pc.parameters)
        file.writeTo(codeGenerator, Dependencies(true))
    }

    private fun generateFile(
        vm: KSClassDeclaration,
        suffix: String,
        parameters: List<KSValueParameter>,
    ): FileSpec {
        val newClassName =
            ClassName(vm.packageName.asString(), "${vm.simpleName.asString()}$suffix")
        val addParams = parameters.map { param ->
            param.name!!.asString() to param.type.resolve().toTypeName()
        }

        val pc = FunSpec.constructorBuilder().apply {
            addParams.forEach { (name, type) -> addParameter(name, type) }
        }.build()

        val codeBlock = typedCallCode(addParams)
        val func = vmCreateFunction(vmK) { t ->
            addStatement("return %N($codeBlock) as %T", vm.simpleName.asString(), t)
        }

        val typeClass = TypeSpec.classBuilder(newClassName).apply {
            addSuperinterface(vmFactoryK)
            primaryConstructor(pc)

            // constructor properties
            addParams.forEach { (name, type) ->
                addProperty(
                    PropertySpec.builder(name, type)
                        .initializer(name)
                        .addModifiers(KModifier.PRIVATE)
                        .build()
                )
            }

            // functions
            addFunction(func)
        }.build()

        return FileSpec.builder(newClassName.packageName, newClassName.simpleName)
            .addType(typeClass)
            .addImport(vm.packageName.asString(), vm.simpleName.asString())
            .build()
    }

    companion object {
        const val VIEWMODEL_ARG = "autogen-viewmodel"
        const val VIEWMODEL_DEFAULT = "androidx.lifecycle.ViewModel"
        const val VIEWMODEL_PROVIDER_FACTORY_ARG = "autogen-viewmodel-provider-factory"
        const val VIEWMODEL_PROVIDER_FACTORY_DEFAULT = "androidx.lifecycle.ViewModel"


        private fun KSClassDeclaration?.isExtendViewModel(vmQ: String): Boolean = this?.getAllSuperTypes()
            ?.any { it.declaration.qualifiedName?.asString() == vmQ } == true

        private fun vmCreateFunction(vmK: ClassName, block: FunSpec.Builder.(t: TypeVariableName) -> Unit): FunSpec {
            val t = TypeVariableName("T", vmK)
            return FunSpec.builder("create")
                .addModifiers(KModifier.OVERRIDE)
                .addTypeVariable(t)
                .returns(t)
                .addParameter("modelClass", Class::class.asClassName().parameterizedBy(t))
                .addAnnotation(
                    AnnotationSpec.builder(Suppress::class)
                        .addMember(CodeBlock.of("%S", "UNCHECKED_CAST"))
                        .build()
                )
                .apply { block(t) }
                .build()
        }

        private fun typedCallCode(params: List<Pair<String, TypeName>>): String {
            val pattern = params.joinToString(separator = ", ") { (name, _) -> "%${name}:N" }
            return CodeBlock.builder()
                .addNamed(pattern, params.associate { (name, _) -> name to name })
                .build()
                .toString()
        }
    }
}