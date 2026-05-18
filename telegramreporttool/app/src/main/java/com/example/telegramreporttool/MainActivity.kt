package com.example.telegramreporttool

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.*
import androidx.navigation3.ui.NavDisplay
import androidx.room.Room
import com.example.telegramreporttool.data.local.ReportDatabase
import com.example.telegramreporttool.data.model.Report
import com.example.telegramreporttool.data.repository.ReportRepository
import com.example.telegramreporttool.data.telegram.TelegramManager
import com.example.telegramreporttool.ui.auth.*
import com.example.telegramreporttool.ui.dashboard.*
import com.example.telegramreporttool.ui.landing.LandingScreen
import com.example.telegramreporttool.ui.navigation.Route
import com.example.telegramreporttool.ui.reporting.*
import com.example.telegramreporttool.ui.settings.SettingsScreen
import com.example.telegramreporttool.ui.theme.TelegramReportToolTheme
import kotlinx.coroutines.launch
import org.drinkless.tdlib.TdApi
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffold
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator

class MainActivity : ComponentActivity() {
    
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            ReportDatabase::class.java,
            ReportDatabase.DATABASE_NAME
        ).fallbackToDestructiveMigration(true)
            .build()
    }

    private val telegramManager by lazy {
        TelegramManager(applicationContext)
    }

    private val repository by lazy {
        ReportRepository(database.reportDao(), telegramManager)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkMode by rememberSaveable { mutableStateOf(false) }
            val systemInDarkTheme = androidx.compose.foundation.isSystemInDarkTheme()
            val finalDarkMode = if (isDarkMode) true else systemInDarkTheme

            TelegramReportToolTheme(darkTheme = finalDarkMode) {
                MainApp(repository, telegramManager, isDarkMode, onDarkModeToggle = { isDarkMode = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainApp(
    repository: ReportRepository, 
    telegramManager: TelegramManager,
    isDarkMode: Boolean,
    onDarkModeToggle: (Boolean) -> Unit
) {
    val authState by telegramManager.authorizationState.collectAsStateWithLifecycle()
    val activeUserId by repository.activeUserId.collectAsStateWithLifecycle()
    
    val backStack: NavBackStack<NavKey> = rememberNavBackStack(Route.Landing)
    val navigator = rememberListDetailPaneScaffoldNavigator<Report>()
    val coroutineScope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Sync accounts from DB on launch
    LaunchedEffect(Unit) {
        repository.syncAccountsFromDb()
    }

    // Force synchronization between Auth State and Navigation
    LaunchedEffect(authState, activeUserId) {
        if (authState is TdApi.AuthorizationStateReady || activeUserId != null) {
            if ((backStack.contains(Route.Login) || backStack.contains(Route.Landing)) && backStack.size == 1) {
                backStack.clear()
                backStack.add(Route.Dashboard)
            }
        }
    }

    val entryProvider: (NavKey) -> NavEntry<NavKey> = entryProvider {
        entry<Route.Landing> {
            LandingScreen(
                onGetStarted = {
                    if (activeUserId != null) {
                        backStack.clear()
                        backStack.add(Route.Dashboard)
                    } else {
                        backStack.add(Route.Login)
                    }
                }
            )
        }
        entry<Route.Settings> {
            SettingsScreen(
                isDarkMode = isDarkMode,
                onDarkModeToggle = onDarkModeToggle,
                onBack = { backStack.removeAt(backStack.size - 1) }
            )
        }
        entry<Route.Login> {
            val viewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(telegramManager, repository)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            if (state.step == AuthStep.SUCCESS) {
                LaunchedEffect(Unit) {
                    backStack.removeAt(backStack.size - 1)
                    backStack.add(Route.Dashboard)
                }
            }
            
            AuthScreen(
                state = state,
                onPhoneNumberChange = viewModel::onPhoneNumberChange,
                onOtpChange = viewModel::onOtpChange,
                onPasswordChange = viewModel::onPasswordChange,
                onSendPhone = viewModel::sendPhoneNumber,
                onSendOtp = viewModel::sendOtp,
                onSendPassword = viewModel::sendPassword
            )
        }
        entry<Route.Dashboard> {
            val viewModel: DashboardViewModel = viewModel(
                factory = DashboardViewModelFactory(repository)
            )
            val reports by viewModel.reports.collectAsStateWithLifecycle()

            ListDetailPaneScaffold(
                directive = navigator.scaffoldDirective,
                value = navigator.scaffoldValue,
                listPane = {
                    val accounts by viewModel.accounts.collectAsStateWithLifecycle()
                    DashboardScreen(
                        reports = reports,
                        accounts = accounts,
                        onNewReport = { backStack.add(Route.Reporting) },
                        onReportClick = { report ->
                            coroutineScope.launch {
                                navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, report)
                            }
                        },
                        onSwitchAccount = viewModel::switchAccount,
                        onAddAccount = {
                            backStack.add(Route.Login)
                        },
                        onLogout = viewModel::logout,
                        onMenuClick = {
                            coroutineScope.launch { drawerState.open() }
                        }
                    )
                },
                detailPane = {
                    navigator.currentDestination?.contentKey?.let { report ->
                        ReportDetailScreen(report = report)
                    } ?: Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Select a report to see details")
                    }
                }
            )
        }
        entry<Route.Reporting> {
            val viewModel: ReportingViewModel = viewModel(
                factory = ReportingViewModelFactory(repository)
            )
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            
            ReportingScreen(
                state = state,
                onValueChange = viewModel::onTargetValueChange,
                onTypeChange = viewModel::onTargetTypeChange,
                onTemplateSelect = viewModel::selectTemplate,
                onAddEvidence = viewModel::addEvidence,
                onRemoveEvidence = viewModel::removeEvidence,
                onNext = {
                    if (state.submissionSuccess == true) {
                        backStack.removeAt(backStack.size - 1)
                    } else {
                        viewModel.nextStep()
                    }
                },
                onBack = {
                    if (state.currentStep == ReportingStep.TARGET) {
                        backStack.removeAt(backStack.size - 1)
                    } else {
                        viewModel.previousStep()
                    }
                }
            )
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(modifier = Modifier.height(16.dp))
                NavigationDrawerItem(
                    label = { Text("Landing Page") },
                    selected = backStack.lastOrNull() == Route.Landing,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            backStack.clear()
                            backStack.add(Route.Landing)
                        }
                    },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Dashboard") },
                    selected = backStack.lastOrNull() == Route.Dashboard,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            if (activeUserId != null) {
                                backStack.clear()
                                backStack.add(Route.Dashboard)
                            } else {
                                backStack.add(Route.Login)
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    label = { Text("Settings") },
                    selected = backStack.lastOrNull() == Route.Settings,
                    onClick = {
                        coroutineScope.launch {
                            drawerState.close()
                            backStack.add(Route.Settings)
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { backStack.removeAt(backStack.size - 1) },
            entryProvider = entryProvider,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator()
            )
        )
    }
}
