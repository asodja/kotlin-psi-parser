package psi.parser

import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.cli.common.config.addKotlinSourceRoots
import org.jetbrains.kotlin.cli.common.messages.AnalyzerWithCompilerReport
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.cli.jvm.compiler.NoScopeRecordCliBindingTrace
import org.jetbrains.kotlin.cli.jvm.compiler.TopDownAnalyzerFacadeForJVM
import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoots
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmAnalysisFlags
import org.jetbrains.kotlin.config.JvmDefaultMode.ENABLE
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_1_4
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.load.java.JavaTypeEnhancementState
import org.jetbrains.kotlin.load.java.Jsr305Settings
import org.jetbrains.kotlin.load.java.ReportLevel.STRICT
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

object Environment {

    fun newEnvironment(): KotlinCoreEnvironment {
        return KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            getConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        )
    }

    private
    fun getConfiguration() = CompilerConfiguration().apply {
        put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, messageCollector)
        put(JVMConfigurationKeys.RETAIN_OUTPUT_IN_MEMORY, false)
        put(JVMConfigurationKeys.DISABLE_OPTIMIZATION, true)
        put(CommonConfigurationKeys.MODULE_NAME, "parser")
        configureKotlinCompilerForGradleBuild()
        addJvmClasspathRoots(PathUtil.getJdkClassesRoots(File("/Users/asodja/.sdkman/candidates/java/current")))
//            addJvmClasspathRoots(compilationClasspath)
        addKotlinSourceRoots(listOf("/Users/asodja/prototype/kotlin-psi-parser/lib/src/test/kotlin/test"))
    }

    private
    val messageCollector: MessageCollector
        get() = PrintingMessageCollector(System.out, MessageRenderer.PLAIN_RELATIVE_PATHS, false)

    private
    fun CompilerConfiguration.configureKotlinCompilerForGradleBuild() {
        put(
            CommonConfigurationKeys.LANGUAGE_VERSION_SETTINGS,
            LanguageVersionSettingsImpl(
                languageVersion = KOTLIN_1_4,
                apiVersion = ApiVersion.KOTLIN_1_4,
                analysisFlags = mapOf(
                    JvmAnalysisFlags.javaTypeEnhancementState to JavaTypeEnhancementState(
                        Jsr305Settings(STRICT, STRICT)
                    ) { STRICT },
                    JvmAnalysisFlags.jvmDefaultMode to ENABLE
                )
            )
        )

        put(JVMConfigurationKeys.PARAMETERS_METADATA, true)
        put(JVMConfigurationKeys.JVM_TARGET, JvmTarget.JVM_1_8)
    }

    fun getBindingContext(env: KotlinCoreEnvironment, files: List<KtFile>): BindingContext {
        val analyzer = AnalyzerWithCompilerReport(
            messageCollector,
            env.configuration.languageVersionSettings,
        )
        analyzer.analyzeAndReport(files) {
            TopDownAnalyzerFacadeForJVM.analyzeFilesWithJavaIntegration(
                env.project,
                files,
                NoScopeRecordCliBindingTrace(),
                env.configuration,
                env::createPackagePartProvider,
                ::FileBasedDeclarationProviderFactory
            )
        }

        return analyzer.analysisResult.bindingContext
    }
}