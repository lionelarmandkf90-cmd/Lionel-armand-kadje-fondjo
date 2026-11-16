package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PointOfSale
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.model.Role
import com.example.data.repository.MaquisRepository
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.DepensesScreen
import com.example.ui.screens.PosScreen
import com.example.ui.screens.ProfitDashboardScreen
import com.example.ui.screens.StockScreen
import com.example.ui.theme.MaquisAmber
import com.example.ui.theme.MaquisBg
import com.example.ui.theme.MaquisBorder
import com.example.ui.theme.MaquisEmerald
import com.example.ui.theme.MaquisRose
import com.example.ui.theme.MaquisSurface
import com.example.ui.theme.MaquisSurfaceAlt
import com.example.ui.theme.MaquisTextPrimary
import com.example.ui.theme.MaquisTextSecondary
import com.example.ui.theme.MaquisProTheme
import com.example.ui.viewmodel.MaquisTab
import com.example.ui.viewmodel.MaquisUiState
import com.example.ui.viewmodel.MaquisViewModel
import com.example.util.NetworkObserver
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaquisProTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            MaquisFeatureRoot(currentUserId = user.uid)
        }
    }
}

@Composable
private fun MaquisFeatureRoot(currentUserId: String) {
    val context = LocalContext.current
    val networkObserver = remember(context) { NetworkObserver(context.applicationContext) }
    DisposableEffect(networkObserver) {
        onDispose { networkObserver.release() }
    }

    val viewModel: MaquisViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                val repo = MaquisRepository(db, Firebase.auth)
                MaquisViewModel(repo, currentUserId, networkObserver.isOnline)
            }
        }
    )

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var showSignOutConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        val msg = uiState.snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = uiState.activeTab != MaquisTab.CAISSE) {
        viewModel.selectTab(MaquisTab.CAISSE)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(MaquisBg)) {
        val useRail = maxWidth >= 840.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = MaquisSurface,
                    contentColor = MaquisTextPrimary,
                    modifier = Modifier.fillMaxHeight()
                ) {
                    Spacer(Modifier.height(16.dp))
                    MaquisTab.entries.forEach { tab ->
                        val selected = uiState.activeTab == tab
                        NavigationRailItem(
                            selected = selected,
                            onClick = { viewModel.selectTab(tab) },
                            icon = {
                                Icon(
                                    imageVector = tabIcon(tab, selected),
                                    contentDescription = tab.label
                                )
                            },
                            label = { Text(tab.label, fontWeight = FontWeight.Bold) },
                            colors = NavigationRailItemDefaults.colors(
                                selectedIconColor = MaquisEmerald,
                                selectedTextColor = MaquisEmerald,
                                indicatorColor = MaquisEmerald.copy(alpha = 0.16f),
                                unselectedIconColor = MaquisTextSecondary,
                                unselectedTextColor = MaquisTextSecondary
                            )
                        )
                    }
                }
            }

            Scaffold(
                containerColor = MaquisBg,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    MaquisTopBar(
                        state = uiState,
                        onToggleRole = {
                            if (uiState.role == Role.PROPRIETAIRE) {
                                viewModel.lockToGerant()
                            } else {
                                viewModel.selectTab(MaquisTab.BILAN)
                            }
                        },
                        onSignOutClick = { showSignOutConfirm = true }
                    )
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaquisSurface,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            MaquisTab.entries.forEach { tab ->
                                val selected = uiState.activeTab == tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = {
                                        Icon(
                                            imageVector = tabIcon(tab, selected),
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = tab.label,
                                            fontSize = 11.sp,
                                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaquisEmerald,
                                        selectedTextColor = MaquisEmerald,
                                        indicatorColor = MaquisEmerald.copy(alpha = 0.16f),
                                        unselectedIconColor = MaquisTextSecondary,
                                        unselectedTextColor = MaquisTextSecondary
                                    ),
                                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (uiState.isLoading && uiState.produits.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaquisEmerald)
                                Spacer(Modifier.height(12.dp))
                                Text("Chargement du maquis...", color = MaquisTextSecondary)
                            }
                        }
                    } else {
                        when (uiState.activeTab) {
                            MaquisTab.CAISSE -> PosScreen(
                                state = uiState,
                                onSelectTable = viewModel::selectTable,
                                onSelectCategory = viewModel::selectCategory,
                                onAddToCart = viewModel::addToCart,
                                onUpdateCartQuantity = viewModel::updateCartQuantity,
                                onSaveTableOuverte = viewModel::saveTableOuverte,
                                onEncaisserVente = viewModel::encaisserVente,
                                onDeclarerPerte = viewModel::declarerPerte,
                                onDismissReceipt = viewModel::dismissReceiptBanner
                            )
                            MaquisTab.STOCK -> StockScreen(
                                state = uiState,
                                onRestockProduit = viewModel::approvisionnerCasier,
                                onCreateProduit = viewModel::creerProduit,
                                onUpdateEmballage = viewModel::updateEmballage
                            )
                            MaquisTab.DEPENSES -> DepensesScreen(
                                state = uiState,
                                onAjouterDepense = viewModel::ajouterDepense,
                                onSupprimerDepense = viewModel::supprimerDepense
                            )
                            MaquisTab.BILAN -> ProfitDashboardScreen(
                                state = uiState,
                                onUnlockWithPin = viewModel::unlockOwnerWithPin,
                                onLockToGerant = viewModel::lockToGerant,
                                onSetPeriode = viewModel::setPeriodeBilan,
                                onSyncNow = viewModel::syncNow,
                                onReglerArdoise = viewModel::reglerArdoise,
                                onUpdatePin = viewModel::updateOwnerPin
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSignOutConfirm) {
        AlertDialog(
            onDismissRequest = { showSignOutConfirm = false },
            containerColor = MaquisSurface,
            title = { Text("Déconnexion", color = MaquisTextPrimary) },
            text = {
                Text(
                    "Voulez-vous vous déconnecter du compte Google ? Les données non synchronisées restent en attente sur l'appareil.",
                    color = MaquisTextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSignOutConfirm = false
                        signOutUser(credentialManager, coroutineScope)
                    }
                ) {
                    Text("Déconnecter", color = MaquisRose, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSignOutConfirm = false }) {
                    Text("Annuler", color = MaquisTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MaquisTopBar(
    state: MaquisUiState,
    onToggleRole: () -> Unit,
    onSignOutClick: () -> Unit
) {
    Surface(
        color = MaquisSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaquisEmerald),
                    contentAlignment = Alignment.Center
                ) {
                    Text("MP", color = MaquisBg, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = state.settings.maquisName.ifBlank { "MaquisPro" },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaquisTextPrimary
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val isSynced = state.isOnline && state.pendingSyncCount == 0
                        val statusColor = when {
                            !state.isOnline -> MaquisAmber
                            state.pendingSyncCount > 0 -> MaquisAmber
                            else -> MaquisEmerald
                        }
                        val statusText = when {
                            !state.isOnline -> "Hors-ligne (${state.pendingSyncCount} local)"
                            state.pendingSyncCount > 0 -> "En ligne • ${state.pendingSyncCount} à synchro"
                            else -> "En ligne • Synchro active"
                        }
                        Icon(
                            imageVector = if (state.isOnline) Icons.Filled.Wifi else Icons.Filled.CloudOff,
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = statusText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isOwner = state.role == Role.PROPRIETAIRE
                Surface(
                    onClick = onToggleRole,
                    color = if (isOwner) MaquisAmber.copy(alpha = 0.18f) else MaquisSurfaceAlt,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isOwner) MaquisAmber.copy(alpha = 0.5f) else MaquisBorder),
                    modifier = Modifier.height(40.dp).testTag("role_badge_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isOwner) Icons.Filled.Shield else Icons.Filled.Person,
                            contentDescription = null,
                            tint = if (isOwner) MaquisAmber else MaquisTextSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = if (isOwner) "Propriétaire" else "Gérant",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isOwner) MaquisAmber else MaquisTextPrimary
                        )
                    }
                }

                IconButton(
                    onClick = onSignOutClick,
                    modifier = Modifier.minimumInteractiveComponentSize().testTag("sign_out_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = "Déconnexion",
                        tint = MaquisTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

private fun tabIcon(tab: MaquisTab, selected: Boolean) = when (tab) {
    MaquisTab.CAISSE -> if (selected) Icons.Filled.PointOfSale else Icons.Outlined.PointOfSale
    MaquisTab.STOCK -> if (selected) Icons.Filled.Inventory2 else Icons.Outlined.Inventory2
    MaquisTab.DEPENSES -> if (selected) Icons.Filled.ReceiptLong else Icons.Outlined.ReceiptLong
    MaquisTab.BILAN -> if (selected) Icons.Filled.BarChart else Icons.Outlined.BarChart
}
