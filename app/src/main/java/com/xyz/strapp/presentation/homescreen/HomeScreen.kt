package com.xyz.strapp.presentation.homescreen

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.presentation.userlogin.LoginUiState
import com.xyz.strapp.presentation.userlogin.LoginViewModel
import com.xyz.strapp.ui.theme.StrAppTheme

// Data class for Bottom Navigation items (already defined, ensure it's accessible)
data class BottomNavItem(
    val title: String,
    val icon: ImageVector,
    val contentDescription: String,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    loginViewModel: LoginViewModel = hiltViewModel(), // Kept for potential use in Profile tab
    onNavigateToFaceLiveness: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    onNavigateToFavorites: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    // Add other navigation callbacks as needed, e.g., for TopAppBar actions
    onOpenDrawer: () -> Unit = {},
    onSearchClicked: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItemIndex by rememberSaveable { mutableIntStateOf(0) }
    val bottomNavItems = listOf(
        BottomNavItem("Home", Icons.Filled.Home, "Home Screen"),
        BottomNavItem("Favorites", Icons.Filled.Favorite, "Favorites Screen"),
        BottomNavItem("Profile", Icons.Filled.AccountCircle, "Profile Screen")
    )

    var showMenu by remember { mutableStateOf(false) } // For TopAppBar overflow menu

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(bottomNavItems[selectedItemIndex].title) }, // Title changes with tab
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) { // Hook for nav drawer
                        Icon(Icons.Filled.Menu, contentDescription = "Open Drawer")
                    }
                },
                actions = {
                    IconButton(onClick = onSearchClicked) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                    IconButton(onClick = { showMenu = !showMenu }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    // TODO: Implement DropdownMenu for 'showMenu'
                },
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
                            when (index) {
                                0 -> onNavigateToHome() // These could be used for actual NavController navigation
                                1 -> onNavigateToFavorites()
                                2 -> onNavigateToProfile()
                            }
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
                0 -> HomeTabContent(onNavigateToFaceLiveness)
                1 -> FavoritesTabContent()
                2 -> ProfileTabContent(loginViewModel) // Pass ViewModel if needed
            }
        }
    }
}

// Data class for Bottom Navigation items (already defined, ensure it's accessible)
// data class BottomNavItem(
//    val title: String,
//    val icon: ImageVector,
//    val contentDescription: String,
// )
@Composable
fun HomeTabContent(onNavigateToFaceLiveness: () -> Unit) {
    // Replace with your actual Home screen content
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // Changed from Center for more typical home layout
    ) {
        Text("RTS Face Liveness Attendance", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Note - Select Check-In button to mark your attendance. Please make sure your camera permissions are enabled for this application to work. If not, then please enable camera permissions from app setting option.")
        Spacer(modifier = Modifier.height(30.dp))
        // Example: Placeholder for a list of items
        Column {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                onClick = {
                    onNavigateToFaceLiveness()
                }
            ) {
                Text(
                    text = "CheckIn Attendance",
                    modifier = Modifier.padding(26.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(30.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Text(
                    text = "Register User",
                    modifier = Modifier.padding(26.dp),
                    style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center,
                )
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
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Your Favorites", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            Icons.Filled.Favorite,
            contentDescription = "Favorites Icon",
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text("Items you've marked as favorite will appear here.")
    }
}

@Composable
fun ProfileTabContent(loginViewModel: LoginViewModel) {
    // Replace with your actual Profile screen content
    // You can use the loginViewModel here to display user info or offer logout
    val loginUiState by loginViewModel.loginUiState.collectAsState()
    val email by loginViewModel.email.collectAsState() // Example: if email is part of LoginViewModel state

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("User Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Icon(
            Icons.Filled.AccountCircle,
            contentDescription = "Profile Icon",
            modifier = Modifier.height(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (loginUiState is LoginUiState.Success) {
            // Assuming LoginResponse has user details, or LoginViewModel exposes them
            // For example, if LoginResponse was stored or email is available:
            Text("Email: $email", style = MaterialTheme.typography.bodyLarge)
            // Text("User ID: ${(loginUiState as LoginUiState.Success).loginResponse.userId}", style = MaterialTheme.typography.bodyLarge)
        } else if (loginUiState is LoginUiState.Loading) {
            CircularProgressIndicator()
        } else {
            Text(
                "Not logged in or user data not available.",
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = {
            // TODO: Implement logout functionality
            // loginViewModel.logout() // Example call
            Log.d("ProfileTab", "Logout button clicked")
        }) {
            Text("Logout (Placeholder)")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    StrAppTheme {
        HomeScreen() // Removed onCheckIn as it wasn't used in the new design
    }
}
