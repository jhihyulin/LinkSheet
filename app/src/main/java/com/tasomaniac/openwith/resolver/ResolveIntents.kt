package com.tasomaniac.openwith.resolver

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.tasomaniac.openwith.extension.componentName
import com.tasomaniac.openwith.extension.isHttp
import com.tasomaniac.openwith.preferred.PreferredResolver
import fe.fastforwardkt.FastForwardLoader
import fe.libredirectkt.LibRedirect
import fe.libredirectkt.LibRedirectLoader
import fe.linksheet.BuildConfig
import fe.linksheet.activity.bottomsheet.BottomSheetViewModel
import fe.linksheet.extension.getUri
import fe.linksheet.extension.sourceIntent
import fe.linksheet.resolver.ResolveListGrouper
import timber.log.Timber

object ResolveIntents {
    val fastForwardRulesObject by lazy { FastForwardLoader.loadBuiltInFastForwardRules() }
    private val libRedirectServices by lazy { LibRedirectLoader.loadBuiltInServices() }

    suspend fun resolve(
        context: Context,
        intent: Intent,
        viewModel: BottomSheetViewModel
    ): IntentResolverResult {
        Timber.tag("ResolveIntents").d("Intent: $intent")

        var uri = intent.getUri(viewModel.useClearUrls, viewModel.useFastForwardRules)
        var followRedirect: BottomSheetViewModel.FollowRedirect? = null

        if (viewModel.followRedirects && uri != null) {
            viewModel.followRedirects(
                uri,
                viewModel.followRedirectsLocalCache,
                fastForwardRulesObject
            ).getOrNull()?.let {
                followRedirect = it
                uri = Uri.parse(it.resolvedUrl)
            }
        }

        if (viewModel.enableLibRedirect) {
            val service = LibRedirect.findServiceForUrl(uri.toString(), libRedirectServices)
            Timber.tag("ResolveIntents").d("LibRedirect $service")
            if (service != null && viewModel.loadLibRedirectState(service.key) == true) {
                val savedDefault = viewModel.getLibRedirectDefault(service.key)
                val redirected = if (savedDefault != null) {
                    LibRedirect.redirect(
                        uri.toString(),
                        savedDefault.frontendKey,
                        savedDefault.instanceUrl
                    )
                } else {
                    val defaultInstance =
                        LibRedirect.getDefaultInstanceForFrontend(service.defaultFrontend.key)
                    LibRedirect.redirect(
                        uri.toString(),
                        service.defaultFrontend.key,
                        defaultInstance?.first()!!
                    )
                }

                Timber.tag("ResolveIntents").d("LibRedirect $redirected")
                if (redirected != null) {
                    uri = Uri.parse(redirected)
                }
            }
        }


        val preferredApp = uri?.let {
            PreferredResolver.resolve(context, it.host!!)
        }

        Timber.tag("ResolveIntents").d("PreferredApp: $preferredApp")

        val hostHistory = uri?.let {
            PreferredResolver.resolveHostHistory(context, it.host!!)
        } ?: emptyMap()


        Timber.tag("ResolveIntents").d("HostHistory: $hostHistory")


        val alwaysPreferred = preferredApp?.app?.alwaysPreferred

        val sourceIntent = intent.sourceIntent(uri)
        Timber.tag("ResolveIntents").d("SourceIntent: $sourceIntent")

        val resolveListPreSort = context.packageManager.queryIntentActivities(
            sourceIntent, PackageManager.MATCH_ALL
        )

        resolveListPreSort.removeAll {
            it.activityInfo.packageName == BuildConfig.APPLICATION_ID
        }

        Timber.tag("ResolveIntents").d("PreferredApp ComponentName: ${preferredApp?.app?.componentName}")

        val browserMode = if (sourceIntent.isHttp()) {
            BrowserHandler.handleBrowsers(context, resolveListPreSort, viewModel)
        } else null


        val (resolved, filteredItem, showExtended) = ResolveListGrouper.resolveList(
            context,
            resolveListPreSort,
            hostHistory,
            preferredApp?.app?.componentName,
            !viewModel.dontShowFilteredItem
        )

        val selectedBrowserIsSingleOption =
            browserMode?.first == BrowserHandler.BrowserMode.SelectedBrowser
                    && resolveListPreSort.singleOrNull()?.activityInfo?.componentName() == browserMode.second?.activityInfo?.componentName()

        val noBrowsersPresentOnlySingleApp =
            browserMode?.first == BrowserHandler.BrowserMode.None && resolveListPreSort.size == 1


        Timber.tag("ResolveIntents").d(
            "Resolved: $resolved, filteredItem: $filteredItem, showExtended: $showExtended, selectedBrowserIsSingleOption: $selectedBrowserIsSingleOption"
        )

        return IntentResolverResult(
            uri,
            resolved,
            filteredItem,
            showExtended,
            alwaysPreferred,
            selectedBrowserIsSingleOption || noBrowsersPresentOnlySingleApp,
            followRedirect
        )
    }
}