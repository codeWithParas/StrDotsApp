package com.xyz.strapp.presentation.userlogin

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.R
import com.xyz.strapp.ui.theme.StrAppTheme
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    loginViewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit, // Callback for successful login & navigation
    onNavigateToRegister: () -> Unit // Callback for navigating to registration
) {
    //SetupSystemUiController(isNavigationBarContrastEnforced = false)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val email by loginViewModel.email.collectAsState()
    val password by loginViewModel.password.collectAsState()
    val loginUiState by loginViewModel.loginUiState.collectAsState()

    var passwordVisible by remember { mutableStateOf(false) }
    val isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches()
    val isFormValid = isEmailValid && password.isNotEmpty() && loginUiState !is LoginUiState.Loading

            // Handle UI state changes, especially for errors and success
    LaunchedEffect(loginUiState) {
        when (val state = loginUiState) {
            is LoginUiState.Success -> {
                Log.d("LoginScreen", "Login successful: ${state.loginResponse.userName}")
                // Here you would typically save tokens, user data, etc.
                // For now, we'll show a snackbar and then navigate.
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Login Successful: ${state.loginResponse.userName}",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                }
                // Potentially clear sensitive fields from ViewModel after success
                // loginViewModel.clearSensitiveData()
                onLoginSuccess() // Trigger navigation
            }
            is LoginUiState.Error -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        message = "Login Failed: ${state.message}",
                        duration = androidx.compose.material3.SnackbarDuration.Long
                    )
                }
                // Optionally reset state in ViewModel to allow retry without error sticking
                loginViewModel.resetLoginState()
            }
            else -> {
                // Idle or Loading, no snackbar needed here by default
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->

        Box {
            Surface(
                color = Color.Black,
                modifier = Modifier.fillMaxSize(),
            ){ }
            BackgroundImage(modifier = Modifier)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues) // Apply padding from Scaffold
                    .padding(16.dp), // Add your own screen padding
                //verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Title(modifier = Modifier)
                Spacer(modifier = Modifier.height(64.dp))
                Text(
                    text = "Login",
                    color = Color.White,
                    fontSize = 30.sp,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(24.dp))

                TextField(
                    value = email,
                    onValueChange = { loginViewModel.onEmailChange(it)  },
                    label = { Text("Email") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    isError = email.isNotEmpty() && !isEmailValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Email Icon"
                        )
                    },
                    enabled = loginUiState !is LoginUiState.Loading, // Disable when loading
                    modifier = Modifier
                        .height(62.dp).fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(20.dp))
                TextField(
                    value = password,
                    onValueChange = { loginViewModel.onPasswordChange(it) },
                    label = { Text("Password") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Password Icon"
                        )
                    },
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = image,
                                contentDescription = if (passwordVisible) "Hide password" else "Show password"
                            )
                        }
                    },
                    enabled = loginUiState !is LoginUiState.Loading, // disabled when loading
                    modifier = Modifier
                        .height(62.dp).fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (loginUiState) {
                    is LoginUiState.Loading -> {
                        CircularProgressIndicator()
                    }
                    else -> {
                        // Continue button
                        Button(
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "Login with: $email",
                                    Toast.LENGTH_SHORT
                                ).show()

                                loginViewModel.onLoginClicked()
                            },
                            enabled = isFormValid,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF2F2F7),
                                contentColor = Color(0xFF007AFF),
                                disabledContainerColor = Color.LightGray,
                                disabledContentColor = Color.Gray
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .height(62.dp).fillMaxWidth()
                        ) {
                            Text(
                                text = "Continue",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Optional: Display a text message for success/error directly on screen
                // if (loginUiState is LoginUiState.Error) {
                //     Text(
                //         text = (loginUiState as LoginUiState.Error).message,
                //         color = MaterialTheme.colorScheme.error,
                //         modifier = Modifier.padding(top = 8.dp)
                //     )
                // }
                // if (loginUiState is LoginUiState.Success) {
                //     Text(
                //         text = "Login Successful!", // Or more specific message
                //         color = MaterialTheme.colorScheme.primary, // Or a success color
                //         modifier = Modifier.padding(top = 8.dp)
                //     )
                // }
            }
        }
    }
}

@Composable
fun Title(modifier: Modifier){
    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = "Attendance Tracker",
            color = Color.White,
            fontSize = 30.sp,
            modifier = modifier
                .padding(16.dp)
        )
        Text(
            text = "Sign in to manage your attendance",
            color = Color.White,
            modifier = modifier
                .padding(16.dp)
        )
    }

}

@Composable
fun BackgroundImage(modifier: Modifier){
    val image = painterResource(R.drawable.loginflatbg)
    Image(
        modifier = Modifier.fillMaxSize(),

        painter = image,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        alpha = 0.5F
    )
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    StrAppTheme { // Ensure your theme is applied for previews
        LoginScreen(
            onLoginSuccess = { Log.d("Preview", "Login Success Clicked") },
            onNavigateToRegister = { Log.d("Preview", "Navigate to Register Clicked") }
            // You might need to provide a mock ViewModel for more complex previews
        )
    }
}

