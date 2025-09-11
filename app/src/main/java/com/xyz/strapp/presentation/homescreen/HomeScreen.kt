package com.xyz.strapp.presentation.homescreen

import android.Manifest
import android.util.Log
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.xyz.strapp.R
import com.xyz.strapp.presentation.logs.LogsScreen
import com.xyz.strapp.presentation.profile.ProfileScreen
import com.xyz.strapp.presentation.userlogin.LoginViewModel
import com.xyz.strapp.ui.theme.StrAppTheme
import kotlinx.coroutines.delay

// Data class for Bottom Navigation items (already defined, ensure it's accessible)
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val contentDescription: String,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    loginViewModel: LoginViewModel = hiltViewModel(), // Kept for potential use in Profile tab
    onNavigateToFaceLiveness: (Boolean, Double, Double) -> Unit,
    onNavigateToHome: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, "Home Screen"),
        BottomNavItem("Dashboard", Icons.Filled.Dashboard, "Dashboard Screen"),
        BottomNavItem("Logs", Icons.Filled.History, "Logs Screen"),
        BottomNavItem("Profile", Icons.Filled.AccountCircle, "Profile Screen")
    )


    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(bottomNavItems[selectedItemIndex].title) }, // Title changes with tab
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                bottomNavItems.forEachIndexed { index, item ->
                    NavigationBarItem(
                        selected = selectedItemIndex == index,
                        onClick = {
                            selectedItemIndex = index
                                /*when (index) {
                                    0 -> onNavigateToHome() // These could be used for actual NavController navigation
                                    1 -> onNavigateToFavorites()
                                    2 -> {} // Logs tab handled in the when statement above
                                    3 -> onNavigateToProfile()
                                }*/
                            Log.d("HomeScreen", "Selected: ${item.title}")
                        },
                        label = { Text(item.title) },
                        icon = { Icon(item.icon, contentDescription = item.contentDescription) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) { // Apply innerPadding to the content root
            when (selectedItemIndex) {
                0 -> HomeTabContent(viewModel, onNavigateToFaceLiveness)
                1 -> FavoritesTabContent()
                2 -> LogsTabContent()
                3 -> ProfileTabContent(onLogout = { onLogout() }) // Pass ViewModel if needed
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun HomeTabContent(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToFaceLiveness: (Boolean, Double, Double) -> Unit
) {
    val locationState by viewModel.locationState.collectAsState()
    val locationPermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted) {
            Log.d("HomeScreen", "Location permissions are granted. Fetching location.")
            viewModel.fetchCurrentUserLocation()
        } else {
            // Location Permissions not granted
        }
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed from Center for more typical home layout
    ) {
        Text("RTS Attendance App", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Note - Select Check-In button to mark your attendance, try and place your phone in a way to place your face withing the oval to detect the face.")
        Spacer(modifier = Modifier.height(30.dp))

        // Location UI
        when (val state = locationState) {
            is LocationUiState.Idle -> {
                CircularProgressIndicator()
                Text("Awaiting location permission...")
            }
            is LocationUiState.Fetching -> {
                CircularProgressIndicator()
                Text("Fetching location...")
            }
            is LocationUiState.Success -> {
                viewModel.setLocation(state.location.latitude.toFloat(), state.location.longitude.toFloat())
                Text(modifier = Modifier.fillMaxWidth(), text = "You are at location : ", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(10.dp))
                Text(state.location.address, style = MaterialTheme.typography.bodyMedium)
            }
            is LocationUiState.Error -> {
                Text("Location Error: ${state.message}")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (!locationPermissionsState.allPermissionsGranted) {
            LaunchedEffect(Unit) {
                delay(800)
                locationPermissionsState.launchMultiplePermissionRequest()
            }
        } else if (locationState is LocationUiState.Error || locationState is LocationUiState.Idle) {
            Button(onClick = { viewModel.fetchCurrentUserLocation() }) {
                Text("Retry/Fetch Location")
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = {
                    when (val state = locationState) {
                        is LocationUiState.Success -> onNavigateToFaceLiveness(true, state.location.latitude, state.location.longitude)
                        else -> {}
                    }
                },
                enabled = locationPermissionsState.allPermissionsGranted && locationState is LocationUiState.Success
            ) {
                val image = painterResource(R.drawable.checkin)
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                    modifier = Modifier
                        .height(50.dp)
                        .width(50.dp),
                    painter = image,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alpha = 0.5F
                )
                    Text(
                        text = "Check In",
                        modifier = Modifier.padding(26.dp),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )}
            }
            Spacer(modifier = Modifier.height(30.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = {
                    when (val state = locationState) {
                        is LocationUiState.Success -> onNavigateToFaceLiveness(false, state.location.latitude, state.location.longitude)
                        else -> {}
                    }
                },
                enabled = locationPermissionsState.allPermissionsGranted && locationState is LocationUiState.Success
            ) {
                val image = painterResource(R.drawable.checkout)
                Row(
                    modifier = Modifier.padding(start = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        modifier = Modifier
                            .height(50.dp)
                            .width(50.dp),

                        painter = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.5F
                    )
                    Text(
                        text = "Check Out",
                        modifier = Modifier.padding(26.dp),
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        /*LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(2) { index ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Item ${index + 1}",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }*/
    }
}

@Composable
fun FavoritesTabContent() {
    // Replace with your actual Favorites screen content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        WebViewScreen("http://103.186.230.15:6203/AttendanceDashboard?email=ravi@dtc.com", modifier = Modifier)
//        Text("Your Favorites", style = MaterialTheme.typography.headlineMedium)
//        Spacer(modifier = Modifier.height(16.dp))
//        Icon(
//            Icons.Filled.Favorite,
//            contentDescription = "Favorites Icon",
//            modifier = Modifier.height(48.dp)
//        )
//        Spacer(modifier = Modifier.height(8.dp))
//        Text("Items you've marked as favorite will appear here.")
    }
}

@Composable
fun WebViewScreen(
    url: String,
    modifier: Modifier = Modifier
) {
    // AndroidView is a composable that can host a traditional Android View.
    AndroidView(
        modifier = modifier,
        factory = { context ->
            // The factory block is where you create and initialize the View.
            // This block is called only once.
            WebView(context).apply {
                // Basic configuration for the WebView
                settings.javaScriptEnabled = true
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // The WebViewClient is essential for handling navigation within the WebView
                // instead of letting the system open a browser.
                webViewClient = WebViewClient()

                // Load the initial URL
                loadUrl(url)
            }
        },
        update = { webView ->
            // The update block is called when the composable is recomposed.
            // You can use it to update the View with new state.
            // For example, if the `url` parameter changes, you can load the new URL here.
            webView.loadUrl(url)
        }
    )
}
@Composable
fun LogsTabContent() {
    LogsScreen()
}

@Composable
fun ProfileTabContent(onLogout: () -> Unit) {
    ProfileScreen( onLogout = {onLogout()})
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    StrAppTheme {
    }
}
