package io.github.mrcoder20.ioclens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import io.github.mrcoder20.ioclens.core.storage.InMemoryKeyValueStorage
import io.github.mrcoder20.ioclens.core.storage.createPersistentKeyValueStorage
import io.github.mrcoder20.ioclens.data.repository.SettingsRepositoryImpl
import io.github.mrcoder20.ioclens.data.repository.ThreatIntelRepositoryImpl
import io.github.mrcoder20.ioclens.domain.repository.SettingsRepository
import io.github.mrcoder20.ioclens.domain.usecase.AnalyzeIndicatorUseCase
import io.github.mrcoder20.ioclens.domain.usecase.GetAppSettingsUseCase
import io.github.mrcoder20.ioclens.domain.usecase.SetIntroCompletedUseCase
import io.github.mrcoder20.ioclens.presentation.home.HomeViewModel
import io.github.mrcoder20.ioclens.presentation.intro.IntroViewModel
import io.github.mrcoder20.ioclens.presentation.main.MainScreen
import io.github.mrcoder20.ioclens.presentation.main.MainViewModel

object AppDependencies {
    val keyValueStorage by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        createPersistentKeyValueStorage()
    }
    val settingsRepository: SettingsRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SettingsRepositoryImpl(keyValueStorage)
    }
    val threatIntelRepository by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        ThreatIntelRepositoryImpl(settingsRepository)
    }
    val getAppSettingsUseCase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GetAppSettingsUseCase(settingsRepository)
    }
    val setIntroCompletedUseCase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        SetIntroCompletedUseCase(settingsRepository)
    }
    val analyzeIndicatorUseCase by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        AnalyzeIndicatorUseCase(threatIntelRepository)
    }
    val mainViewModel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        MainViewModel(getAppSettingsUseCase, setIntroCompletedUseCase)
    }
    val introViewModel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        IntroViewModel(setIntroCompletedUseCase, settingsRepository)
    }
    val homeViewModel by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        HomeViewModel(analyzeIndicatorUseCase, setIntroCompletedUseCase, settingsRepository)
    }
}

@Composable
@Preview
fun App() {
    val isPreview = LocalInspectionMode.current
    if (isPreview) {
        val settingsRepo = remember { SettingsRepositoryImpl(InMemoryKeyValueStorage()) }
        val threatRepo = remember { ThreatIntelRepositoryImpl(settingsRepo) }
        val getSettings = remember { GetAppSettingsUseCase(settingsRepo) }
        val setIntro = remember { SetIntroCompletedUseCase(settingsRepo) }
        val analyze = remember { AnalyzeIndicatorUseCase(threatRepo) }
        MainScreen(
            mainViewModel = remember { MainViewModel(getSettings, setIntro) },
            introViewModel = remember { IntroViewModel(setIntro, settingsRepo) },
            homeViewModel = remember { HomeViewModel(analyze, setIntro, settingsRepo) },
            settingsRepository = settingsRepo
        )
        return
    }

    MainScreen(
        mainViewModel = AppDependencies.mainViewModel,
        introViewModel = AppDependencies.introViewModel,
        homeViewModel = AppDependencies.homeViewModel,
        settingsRepository = AppDependencies.settingsRepository
    )
}
