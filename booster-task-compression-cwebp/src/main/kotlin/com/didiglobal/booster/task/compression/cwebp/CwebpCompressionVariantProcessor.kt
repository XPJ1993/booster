package com.didiglobal.booster.task.compression.cwebp

import com.android.build.gradle.api.BaseVariant
import com.didiglobal.booster.compression.CompressionResults
import com.didiglobal.booster.compression.generateReport
import com.didiglobal.booster.compression.isFlatPngExceptRaw
import com.didiglobal.booster.compression.isPngExceptRaw
import com.didiglobal.booster.gradle.isAapt2Enabled
import com.didiglobal.booster.gradle.mergeResourcesTaskProvider
import com.didiglobal.booster.gradle.mergedRes
import com.didiglobal.booster.gradle.project
import com.didiglobal.booster.kotlinx.Wildcard
import com.didiglobal.booster.kotlinx.search
import com.didiglobal.booster.task.spi.VariantProcessor
import com.google.auto.service.AutoService

/**
 * @author johnsonlee
 */
@AutoService(VariantProcessor::class)
class CwebpCompressionVariantProcessor : VariantProcessor {

    override fun process(variant: BaseVariant) {
        val project = variant.project
        val results = CompressionResults()
        val ignores = project.findProperty(PROPERTY_IGNORES)?.toString()?.trim()?.split(',')?.map {
            Wildcard(it)
        }?.toSet() ?: emptySet()

        Cwebp.get(variant)?.newCompressionTaskCreator()?.createCompressionTask(variant, results, "resources", {
            variant.mergedRes.search(if (project.isAapt2Enabled) ::isFlatPngExceptRaw else ::isPngExceptRaw)
        }, ignores, variant.mergeResourcesTaskProvider)?.configure {
            it.doLast {
                results.generateReport(variant, Build.ARTIFACT)
            }
        }
    }

}

private val PROPERTY_PREFIX = Build.ARTIFACT.replace('-', '.')

private val PROPERTY_IGNORES = "$PROPERTY_PREFIX.ignores"