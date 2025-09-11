package com.xyz.strapp.presentation.LanguageScreen

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.xyz.strapp.presentation.navigation.AuthStatusViewModel
import com.xyz.strapp.utils.Constants
import com.xyz.strapp.utils.restartApp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch


@Composable
fun LanguageScreen(
    authStatusViewModel: AuthStatusViewModel = hiltViewModel(),
    onLanguageSelection: (languageCode: String) -> Unit,
) {
    var selectedLanguage by remember { mutableStateOf("") }
    LanguageSelectionView(
        selectedLanguage = selectedLanguage,
        onLanguageChange = { language ->
            selectedLanguage = language
        },
        onLanguageSelection = onLanguageSelection,
        modifier = Modifier
    )
//    if(authStatusViewModel.isRestart.first()){
//        restartApp(LocalContext.current)
//    }
}

@Composable
fun LanguageSelectionView(
    selectedLanguage: String,
    onLanguageChange: (String) -> Unit,
    onLanguageSelection: (languageCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val languages = Constants.AVAILABLE_LANGUAGES

    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxSize()
    ){
        Column(
            modifier = modifier.fillMaxHeight()
        ) {
            Column(
                modifier = modifier.padding(16.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select your language.",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp),
                )

                // iOS-style language options
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 50.dp)
                        .background(
                            color = Color(0xFFF2F2F7),
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Column {
                        languages.forEachIndexed { index, language ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { onLanguageChange(language) }
                                    .background(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (selectedLanguage == language)
                                            Color(0xFFE3F2FD)
                                        else
                                            Color.Transparent
                                    )
                                    .padding(16.dp)
                            ) {
                                Text(
                                    text = language,
                                    fontSize = 18.sp,
                                    color = if (selectedLanguage == language)
                                        Color(0xFF007AFF)
                                    else
                                        Color.Black,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Add divider between options (not after last item)
                            if (index < languages.size - 1) {
                                HorizontalDivider(
                                    color = Color(0xFFE0E0E0),
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
                if (selectedLanguage.isNotEmpty()) {
                    ContinueButton(
                        selectedLanguage = selectedLanguage,
                        onLanguageSelection = onLanguageSelection,
                        modifier = Modifier
                    )
                }
            }


        }

    }
}

@Composable
fun ContinueButton(
    selectedLanguage: String,
    onLanguageSelection: (languageCode: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    Button(
        onClick = {
            if(selectedLanguage == "English"){
                onLanguageSelection("en");
            }
            else {
                onLanguageSelection("ta");
            }
            scope.launch {
                delay(1000)
                restartApp(context)
            }
            val message = if (selectedLanguage.isNotEmpty()) {
                "Switching to $selectedLanguage"
            } else {
                "Please select a language first"
            }
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(color = 0xFFF2F2F7),
            contentColor = Color(color = 0xFF007AFF)
        ),
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .height(50.dp)
            .width(250.dp)
    ){
        Text(
            text = "Continue",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold)
    }
}
