package com.didiglobal.booster.android.gradle.v4_1

import com.android.build.api.artifact.Artifact
import com.android.build.api.artifact.ArtifactType
import com.android.build.api.artifact.impl.ArtifactsImpl
import com.android.build.api.component.impl.ComponentPropertiesImpl
import com.android.build.api.transform.Context
import com.android.build.api.transform.QualifiedContent
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.api.LibraryVariant
import com.android.build.gradle.internal.api.BaseVariantImpl
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.pipeline.TransformTask
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.android.build.gradle.internal.scope.AnchorOutputType
import com.android.build.gradle.internal.scope.GlobalScope
import com.android.build.gradle.internal.scope.InternalArtifactType
import com.android.build.gradle.internal.scope.VariantScope
import com.android.build.gradle.internal.variant.BaseVariantData
import com.android.builder.core.VariantType
import com.android.builder.model.ApiVersion
import com.android.sdklib.AndroidVersion
import com.android.sdklib.BuildToolInfo
import com.didiglobal.booster.gradle.AGPInterface
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ArtifactCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.util.TreeMap

@Suppress("UnstableApiUsage")
private val ARTIFACT_TYPES = arrayOf(
        ArtifactType::class,
        AnchorOutputType::class,
        InternalArtifactType::class
).map {
    it.sealedSubclasses
}.flatten().map {
    it.objectInstance as Artifact.SingleArtifact<out FileSystemLocation>
}.map {
    it.javaClass.simpleName to it
}.toMap()

internal object V41 : AGPInterface {

    private val BaseVariant.componentProperties: ComponentPropertiesImpl
        get() = BaseVariantImpl::class.java.getDeclaredField("componentProperties").apply {
            isAccessible = true
        }.get(this) as ComponentPropertiesImpl

    @Suppress("UnstableApiUsage")
    private fun <T : FileSystemLocation> BaseVariant.getFinalArtifactFiles(type: Artifact.SingleArtifact<T>): Collection<File> {
        return listOfNotNull(artifacts.get(type).map(FileSystemLocation::getAsFile).orNull)
    }

    @Suppress("UnstableApiUsage")
    private val BaseVariant.artifacts: ArtifactsImpl
        get() = componentProperties.artifacts

    override val scopeFullWithFeatures: MutableSet<in QualifiedContent.Scope>
        get() = TransformManager.SCOPE_FULL_WITH_FEATURES

    override val scopeFullLibraryWithFeatures: MutableSet<in QualifiedContent.Scope>
        get() = (TransformManager.SCOPE_FEATURES + QualifiedContent.Scope.PROJECT).toMutableSet()

    override val BaseVariant.project: Project
        get() = globalScope.project

    override val BaseVariant.javaCompilerTaskProvider: TaskProvider<out Task>
        get() = javaCompileProvider

    override val BaseVariant.preBuildTaskProvider: TaskProvider<out Task>
        get() = preBuildProvider

    override val BaseVariant.assembleTaskProvider: TaskProvider<out Task>
        get() = assembleProvider

    override val BaseVariant.mergeAssetsTaskProvider: TaskProvider<out Task>
        get() = mergeAssetsProvider

    override val BaseVariant.mergeResourcesTaskProvider: TaskProvider<out Task>
        get() = mergeResourcesProvider

    override val BaseVariant.processJavaResourcesTaskProvider: TaskProvider<out Task>
        get() = processJavaResourcesProvider

    override fun BaseVariant.getTaskName(prefix: String): String {
        return componentProperties.computeTaskName(prefix)
    }

    override fun BaseVariant.getTaskName(prefix: String, suffix: String): String {
        return componentProperties.computeTaskName(prefix, suffix)
    }

    override val BaseVariant.variantData: BaseVariantData
        get() = javaClass.getDeclaredMethod("getVariantData").apply {
            isAccessible = true
        }.invoke(this) as BaseVariantData

    override val BaseVariant.variantScope: VariantScope
        get() = componentProperties.variantScope

    override val BaseVariant.globalScope: GlobalScope
        get() = componentProperties.globalScope

    override val BaseVariant.originalApplicationId: String
        get() = componentProperties.variantDslInfo.packageName.get()

    override val BaseVariant.hasDynamicFeature: Boolean
        get() = globalScope.hasDynamicFeatures()

    override val BaseVariant.rawAndroidResources: Collection<File>
        get() = componentProperties.variantData.allRawAndroidResources.files

    override fun BaseVariant.getArtifactCollection(
            configType: AndroidArtifacts.ConsumedConfigType,
            scope: AndroidArtifacts.ArtifactScope,
            artifactType: AndroidArtifacts.ArtifactType
    ): ArtifactCollection {
        return componentProperties.variantDependencies.getArtifactCollection(configType, scope, artifactType)
    }

    override fun BaseVariant.getArtifactFileCollection(
            configType: AndroidArtifacts.ConsumedConfigType,
            scope: AndroidArtifacts.ArtifactScope,
            artifactType: AndroidArtifacts.ArtifactType
    ): FileCollection {
        return componentProperties.variantDependencies.getArtifactFileCollection(configType, scope, artifactType)
    }

    override val BaseVariant.allArtifacts: Map<String, Collection<File>>
        get() = ARTIFACT_TYPES.entries.map {
            val artifacts: Collection<File> by lazy {
                getFinalArtifactFiles(it.value)
            }
            it.key to artifacts
        }.toMap(TreeMap())

    override val BaseVariant.minSdkVersion: AndroidVersion
        get() = componentProperties.minSdkVersion

    override val BaseVariant.targetSdkVersion: ApiVersion
        get() = componentProperties.targetSdkVersion

    override val BaseVariant.variantType: VariantType
        get() = componentProperties.variantType

    override val BaseVariant.aar: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.AAR)

    override val BaseVariant.apk: Collection<File>
        get() = getFinalArtifactFiles(ArtifactType.APK)

    override val BaseVariant.mergedManifests: Collection<File>
        get() = getFinalArtifactFiles(ArtifactType.MERGED_MANIFEST)

    override val BaseVariant.mergedRes: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.MERGED_RES)

    override val BaseVariant.mergedAssets: Collection<File>
        get() = when (this) {
            is ApplicationVariant -> getFinalArtifactFiles(InternalArtifactType.MERGED_ASSETS)
            is LibraryVariant -> getFinalArtifactFiles(InternalArtifactType.LIBRARY_ASSETS)
            else -> TODO("Unsupported variant type: $variantType")
        }

    override val BaseVariant.processedRes: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.PROCESSED_RES)

    override val BaseVariant.symbolList: Collection<File>
        get() = when (this) {
            is ApplicationVariant -> getFinalArtifactFiles(InternalArtifactType.RUNTIME_SYMBOL_LIST)
            is LibraryVariant -> getFinalArtifactFiles(InternalArtifactType.COMPILE_SYMBOL_LIST)
            else -> TODO("Unsupported variant type : $variantType")
        }

    override val BaseVariant.symbolListWithPackageName: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.SYMBOL_LIST_WITH_PACKAGE_NAME)

    override val BaseVariant.dataBindingDependencyArtifacts: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.DATA_BINDING_DEPENDENCY_ARTIFACTS)

    override val BaseVariant.allClasses: Collection<File>
        get() = getFinalArtifactFiles(InternalArtifactType.JAVAC)

    override val BaseVariant.buildTools: BuildToolInfo
        get() = globalScope.sdkComponents.get().buildToolInfoProvider.get()

    override val BaseVariant.isPrecompileDependenciesResourcesEnabled: Boolean
        get() = variantScope.isPrecompileDependenciesResourcesEnabled

    override val Context.task: TransformTask
        get() = javaClass.getDeclaredField("this$1").apply {
            isAccessible = true
        }.get(this).run {
            javaClass.getDeclaredField("this$0").apply {
                isAccessible = true
            }.get(this)
        } as TransformTask

    override val Project.aapt2Enabled: Boolean
        get() = true

}
